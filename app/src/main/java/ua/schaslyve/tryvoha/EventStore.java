package ua.schaslyve.tryvoha;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

final class EventStore {
    static void add(Context c, boolean active) {
        SharedPreferences prefs = Prefs.get(c);
        JSONArray old;
        try { old = new JSONArray(prefs.getString(Prefs.KEY_LOG, "[]")); }
        catch (Exception e) { old = new JSONArray(); }
        JSONArray next = new JSONArray();
        JSONObject entry = new JSONObject();
        try {
            entry.put("active", active);
            entry.put("when", System.currentTimeMillis());
            entry.put("area", Prefs.area(c).city);
            next.put(entry);
            for (int i = 0; i < Math.min(old.length(), 39); i++) next.put(old.getJSONObject(i));
        } catch (Exception ignored) { }
        prefs.edit().putString(Prefs.KEY_LOG, next.toString()).apply();
    }

    static JSONArray get(Context c) {
        try { return new JSONArray(Prefs.get(c).getString(Prefs.KEY_LOG, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
}
