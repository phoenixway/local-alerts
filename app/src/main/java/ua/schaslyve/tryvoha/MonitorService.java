package ua.schaslyve.tryvoha;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import java.util.concurrent.atomic.AtomicBoolean;

/** User-enabled, visible foreground monitoring. No background location access. */
public final class MonitorService extends Service {
    public static final String ACTION_CHECK = "ua.schaslyve.tryvoha.CHECK";
    private final AtomicBoolean alive = new AtomicBoolean(false);
    private Thread pollingThread;

    @Override public void onCreate() {
        super.onCreate();
        Notify.channels(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Prefs.ready(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        enterForeground("Моніторинг активний · " + Prefs.area(this).city);
        if (alive.compareAndSet(false, true)) {
            pollingThread = new Thread(this::loop, "air-alert-monitor");
            pollingThread.start();
        } else if (intent != null && ACTION_CHECK.equals(intent.getAction()) && pollingThread != null) {
            pollingThread.interrupt();
        }
        return START_STICKY;
    }

    private void enterForeground(String line) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notify.ONGOING_ID, Notify.ongoing(this, line), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(Notify.ONGOING_ID, Notify.ongoing(this, line));
        }
    }

    private void loop() {
        while (alive.get() && Prefs.ready(this)) {
            long wait = 45_000L;
            String token = Prefs.token(this);
            AlertMatcher.Area area = Prefs.area(this);
            try {
                AlertsClient.Result result = AlertsClient.fetch(token, area);
                // Discard in-flight responses from a location/token just changed by the user.
                AlertMatcher.Area current = Prefs.area(this);
                if (!token.equals(Prefs.token(this)) || !sameArea(area, current) || !Prefs.monitoring(this)) {
                    continue;
                }
                SharedPreferences p = Prefs.get(this);
                boolean hadBaseline = p.getBoolean(Prefs.KEY_HAS_BASELINE, false);
                boolean wasActive = p.getBoolean(Prefs.KEY_LAST_ACTIVE, false);
                TransitionPolicy.Event event = TransitionPolicy.decide(hadBaseline, wasActive, result.active);
                p.edit()
                        .putBoolean(Prefs.KEY_LAST_ACTIVE, result.active)
                        .putLong(Prefs.KEY_LAST_CHECK, System.currentTimeMillis())
                        .putBoolean(Prefs.KEY_HAS_BASELINE, true)
                        .remove(Prefs.KEY_ERROR)
                        .apply();

                // The first authoritative result is a baseline. Alert if it is
                // already active, but never announce a spurious initial all-clear.
                if (event != TransitionPolicy.Event.NONE) {
                    EventStore.add(this, result.active);
                    if (Prefs.get(this).getBoolean(result.active ? Prefs.KEY_START : Prefs.KEY_END, true)) {
                        Notify.transition(this, result.active);
                    }
                }
                enterForeground(result.active ? "Тривога · " + area.city : "Спокійно · " + area.city);
            } catch (Exception e) {
                String message;
                if (e instanceof AlertsClient.ApiException) {
                    AlertsClient.ApiException api = (AlertsClient.ApiException) e;
                    message = api.getMessage();
                    if (api.status == 429 || api.status == 401) wait = 180_000L;
                } else {
                    message = "Немає з’єднання з джерелом";
                }
                Prefs.get(this).edit().putString(Prefs.KEY_ERROR, message).apply();
                enterForeground("Дані недоступні · " + area.city);
                // Crucially, leave last state untouched, and never synthesize all-clear.
            }
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ignored) {
                // Recheck immediately after an explicit refresh request.
            }
        }
        alive.set(false);
        stopSelf();
    }

    private static boolean sameArea(AlertMatcher.Area a, AlertMatcher.Area b) {
        return a.city.equals(b.city) && a.community.equals(b.community)
                && a.district.equals(b.district) && a.oblast.equals(b.oblast);
    }

    @Override public void onDestroy() {
        alive.set(false);
        if (pollingThread != null) pollingThread.interrupt();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    static void update(android.content.Context c) {
        if (Prefs.ready(c)) {
            try { c.startForegroundService(new Intent(c, MonitorService.class).setAction(ACTION_CHECK)); }
            catch (Exception e) { Prefs.get(c).edit().putString(Prefs.KEY_ERROR, "Не вдалося запустити моніторинг").apply(); }
        } else {
            c.stopService(new Intent(c, MonitorService.class));
        }
    }
}
