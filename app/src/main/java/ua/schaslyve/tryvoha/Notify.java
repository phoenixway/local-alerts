package ua.schaslyve.tryvoha;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;

final class Notify {
    static final String RUN = "run";
    static final String START = "start_alert";
    static final String END = "end_alert";
    static final int ONGOING_ID = 701;
    static final int START_ID = 702;
    static final int END_ID = 703;

    static void channels(Context ctx) {
        NotificationManager m = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel run = new NotificationChannel(RUN, "Фоновий моніторинг", NotificationManager.IMPORTANCE_LOW);
        run.setDescription("Показує, що перевірка тривог увімкнена");
        run.setSound(null, null);
        NotificationChannel start = new NotificationChannel(START, "Початок повітряної тривоги", NotificationManager.IMPORTANCE_HIGH);
        start.setDescription("Сповіщення про оголошення тривоги");
        start.enableVibration(true);
        start.setVibrationPattern(new long[]{0, 350, 200, 350, 200, 550});
        start.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
        NotificationChannel end = new NotificationChannel(END, "Відбій повітряної тривоги", NotificationManager.IMPORTANCE_HIGH);
        end.setDescription("Сповіщення про відбій тривоги");
        end.enableVibration(true);
        end.setVibrationPattern(new long[]{0, 170, 120, 170});
        end.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
        m.createNotificationChannel(run);
        m.createNotificationChannel(start);
        m.createNotificationChannel(end);
    }

    private static PendingIntent click(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static Notification ongoing(Context c, String body) {
        return new Notification.Builder(c, RUN)
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle("Тривога • Локально")
                .setContentText(body)
                .setContentIntent(click(c))
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
    }

    static void test(Context c) {
        NotificationManager m = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new Notification.Builder(c, START)
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle("Тестове сповіщення ✓")
                .setContentText("Це перевірка звуку та сповіщень. Повітряну тривогу не оголошено.")
                .setContentIntent(click(c))
                .setAutoCancel(true)
                .build();
        m.notify(704, n);
    }

    static void transition(Context c, boolean active) {
        // On Android 13+ a declined permission means only the foreground service
        // appears in task manager. Never claim an alert was delivered.
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager m = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        m.cancel(active ? END_ID : START_ID);
        String place = Prefs.area(c).city;
        Notification n = new Notification.Builder(c, active ? START : END)
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setContentTitle(active ? "🔴 ПОВІТРЯНА ТРИВОГА" : "🟢 ВІДБІЙ ТРИВОГИ")
                .setContentText(place + (active ? " · Оголошено тривогу" : " · Тривогу завершено"))
                .setStyle(new Notification.BigTextStyle().bigText(active
                        ? "Повітряна тривога для місцевості: " + place + ". Перейдіть до укриття й перевіряйте офіційні повідомлення."
                        : "Відбій повітряної тривоги для місцевості: " + place + "."))
                .setCategory(Notification.CATEGORY_ALARM)
                .setColor(active ? Color.rgb(255, 100, 103) : Color.rgb(76, 220, 170))
                .setContentIntent(click(c))
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        m.notify(active ? START_ID : END_ID, n);
    }
}
