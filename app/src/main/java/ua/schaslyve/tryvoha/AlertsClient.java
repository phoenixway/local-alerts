package ua.schaslyve.tryvoha;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class AlertsClient {
    static final class Result {
        final boolean active;
        Result(boolean active) { this.active = active; }
    }
    static final class ApiException extends Exception {
        final int status;
        ApiException(int status, String message) { super(message); this.status = status; }
    }

    static Result fetch(String token, AlertMatcher.Area area) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.alerts.in.ua/v1/alerts/active.json").openConnection();
        conn.setConnectTimeout(12_000);
        conn.setReadTimeout(15_000);
        conn.setRequestMethod("GET");
        conn.setUseCaches(false);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Cache-Control", "no-cache");
        try {
            int status = conn.getResponseCode();
            if (status != 200) {
                if (status == 401) throw new ApiException(status, "Недійсний API-токен (401)");
                if (status == 429) throw new ApiException(status, "Ліміт API (429), повторимо пізніше");
                throw new ApiException(status, "Сервер повернув HTTP " + status);
            }
            // Bound memory usage on malformed/oversized responses.
            byte[] response = readUpTo(conn.getInputStream(), 2_000_000);
            JSONObject root = new JSONObject(new String(response, StandardCharsets.UTF_8));
            JSONArray objects = root.getJSONArray("alerts");
            List<AlertMatcher.Alert> alerts = new ArrayList<>();
            for (int i = 0; i < objects.length(); i++) {
                JSONObject item = objects.getJSONObject(i);
                if (!"air_raid".equals(item.optString("alert_type", ""))) continue;
                alerts.add(new AlertMatcher.Alert(
                        item.optString("alert_type", ""),
                        item.optString("location_type", ""),
                        item.optString("location_title", ""),
                        item.optString("location_oblast", ""),
                        item.optString("location_raion", "")));
            }
            return new Result(AlertMatcher.active(area, alerts));
        } catch (JSONException e) {
            throw new ApiException(-1, "Некоректна відповідь API");
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] readUpTo(InputStream in, int max) throws Exception {
        try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) != -1) {
                if (out.size() + n > max) throw new Exception("Відповідь API завелика");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }
}
