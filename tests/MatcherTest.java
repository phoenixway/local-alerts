import ua.schaslyve.tryvoha.AlertMatcher;
import java.util.Arrays;
import java.util.List;

public final class MatcherTest {
    static AlertMatcher.Area area = new AlertMatcher.Area("Щасливе", "Пристолична громада", "Бориспільський район", "Київська область");
    static void assertMatch(String name, boolean expected, AlertMatcher.Alert... alerts) {
        boolean actual = AlertMatcher.active(area, Arrays.asList(alerts));
        if (actual != expected) throw new AssertionError(name + ": expected " + expected + " got " + actual);
        System.out.println("PASS: " + name);
    }
    static AlertMatcher.Alert a(String type, String level, String title, String oblast, String district) {
        return new AlertMatcher.Alert(type, level, title, oblast, district);
    }
    public static void main(String[] args) {
        assertMatch("province-wide air raid", true, a("air_raid","oblast","Київська область","Київська область", ""));
        assertMatch("district air raid", true, a("air_raid","raion","Бориспільський район","Київська область", ""));
        assertMatch("community air raid", true, a("air_raid","hromada","Пристолична територіальна громада","Київська область", "Бориспільський район"));
        assertMatch("local city air raid", true, a("air_raid","city","село Щасливе","Київська область","Бориспільський район"));
        assertMatch("Kyiv City is not Kyiv Oblast", false, a("air_raid","city","Київ","м. Київ", ""));
        assertMatch("Brovary community not relevant", false, a("air_raid","hromada","Броварська громада","Київська область","Броварський район"));
        assertMatch("Other oblast same district not relevant", false, a("air_raid","raion","Бориспільський район","Черкаська область",""));
        assertMatch("Other threat not an air raid", false, a("artillery_shelling","oblast","Київська область","Київська область", ""));
        assertMatch("Unknown location types fail closed", false, a("air_raid","unknown","Щасливе","Київська область", ""));
        assertMatch("Only irrelevant alerts", false,
                a("air_raid","oblast","Чернігівська область","Чернігівська область", ""),
                a("air_raid","raion","Броварський район","Київська область", ""));
        assertMatch("Any relevant alert among others", true,
                a("air_raid","raion","Броварський район","Київська область", ""),
                a("air_raid","hromada","Пристолична громада","Київська область", ""));
        System.out.println("11/11 tests passed");
    }
}
