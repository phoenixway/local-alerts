package ua.schaslyve.tryvoha;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Lightweight, dependency-free premium dark UI. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 15, 25);
    private static final int CARD = Color.rgb(19, 29, 43);
    private static final int BORDER = Color.rgb(35, 50, 65);
    private static final int TEXT = Color.rgb(242, 248, 252);
    private static final int MUTED = Color.rgb(150, 168, 185);
    private static final int MINT = Color.rgb(83, 224, 180);
    private static final int RED = Color.rgb(255, 108, 113);
    private static final int AMBER = Color.rgb(255, 190, 100);
    private static final Locale UK = new Locale("uk", "UA");

    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout root, contents;
    private int tab = 0;
    private boolean manualBusy = false;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (tab != 2 && !manualBusy) render();
            ui.postDelayed(this, 8_000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        w.getDecorView().setSystemUiVisibility(0);
        Notify.channels(this);
        tab = state == null ? 0 : state.getInt("tab", 0);
        render();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
        }
        if (Prefs.ready(this)) MonitorService.update(this);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("tab", tab);
        super.onSaveInstanceState(state);
    }
    @Override protected void onResume() {
        super.onResume();
        ui.removeCallbacks(refresh);
        ui.postDelayed(refresh, 8_000);
    }
    @Override protected void onPause() {
        ui.removeCallbacks(refresh);
        super.onPause();
    }

    private int d(float x) { return (int) (x * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int fill, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(d(radius));
        if (stroke != 0) drawable.setStroke(d(1), stroke);
        return drawable;
    }
    private GradientDrawable gradient(int a, int b, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a,b});
        drawable.setCornerRadius(d(radius));
        drawable.setStroke(d(1), BORDER);
        return drawable;
    }
    private TextView t(String str, int sp, int color, boolean strong) {
        TextView view = new TextView(this);
        view.setText(str);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        view.setTypeface(Typeface.create(strong ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }
    private void pad(View v, int x, int y) { v.setPadding(d(x), d(y), d(x), d(y)); }
    private LinearLayout vertical() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        return ll;
    }
    private LinearLayout horizontal() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(Gravity.CENTER_VERTICAL);
        return ll;
    }
    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : d(w), h < 0 ? h : d(h));
    }
    private void gap(LinearLayout p, int dp) {
        View empty = new View(this);
        p.addView(empty, lp(1, dp));
    }
    private LinearLayout card(LinearLayout parent, int padding) {
        LinearLayout c = vertical();
        c.setBackground(bg(CARD, 24, BORDER));
        pad(c, padding, padding);
        parent.addView(c, lp(-1, -2));
        return c;
    }
    private void render() {
        root = vertical();
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        setContentView(root);

        LinearLayout header = horizontal();
        header.setPadding(d(24), d(17), d(24), d(12));
        root.addView(header, lp(-1, 78));
        Glyph logo = new Glyph("radar", MINT);
        logo.setBackground(bg(Color.rgb(23, 54, 57), 16, Color.rgb(34, 83, 76)));
        header.addView(logo, lp(52, 52));
        LinearLayout naming = vertical();
        naming.setPadding(d(13), 0, 0, 0);
        TextView name = t("ТРИВОГА", 21, TEXT, true);
        name.setLetterSpacing(.11f);
        naming.addView(name);
        gap(naming, 4);
        TextView subtitle = t("ЛОКАЛЬНИЙ МОНІТОРИНГ", 10, MUTED, true);
        subtitle.setLetterSpacing(.18f);
        naming.addView(subtitle);
        header.addView(naming, new LinearLayout.LayoutParams(0, -2, 1));
        long lastSuccess = Prefs.get(this).getLong(Prefs.KEY_LAST_CHECK, 0);
        boolean fresh = !Prefs.token(this).isEmpty() && lastSuccess > 0
                && System.currentTimeMillis() - lastSuccess >= 0
                && System.currentTimeMillis() - lastSuccess < 4 * 60_000L
                && Prefs.get(this).getString(Prefs.KEY_ERROR, "").isEmpty();
        TextView badge = t(fresh ? "●  LIVE" : "●  OFFLINE", 10, fresh ? MINT : AMBER, true);
        badge.setLetterSpacing(.07f);
        badge.setPadding(d(10), d(8), d(10), d(8));
        badge.setBackground(bg(fresh ? Color.rgb(23, 52, 52) : Color.rgb(60, 45, 30),
                50, fresh ? Color.rgb(31, 77, 69) : Color.rgb(80, 59, 40)));
        header.addView(badge);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        contents = vertical();
        contents.setPadding(d(22), d(22), d(22), d(22));
        scroll.addView(contents);
        if (tab == 0) dashboard();
        else if (tab == 1) events();
        else settings();

        LinearLayout navWrap = vertical();
        navWrap.setPadding(d(14), d(12), d(14), d(12));
        navWrap.setBackgroundColor(Color.rgb(12, 21, 31));
        root.addView(navWrap, lp(-1, 82));
        LinearLayout nav = horizontal();
        nav.setPadding(d(6), d(4), d(6), d(4));
        nav.setBackground(bg(Color.rgb(21, 33, 47), 20, BORDER));
        navWrap.addView(nav, lp(-1, -1));
        navItem(nav, 0, "СТАТУС", "pulse");
        navItem(nav, 1, "ПОДІЇ", "history");
        navItem(nav, 2, "SETTINGS", "settings");
    }
    private void navItem(LinearLayout nav, int index, String label, String icon) {
        boolean current = tab == index;
        LinearLayout item = horizontal();
        item.setGravity(Gravity.CENTER);
        if (current) item.setBackground(bg(Color.rgb(39, 63, 69), 16, Color.rgb(49, 96, 86)));
        Glyph g = new Glyph(icon, current ? MINT : MUTED);
        item.addView(g, lp(22, 22));
        TextView labelView = t(label, 10, current ? TEXT : MUTED, true);
        labelView.setLetterSpacing(.08f);
        labelView.setPadding(d(5), 0, 0, 0);
        item.addView(labelView);
        item.setOnClickListener(v -> { tab = index; render(); });
        nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void sectionHeading(String overline, String title) {
        TextView over = t(overline, 11, MINT, true);
        over.setLetterSpacing(.19f);
        contents.addView(over);
        gap(contents, 9);
        TextView name = t(title, 30, TEXT, true);
        contents.addView(name);
        gap(contents, 20);
    }
    private void dashboard() {
        sectionHeading("ВАША БЕЗПЕКА", "Поточна ситуація");
        long ts = Prefs.get(this).getLong(Prefs.KEY_LAST_CHECK, 0);
        long age = System.currentTimeMillis() - ts;
        String error = Prefs.get(this).getString(Prefs.KEY_ERROR, "");
        boolean configured = !Prefs.token(this).isEmpty();
        boolean current = configured && ts > 0 && age >= 0 && age < 4 * 60_000L && error.isEmpty();
        boolean active = current && Prefs.get(this).getBoolean(Prefs.KEY_LAST_ACTIVE, false);
        int signal = !current ? AMBER : active ? RED : MINT;
        String status = !current ? "СТАТУС НЕВІДОМИЙ" : active ? "ПОВІТРЯНА ТРИВОГА" : "ТРИВОГИ НЕМАЄ";
        String details = !configured ? "Додайте API-токен у Settings"
                : !current ? (error.isEmpty() ? "Очікуємо актуальні дані" : error)
                : active ? "Загроза для вибраної місцевості" : "За останніми доступними даними";

        LinearLayout hero = vertical();
        hero.setBackground(gradient(active ? Color.rgb(69, 35, 47) : Color.rgb(20, 49, 53),
                active ? Color.rgb(32, 27, 42) : Color.rgb(18, 31, 44), 28));
        pad(hero, 24, 24);
        contents.addView(hero, lp(-1, -2));
        LinearLayout top = horizontal();
        hero.addView(top, lp(-1, 38));
        TextView kicker = t("СТАН ПОВІТРЯ", 11, MUTED, true);
        kicker.setLetterSpacing(.18f);
        top.addView(kicker, new LinearLayout.LayoutParams(0, -2, 1));
        TextView live = t(current ? "●  АКТУАЛЬНО" : "●  НЕМАЄ ДАНИХ", 10, signal, true);
        live.setLetterSpacing(.05f);
        top.addView(live);

        FrameLayout radarSlot = new FrameLayout(this);
        hero.addView(radarSlot, lp(-1, 174));
        Radar radar = new Radar(signal, active, current);
        FrameLayout.LayoutParams radarLp = new FrameLayout.LayoutParams(d(174), d(174), Gravity.CENTER);
        radarSlot.addView(radar, radarLp);
        gap(hero, 3);
        TextView statusView = t(status, active ? 23 : 21, TEXT, true);
        statusView.setLetterSpacing(.012f);
        statusView.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(statusView, lp(-1, -2));
        gap(hero, 9);
        TextView detailsView = t(details, 13, MUTED, false);
        detailsView.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.addView(detailsView, lp(-1, -2));
        gap(hero, 22);
        View separator = new View(this);
        separator.setBackgroundColor(Color.rgb(55, 72, 78));
        hero.addView(separator, lp(-1, 1));
        gap(hero, 16);
        LinearLayout row = horizontal();
        hero.addView(row);
        Glyph pin = new Glyph("pin", MINT);
        row.addView(pin, lp(20, 22));
        TextView city = t("  " + Prefs.area(this).city, 15, TEXT, true);
        row.addView(city, new LinearLayout.LayoutParams(0, -2, 1));
        TextView check = t(current ? "Оновлено " + formatClock(ts) : "Потрібна перевірка", 10, MUTED, false);
        row.addView(check);

        gap(contents, 18);
        LinearLayout m = card(contents, 20);
        LinearLayout indicator = horizontal();
        m.addView(indicator);
        Glyph shield = new Glyph("shield", Prefs.ready(this) ? MINT : AMBER);
        indicator.addView(shield, lp(38, 38));
        LinearLayout statusTxt = vertical();
        statusTxt.setPadding(d(10), 0, 0, 0);
        statusTxt.addView(t(Prefs.ready(this) ? "Моніторинг налаштований" : "Моніторинг вимкнений", 15, TEXT, true));
        gap(statusTxt, 5);
        statusTxt.addView(t(Prefs.ready(this) ? "Перевірка приблизно кожні 45 секунд" : "Увімкніть у налаштуваннях", 12, MUTED, false));
        indicator.addView(statusTxt, new LinearLayout.LayoutParams(0, -2, 1));
        TextView dot = t("●", 18, Prefs.ready(this) ? MINT : AMBER, true);
        indicator.addView(dot);

        if (!notificationsGranted()) {
            gap(contents, 13);
            LinearLayout warning = card(contents, 16);
            warning.addView(t("⚠  Системні сповіщення заборонені", 14, AMBER, true));
            gap(warning, 8);
            warning.addView(t("Дозвольте їх у налаштуваннях телефону, інакше попередження про початок та відбій не з’являться.", 12, MUTED, false));
            gap(warning, 10);
            Button fix = actionButton("ВІДКРИТИ ДОЗВОЛИ", false);
            fix.setOnClickListener(v -> openNotificationSettings());
            warning.addView(fix, lp(-1, 44));
        }
        gap(contents, 18);
        Button update = actionButton(manualBusy ? "ПЕРЕВІРКА..." : "↻  ПЕРЕВІРИТИ ЗАРАЗ", true);
        update.setEnabled(!manualBusy);
        update.setOnClickListener(v -> checkNow());
        contents.addView(update, lp(-1, 55));
        gap(contents, 12);
        TextView note = t("Дані: alerts.in.ua. Інтернет і фоновий доступ потрібні. Цей застосунок не замінює офіційні сирени та укриття.", 12, MUTED, false);
        note.setLineSpacing(d(3), 1f);
        contents.addView(note);
    }

    private void events() {
        sectionHeading("ІСТОРІЯ ПЕРЕХОДІВ", "Останні події");
        JSONArray events = EventStore.get(this);
        LinearLayout explanation = card(contents, 20);
        explanation.addView(t("Зафіксовані зміни стану", 16, TEXT, true));
        gap(explanation, 8);
        explanation.addView(t("Тут зберігаються лише переходи, які застосунок встиг перевірити. Історія локальна, до 40 записів.", 12, MUTED, false));
        gap(contents, 17);
        if (events.length() == 0) {
            LinearLayout empty = card(contents, 24);
            Glyph history = new Glyph("history", MINT);
            empty.addView(history, lp(45, 45));
            gap(empty, 14);
            empty.addView(t("Тут поки тихо", 22, TEXT, true));
            gap(empty, 7);
            empty.addView(t("Коли монітор зафіксує початок або відбій, подія зʼявиться тут.", 13, MUTED, false));
            return;
        }
        for (int i = 0; i < events.length(); i++) {
            JSONObject obj = events.optJSONObject(i);
            if (obj == null) continue;
            boolean isActive = obj.optBoolean("active");
            LinearLayout entry = card(contents, 17);
            LinearLayout line = horizontal();
            entry.addView(line);
            Glyph g = new Glyph(isActive ? "alert" : "check", isActive ? RED : MINT);
            line.addView(g, lp(38, 38));
            LinearLayout center = vertical();
            center.setPadding(d(12), 0, 0, 0);
            center.addView(t(isActive ? "Початок тривоги" : "Відбій тривоги", 15, TEXT, true));
            gap(center, 4);
            center.addView(t(obj.optString("area", "") + " · " + formatFull(obj.optLong("when", 0)), 11, MUTED, false));
            line.addView(center, new LinearLayout.LayoutParams(0, -2, 1));
            gap(contents, 9);
        }
    }

    private EditText cityField, communityField, districtField, oblastField, tokenField;
    private Switch monitoringSwitch, startSwitch, endSwitch;

    private void settings() {
        sectionHeading("ПЕРСОНАЛІЗАЦІЯ", "Налаштування");
        LinearLayout loc = card(contents, 20);
        fieldTitle(loc, "01  ·  ЛОКАЦІЯ", "Місцевість, для якої стежимо");
        AlertMatcher.Area area = Prefs.area(this);
        cityField = field(loc, "Населений пункт", area.city, false);
        communityField = field(loc, "Громада", area.community, false);
        districtField = field(loc, "Район", area.district, false);
        oblastField = field(loc, "Область", area.oblast, false);
        gap(loc, 10);
        Button defaultArea = actionButton("↶  ВІДНОВИТИ ЩАСЛИВЕ", false);
        loc.addView(defaultArea, lp(-1, 47));
        defaultArea.setOnClickListener(v -> {
            cityField.setText("Щасливе");
            communityField.setText("Пристолична громада");
            districtField.setText("Бориспільський район");
            oblastField.setText("Київська область");
        });
        gap(loc, 12);
        TextView hint = t("Дані про тривоги привʼязані до адміністративних зон. Заповніть громаду, район та область точно, інакше можна пропустити місцеву тривогу.", 12, MUTED, false);
        hint.setLineSpacing(d(2), 1f);
        loc.addView(hint);

        gap(contents, 16);
        LinearLayout api = card(contents, 20);
        fieldTitle(api, "02  ·  ДЖЕРЕЛО", "Ключ доступу до API");
        tokenField = field(api, "API-токен alerts.in.ua", Prefs.token(this), true);
        gap(api, 12);
        Button docs = actionButton("ОТРИМАТИ ТОКЕН   ↗", false);
        api.addView(docs, lp(-1, 48));
        docs.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://devs.alerts.in.ua/"))));
        gap(api, 10);
        api.addView(t("Токен зберігається лише на телефоні. Його не буде у вихідному коді або APK.", 12, MUTED, false));

        gap(contents, 16);
        LinearLayout opts = card(contents, 20);
        fieldTitle(opts, "03  ·  МОНІТОРИНГ", "Сповіщення та робота у фоні");
        monitoringSwitch = option(opts, "Фоновий моніторинг", "Постійний індикатор та регулярні перевірки", Prefs.monitoring(this));
        gap(opts, 5);
        startSwitch = option(opts, "Початок тривоги", "Системне сповіщення зі звуком", Prefs.get(this).getBoolean(Prefs.KEY_START, true));
        gap(opts, 5);
        endSwitch = option(opts, "Відбій тривоги", "Системне сповіщення про завершення", Prefs.get(this).getBoolean(Prefs.KEY_END, true));
        gap(opts, 13);
        Button systemNotifications = actionButton("СИСТЕМНІ ДОЗВОЛИ СПОВІЩЕНЬ   ↗", false);
        opts.addView(systemNotifications, lp(-1, 48));
        systemNotifications.setOnClickListener(v -> openNotificationSettings());
        gap(opts, 8);
        Button test = actionButton("НАДІСЛАТИ ТЕСТ СПОВІЩЕННЯ", false);
        opts.addView(test, lp(-1, 48));
        test.setOnClickListener(v -> {
            if (!notificationsGranted()) { openNotificationSettings(); return; }
            Notify.test(this);
            Toast.makeText(this, "Тестове сповіщення відправлено", Toast.LENGTH_SHORT).show();
        });
        gap(opts, 8);
        Button battery = actionButton("НАЛАШТУВАННЯ БАТАРЕЇ   ↗", false);
        opts.addView(battery, lp(-1, 48));
        battery.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
            catch (ActivityNotFoundException ex) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
        });
        gap(opts, 11);
        opts.addView(t("Дозвольте сповіщення та необмежену роботу від батареї. Деякі телефони все одно можуть призупиняти фонові запити.", 12, MUTED, false));

        gap(contents, 19);
        Button save = actionButton("ЗБЕРЕГТИ НАЛАШТУВАННЯ", true);
        contents.addView(save, lp(-1, 56));
        save.setOnClickListener(v -> saveSettings());
        gap(contents, 12);
        TextView version = t("TRYVOHA LOCAL    /    VERSION 1.0.0", 10, MUTED, true);
        version.setGravity(Gravity.CENTER);
        version.setLetterSpacing(.17f);
        contents.addView(version);
    }

    private void fieldTitle(LinearLayout parent, String overline, String name) {
        TextView label = t(overline, 10, MINT, true);
        label.setLetterSpacing(.15f);
        parent.addView(label);
        gap(parent, 8);
        parent.addView(t(name, 17, TEXT, true));
        gap(parent, 18);
    }
    private EditText field(LinearLayout parent, String label, String value, boolean password) {
        TextView title = t(label, 12, MUTED, true);
        parent.addView(title);
        gap(parent, 8);
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setSelectAllOnFocus(false);
        input.setPadding(d(15), 0, d(15), 0);
        input.setBackground(bg(Color.rgb(11, 22, 33), 13, BORDER));
        if (password) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        parent.addView(input, lp(-1, 48));
        gap(parent, 15);
        return input;
    }
    private Switch option(LinearLayout parent, String name, String explanation, boolean checked) {
        LinearLayout row = horizontal();
        row.setMinimumHeight(d(68));
        parent.addView(row, lp(-1, -2));
        LinearLayout labels = vertical();
        labels.addView(t(name, 14, TEXT, true));
        gap(labels, 4);
        TextView sub = t(explanation, 11, MUTED, false);
        sub.setMaxLines(2);
        labels.addView(sub);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{MINT, MUTED}));
        toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{Color.rgb(43, 105, 90), Color.rgb(57, 68, 79)}));
        toggle.setChecked(checked);
        row.addView(toggle);
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        return toggle;
    }
    private String val(EditText input) { return input.getText().toString().trim(); }
    private void saveSettings() {
        if (val(cityField).isEmpty() || val(communityField).isEmpty()
                || val(districtField).isEmpty() || val(oblastField).isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Заповніть локацію")
                    .setMessage("Потрібні назви населеного пункту, громади, району та області.")
                    .setPositiveButton("Зрозуміло", null).show();
            return;
        }
        if (monitoringSwitch.isChecked() && val(tokenField).isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Потрібен API-токен")
                    .setMessage("Вставте токен alerts.in.ua або вимкніть фоновий моніторинг.")
                    .setPositiveButton("Зрозуміло", null).show();
            return;
        }
        AlertMatcher.Area prev = Prefs.area(this);
        String prevToken = Prefs.token(this);
        boolean prevEnabled = Prefs.monitoring(this);
        boolean changed = !prev.city.equals(val(cityField)) || !prev.community.equals(val(communityField))
                || !prev.district.equals(val(districtField)) || !prev.oblast.equals(val(oblastField))
                || !prevToken.equals(val(tokenField));
        Prefs.get(this).edit()
                .putString(Prefs.KEY_CITY, val(cityField))
                .putString(Prefs.KEY_COMMUNITY, val(communityField))
                .putString(Prefs.KEY_DISTRICT, val(districtField))
                .putString(Prefs.KEY_OBLAST, val(oblastField))
                .putString(Prefs.KEY_TOKEN, val(tokenField))
                .putBoolean(Prefs.KEY_ENABLED, monitoringSwitch.isChecked())
                .putBoolean(Prefs.KEY_START, startSwitch.isChecked())
                .putBoolean(Prefs.KEY_END, endSwitch.isChecked())
                .apply();
        if (changed || (!prevEnabled && monitoringSwitch.isChecked())) Prefs.invalidate(this);
        MonitorService.update(this);
        Toast.makeText(this, "Налаштування збережено", Toast.LENGTH_SHORT).show();
        tab = 0;
        render();
    }

    private void checkNow() {
        String token = Prefs.token(this);
        if (token.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Потрібен API-токен")
                    .setMessage("Відкрийте Settings, отримайте персональний токен alerts.in.ua і збережіть його.")
                    .setNegativeButton("Закрити", null)
                    .setPositiveButton("Settings", (a,b) -> { tab = 2; render(); }).show();
            return;
        }
        if (Prefs.monitoring(this)) {
            MonitorService.update(this);
            Toast.makeText(this, "Запит на перевірку надіслано", Toast.LENGTH_SHORT).show();
            return;
        }
        manualBusy = true;
        render();
        AlertMatcher.Area requestedArea = Prefs.area(this);
        new Thread(() -> {
            String error = null;
            Boolean active = null;
            try { active = AlertsClient.fetch(token, requestedArea).active; }
            catch (AlertsClient.ApiException e) { error = e.getMessage(); }
            catch (Exception e) { error = "Не вдалося отримати дані"; }
            final String outcomeError = error;
            final Boolean outcomeActive = active;
            runOnUiThread(() -> {
                // Don't reuse an in-flight result for a different location or token.
                AlertMatcher.Area now = Prefs.area(this);
                if (!token.equals(Prefs.token(this)) || !requestedArea.city.equals(now.city)
                        || !requestedArea.community.equals(now.community)
                        || !requestedArea.district.equals(now.district)
                        || !requestedArea.oblast.equals(now.oblast)) {
                    manualBusy = false;
                    render();
                    return;
                }
                if (outcomeActive != null) {
                    Prefs.get(this).edit().putLong(Prefs.KEY_LAST_CHECK, System.currentTimeMillis())
                            .putBoolean(Prefs.KEY_LAST_ACTIVE, outcomeActive)
                            .remove(Prefs.KEY_ERROR).apply();
                } else {
                    Prefs.get(this).edit().putString(Prefs.KEY_ERROR, outcomeError).apply();
                }
                manualBusy = false;
                if (!isFinishing()) render();
            });
        }, "manual-check").start();
    }

    private boolean notificationsGranted() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        }
    }

    private Button actionButton(String name, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(name);
        button.setTextSize(12);
        button.setLetterSpacing(.08f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? Color.rgb(7, 28, 29) : MINT);
        button.setBackground(bg(primary ? MINT : Color.rgb(23, 47, 50), 15,
                primary ? Color.rgb(130, 251, 207) : Color.rgb(42, 81, 78)));
        button.setElevation(d(primary ? 3 : 0));
        return button;
    }
    private static String formatClock(long time) { return new SimpleDateFormat("HH:mm", UK).format(new Date(time)); }
    private static String formatFull(long time) { return new SimpleDateFormat("dd.MM · HH:mm", UK).format(new Date(time)); }

    /** All visual ornament is drawn locally: no remote images or external libraries. */
    private final class Radar extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color;
        private final boolean active, valid;
        Radar(int color, boolean active, boolean valid) { super(MainActivity.this); this.color=color; this.active=active; this.valid=valid; }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight(), cx=w/2, cy=h/2;
            float r=Math.min(w,h)*.42f;
            p.setStyle(Paint.Style.STROKE);
            for (int i=0;i<3;i++) {
                p.setColor(Color.argb(24+i*13, Color.red(color),Color.green(color),Color.blue(color)));
                p.setStrokeWidth(d(1));
                c.drawCircle(cx, cy, r*(.72f+i*.15f), p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(30,Color.red(color),Color.green(color),Color.blue(color)));
            c.drawCircle(cx,cy,r*.69f,p);
            p.setColor(color);
            c.drawCircle(cx,cy,r*.55f,p);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(d(7));
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.rgb(11,27,34));
            Path path=new Path();
            if (!valid || active) {
                path.moveTo(cx,cy-d(25));path.lineTo(cx,cy+d(7));
                c.drawPath(path,p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx,cy+d(23),d(4),p);
            } else {
                path.moveTo(cx-d(22),cy);path.lineTo(cx-d(6),cy+d(15));path.lineTo(cx+d(25),cy-d(19));
                c.drawPath(path,p);
            }
        }
    }
    private final class Glyph extends View {
        private final String kind;
        private final int color;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Glyph(String kind,int color){super(MainActivity.this);this.kind=kind;this.color=color;}
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float s=Math.min(getWidth(),getHeight())*.65f, unit=s/24f;
            canvas.save();canvas.translate(getWidth()/2f-s/2, getHeight()/2f-s/2);canvas.scale(unit,unit);
            p.setColor(color);p.setStrokeWidth(2.1f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            Path q=new Path();
            switch(kind){
                case "radar": canvas.drawCircle(12,12,8,p);canvas.drawCircle(12,12,3,p);canvas.drawLine(12,12,19,5,p);break;
                case "pulse": q.moveTo(2,12);q.lineTo(6,12);q.lineTo(9,6);q.lineTo(13,19);q.lineTo(16,11);q.lineTo(22,11);canvas.drawPath(q,p);break;
                case "history": canvas.drawArc(new RectF(3,3,21,21),-70,310,false,p);q.moveTo(3,4);q.lineTo(3,10);q.lineTo(9,10);canvas.drawPath(q,p);canvas.drawLine(12,7,12,12,p);canvas.drawLine(12,12,16,14,p);break;
                case "settings": canvas.drawCircle(12,12,8,p);canvas.drawCircle(12,12,3,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine((float)(12+9*Math.cos(a)),(float)(12+9*Math.sin(a)),(float)(12+11*Math.cos(a)),(float)(12+11*Math.sin(a)),p);}break;
                case "pin": q.moveTo(12,22);q.cubicTo(4,14,4,11,4,9);q.cubicTo(4,-1,20,-1,20,9);q.cubicTo(20,11,20,14,12,22);canvas.drawPath(q,p);canvas.drawCircle(12,9,2.4f,p);break;
                case "shield": q.moveTo(12,2);q.lineTo(21,6);q.lineTo(21,12);q.cubicTo(21,17,17,20,12,22);q.cubicTo(7,20,3,17,3,12);q.lineTo(3,6);q.close();canvas.drawPath(q,p);canvas.drawLine(8,12,11,15,p);canvas.drawLine(11,15,16,9,p);break;
                case "alert": canvas.drawCircle(12,12,9,p);canvas.drawLine(12,7,12,13,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(12,17,1.4f,p);break;
                case "check": canvas.drawCircle(12,12,9,p);q.moveTo(7,12);q.lineTo(11,16);q.lineTo(17,9);canvas.drawPath(q,p);break;
            }
            canvas.restore();
        }
    }
}
