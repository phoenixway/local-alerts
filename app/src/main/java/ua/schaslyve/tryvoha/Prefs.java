package ua.schaslyve.tryvoha;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String NAME = "tryvoha_settings";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_START = "notify_start";
    static final String KEY_END = "notify_end";
    static final String KEY_TOKEN = "token";
    static final String KEY_CITY = "city";
    static final String KEY_COMMUNITY = "community";
    static final String KEY_DISTRICT = "district";
    static final String KEY_OBLAST = "oblast";
    static final String KEY_LAST_ACTIVE = "last_active";
    static final String KEY_LAST_CHECK = "last_check";
    static final String KEY_ERROR = "error";
    static final String KEY_HAS_BASELINE = "has_baseline";
    static final String KEY_LOG = "log";

    static SharedPreferences get(Context c) {
        return c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static AlertMatcher.Area area(Context c) {
        SharedPreferences s = get(c);
        return new AlertMatcher.Area(
                s.getString(KEY_CITY, "Щасливе"),
                s.getString(KEY_COMMUNITY, "Пристолична громада"),
                s.getString(KEY_DISTRICT, "Бориспільський район"),
                s.getString(KEY_OBLAST, "Київська область"));
    }

    static String token(Context c) { return get(c).getString(KEY_TOKEN, "").trim(); }
    static boolean monitoring(Context c) { return get(c).getBoolean(KEY_ENABLED, false); }
    static boolean ready(Context c) { return monitoring(c) && !token(c).isEmpty(); }

    static void invalidate(Context c) {
        get(c).edit()
                .remove(KEY_LAST_CHECK).remove(KEY_LAST_ACTIVE)
                .remove(KEY_HAS_BASELINE).remove(KEY_ERROR)
                .apply();
    }
}
