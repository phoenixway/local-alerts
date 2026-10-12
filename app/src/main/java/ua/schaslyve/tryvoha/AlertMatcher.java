package ua.schaslyve.tryvoha;

import java.util.Locale;
import java.util.List;

/** Small pure-Java decision core; no Android or network dependencies. */
public final class AlertMatcher {
    private AlertMatcher() { }

    public static final class Area {
        public final String city;
        public final String community;
        public final String district;
        public final String oblast;

        public Area(String city, String community, String district, String oblast) {
            this.city = city == null ? "" : city.trim();
            this.community = community == null ? "" : community.trim();
            this.district = district == null ? "" : district.trim();
            this.oblast = oblast == null ? "" : oblast.trim();
        }
    }

    public static final class Alert {
        public final String type;
        public final String locationType;
        public final String title;
        public final String oblast;
        public final String district;

        public Alert(String type, String locationType, String title, String oblast, String district) {
            this.type = type;
            this.locationType = locationType;
            this.title = title;
            this.oblast = oblast;
            this.district = district;
        }
    }

    private static String norm(String value) {
        if (value == null) return "";
        String s = value.toLowerCase(new Locale("uk", "UA"))
                .replace('’', '\'').replace('ʼ', '\'')
                .replaceAll("[.,]", " ")
                .replaceAll("\\s+", " ").trim();
        s = s.replaceFirst("^(село|с\\.|місто|м\\.)\\s+", "");
        s = s.replaceFirst("\\s+(територіальна громада|сільська громада|міська громада|громада|область|район)$", "");
        return s.trim();
    }

    private static boolean eq(String a, String b) {
        return !norm(a).isEmpty() && !norm(b).isEmpty() && norm(a).equals(norm(b));
    }

    /** Only alerts in the selected oblast, including its narrower administrative units. */
    public static boolean applies(Area selected, Alert a) {
        if (!"air_raid".equals(a.type)) return false;
        String locationType = a.locationType == null ? "" : a.locationType;
        if ("oblast".equals(locationType)) return eq(a.title, selected.oblast);

        // For lower administrative levels, require the oblast in the payload.
        // This prevents identically-named towns in different oblasts from matching.
        if (!eq(a.oblast, selected.oblast)) return false;

        switch (locationType) {
            case "raion": return eq(a.title, selected.district);
            case "hromada": return eq(a.title, selected.community);
            case "city":
                // If district is supplied by API, don't confuse same-named cities.
                return eq(a.title, selected.city)
                        && (a.district == null || a.district.trim().isEmpty() || eq(a.district, selected.district));
            default: return false; // fail closed for new/unknown API location types
        }
    }

    public static boolean active(Area area, List<Alert> alerts) {
        for (Alert alert : alerts) if (applies(area, alert)) return true;
        return false;
    }
}
