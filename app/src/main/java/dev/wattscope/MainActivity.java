package dev.wattscope;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 10, REQUEST_CSV = 11;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status, headline, power, direction, current, voltage, capacity, temperature;
    private TextView average, peak, energy, coverage, permission, diagnostics, chartWindowNote, scaleValue;
    private static final long[] RANGE_VALUES = {60000, 300000, 900000, 3600000, 0};
    private final Button[] rangeButtons = new Button[5];
    private SeekBar scaleSlider;
    private boolean sizingOverlay;
    private long renderedRange = -1;
    private Boolean renderedRunning;
    private ChartData.Mode renderedMode;
    private TextView chartTitle, chargeLegend, dischargeLegend, neutralLegend;
    private Button start, interval, compact;
    private Switch floating;
    private PowerChart chart;
    private boolean rendering, overlayPending, startPending;
    private PowerSample renderedSample;
    private boolean backgroundIntroPending, requestBackgroundOnStart;
    private BackgroundGuideDialog backgroundGuide;
    private boolean backgroundGuideOpen;
    private TextView backgroundStatus;

    private final Runnable refresh = new Runnable() {
        @Override public void run() { render(); handler.postDelayed(this, 500); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            overlayPending = state.getBoolean("overlayPending");
            startPending = state.getBoolean("startPending");
            backgroundIntroPending = state.getBoolean("backgroundIntroPending");
            requestBackgroundOnStart = state.getBoolean("requestBackgroundOnStart");
            backgroundGuideOpen = state.getBoolean("backgroundGuideOpen");
        }
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        buildUi();
        if (backgroundGuideOpen) showBackgroundGuide(backgroundIntroPending);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = Ui.column(this);
        int margin = Ui.dp(this, 20);
        root.setPadding(margin, Ui.dp(this, 18), margin, Ui.dp(this, 30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                scroll.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                scroll.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(scroll);
        scroll.requestApplyInsets();

        LinearLayout mast = row();
        android.widget.ImageView mark = new android.widget.ImageView(this);
        mark.setImageResource(R.drawable.ic_power);
        mark.setImageTintList(ColorStateList.valueOf(Ui.GREEN));
        mark.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        mark.setBackground(Ui.outlined(this, 0xFF192B47, 0xFF294262, 14));
        mast.addView(mark, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        gap(mast, 12);
        LinearLayout brand = Ui.column(this);
        brand.addView(Ui.text(this, "瓦特计", 24, Ui.TEXT, true));
        Ui.space(brand, 5);
        TextView subtitle = Ui.text(this, "WATT SCOPE", 9, Ui.MUTED, false);
        subtitle.setLetterSpacing(0.17f);
        brand.addView(subtitle);
        mast.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(mast);
        Ui.space(root, 24);

        LinearLayout hero = card();
        hero.setPadding(Ui.dp(this, 22), Ui.dp(this, 21), Ui.dp(this, 22), Ui.dp(this, 21));
        hero.setBackground(new MeterBackdrop(this));
        LinearLayout top = row();
        headline = Ui.text(this, "电池侧实时功率", 13, 0xFFB1C5E7, false);
        top.addView(headline, new LinearLayout.LayoutParams(0, -2, 1));
        status = Ui.text(this, "待启动", 10, Ui.MUTED, false);
        status.setBackground(Ui.rounded(this, 0x5528436B, 12));
        status.setPadding(Ui.dp(this, 9), Ui.dp(this, 5), Ui.dp(this, 9), Ui.dp(this, 5));
        top.addView(status);
        hero.addView(top);
        Ui.space(hero, 14);
        LinearLayout digits = row();
        digits.setGravity(Gravity.BOTTOM);
        power = Ui.text(this, "—", 68, Ui.TEXT, false);
        power.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        power.setSingleLine(true);
        digits.addView(power, new LinearLayout.LayoutParams(-2, -2));
        TextView watt = Ui.text(this, " W", 22, Ui.GREEN, true);
        watt.setPadding(0, 0, 0, Ui.dp(this, 10));
        digits.addView(watt);
        hero.addView(digits);
        Ui.space(hero, 10);
        direction = Ui.text(this, "开启监测，看见手机的每一瓦", 12, 0xFFA9BDDC, false);
        hero.addView(direction);
        root.addView(hero);
        Ui.space(root, 20);

        LinearLayout section = row();
        section.addView(Ui.text(this, "电池状态", 15, Ui.TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(section);
        Ui.space(root, 12);
        LinearLayout first = row();
        current = metric(first, "电流  /  CURRENT", "— mA");
        gap(first, 10);
        voltage = metric(first, "电压  /  VOLTAGE", "— V");
        root.addView(first);
        Ui.space(root, 10);
        LinearLayout second = row();
        capacity = metric(second, "剩余电量  /  BATTERY", "— %");
        gap(second, 10);
        temperature = metric(second, "电池温度  /  TEMP", "— °C");
        root.addView(second);
        Ui.space(root, 15);
        LinearLayout runRow = row();
        start = button("开始监测", true);
        start.setOnClickListener(v -> {
            if (MonitorStore.running) stopService(new Intent(this, PowerMonitorService.class));
            else requestStart();
            handler.postDelayed(this::render, 250);
        });
        runRow.addView(start, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));
        gap(runRow, 10);
        interval = button("1 秒刷新", false);
        interval.setOnClickListener(v -> chooseInterval());
        runRow.addView(interval, new LinearLayout.LayoutParams(Ui.dp(this, 110), Ui.dp(this, 50)));
        root.addView(runRow);
        Ui.space(root, 22);

        LinearLayout trend = card();
        LinearLayout chartHeader = row();
        chartTitle = Ui.text(this, "电池净功率", 16, Ui.TEXT, true);
        chartHeader.addView(chartTitle, new LinearLayout.LayoutParams(0, -2, 1));
        Button custom = button("自定义", false);
        custom.setTextSize(11);
        custom.setOnClickListener(v -> chooseCustomRange());
        chartHeader.addView(custom, new LinearLayout.LayoutParams(Ui.dp(this, 75), Ui.dp(this, 36)));
        trend.addView(chartHeader);
        Ui.space(trend, 14);
        LinearLayout ranges = row();
        String[] labels = {"1 分", "5 分", "15 分", "1 时", "全部"};
        for (int i = 0; i < rangeButtons.length; i++) {
            final long value = RANGE_VALUES[i];
            Button chip = button(labels[i], false);
            chip.setTextSize(11);
            chip.setPadding(0, 0, 0, 0);
            chip.setOnClickListener(v -> {
                Prefs.get(this).edit().putLong("chart_range", value).apply();
                render();
            });
            rangeButtons[i] = chip;
            if (i > 0) gap(ranges, 5);
            ranges.addView(chip, new LinearLayout.LayoutParams(0, Ui.dp(this, 37), 1));
        }
        trend.addView(ranges);
        Ui.space(trend, 13);
        LinearLayout legend = row();
        chargeLegend = Ui.text(this, "● 净充电", 11, Ui.CYAN, true);
        legend.addView(chargeLegend, new LinearLayout.LayoutParams(0, -2, 1));
        dischargeLegend = Ui.text(this, "● 净放电", 11, Ui.ORANGE, true);
        legend.addView(dischargeLegend, new LinearLayout.LayoutParams(0, -2, 1));
        neutralLegend = Ui.text(this, "等待有效读数", 11, Ui.MUTED, false);
        legend.addView(neutralLegend, new LinearLayout.LayoutParams(0, -2, 1));
        trend.addView(legend);
        Ui.space(trend, 9);
        chartWindowNote = Ui.text(this, "最近 1 分钟 · 按住曲线查看读数", 10, Ui.MUTED, false);
        trend.addView(chartWindowNote);
        Ui.space(trend, 6);
        chart = new PowerChart(this);
        trend.addView(chart, new LinearLayout.LayoutParams(-1, Ui.dp(this, 184)));
        Ui.space(trend, 9);
        View divider = new View(this);
        divider.setBackgroundColor(Ui.LINE);
        trend.addView(divider, new LinearLayout.LayoutParams(-1, Ui.dp(this, 1)));
        Ui.space(trend, 17);
        LinearLayout stats = row();
        average = stat(stats, "平均幅值", "— W");
        peak = stat(stats, "峰值幅值", "— W");
        energy = stat(stats, "累计放电", "— Wh");
        trend.addView(stats);
        Ui.space(trend, 14);
        coverage = Ui.text(this, "尚未开始采样", 10, Ui.MUTED, false);
        trend.addView(coverage);
        root.addView(trend);
        Ui.space(root, 20);

        LinearLayout controls = card();
        LinearLayout floatingRow = row();
        LinearLayout floatingLabels = Ui.column(this);
        floatingLabels.addView(Ui.text(this, "屏幕悬浮窗", 16, Ui.TEXT, true));
        Ui.space(floatingLabels, 6);
        floatingLabels.addView(Ui.text(this, "拖动移动 · 轻点打开 · 长按隐藏", 10, Ui.MUTED, false));
        floatingRow.addView(floatingLabels, new LinearLayout.LayoutParams(0, -2, 1));
        floating = new Switch(this);
        floating.setContentDescription("开启屏幕功率悬浮窗");
        floating.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{Ui.GREEN, Ui.MUTED}));
        floating.setOnCheckedChangeListener((button, checked) -> handleOverlayToggle(checked));
        floatingRow.addView(floating);
        controls.addView(floatingRow);
        Ui.space(controls, 23);

        LinearLayout sizeRow = row();
        sizeRow.addView(Ui.text(this, "窗口大小", 13, Ui.TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
        scaleValue = Ui.text(this, "100%", 17, Ui.GREEN, true);
        sizeRow.addView(scaleValue);
        controls.addView(sizeRow);
        Ui.space(controls, 4);
        scaleSlider = new SeekBar(this);
        scaleSlider.setMax(OverlayStyle.MAX_PERCENT - OverlayStyle.MIN_PERCENT);
        scaleSlider.setProgress(Prefs.overlayScale(this) - OverlayStyle.MIN_PERCENT);
        scaleSlider.setProgressTintList(ColorStateList.valueOf(Ui.GREEN));
        scaleSlider.setThumbTintList(ColorStateList.valueOf(Ui.GREEN));
        scaleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int value = progress + OverlayStyle.MIN_PERCENT;
                Prefs.get(MainActivity.this).edit().putInt("overlay_scale", value).apply();
                scaleValue.setText(value + "%");
                bar.setContentDescription("悬浮窗大小 " + value + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { sizingOverlay = true; }
            @Override public void onStopTrackingTouch(SeekBar bar) { sizingOverlay = false; render(); }
        });
        controls.addView(scaleSlider, new LinearLayout.LayoutParams(-1, Ui.dp(this, 43)));
        LinearLayout scaleLabels = row();
        scaleLabels.addView(Ui.text(this, "70%", 10, Ui.MUTED, false));
        TextView liveResize = Ui.text(this, "拖动滑杆，即时缩放", 10, Ui.MUTED, false);
        liveResize.setGravity(Gravity.CENTER);
        scaleLabels.addView(liveResize, new LinearLayout.LayoutParams(0, -2, 1));
        scaleLabels.addView(Ui.text(this, "200%", 10, Ui.MUTED, false));
        controls.addView(scaleLabels);
        Ui.space(controls, 16);
        LinearLayout styles = row();
        compact = button("样式：标准", false);
        compact.setOnClickListener(v -> {
            SharedPreferences p = Prefs.get(this);
            p.edit().putBoolean("compact", !p.getBoolean("compact", false)).apply();
            render();
        });
        styles.addView(compact, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        gap(styles, 9);
        Button resetScale = button("恢复 100%", false);
        resetScale.setTextSize(12);
        resetScale.setOnClickListener(v -> {
            Prefs.get(this).edit().putInt("overlay_scale", 100).apply();
            render();
        });
        styles.addView(resetScale, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        controls.addView(styles);
        Ui.space(controls, 16);
        LinearLayout settingsRow = row();
        Button notificationSettings = button("管理通知权限", false);
        notificationSettings.setOnClickListener(v -> openNotificationSettings());
        settingsRow.addView(notificationSettings, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        gap(settingsRow, 9);
        Button screenOffSettings = button("息屏记录设置", false);
        screenOffSettings.setOnClickListener(v -> explainScreenOffRecording());
        settingsRow.addView(screenOffSettings, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
        controls.addView(settingsRow);
        root.addView(controls);
        Ui.space(root, 14);

        LinearLayout tools = row();
        Button calibrate = button("读数校准", false);
        calibrate.setOnClickListener(v -> calibrate());
        tools.addView(calibrate, new LinearLayout.LayoutParams(0, Ui.dp(this, 45), 1));
        gap(tools, 10);
        Button export = button("导出 CSV", false);
        export.setOnClickListener(v -> exportCsv());
        tools.addView(export, new LinearLayout.LayoutParams(0, Ui.dp(this, 45), 1));
        root.addView(tools);
        Ui.space(root, 10);
        Button explain = button("读数说明与诊断", false);
        root.addView(explain, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));
        LinearLayout help = Ui.column(this);
        help.setPadding(Ui.dp(this, 3), Ui.dp(this, 14), Ui.dp(this, 3), 0);
        permission = Ui.text(this, "", 12, Ui.MUTED, false);
        permission.setLineSpacing(Ui.dp(this, 3), 1);
        help.addView(permission);
        Ui.space(help, 10);
        backgroundStatus = Ui.text(this, "", 12, Ui.MUTED, false);
        backgroundStatus.setLineSpacing(Ui.dp(this, 3), 1);
        help.addView(backgroundStatus);
        Ui.space(help, 12);
        diagnostics = Ui.text(this, "", 10, Ui.MUTED, false);
        help.addView(diagnostics);
        Ui.space(help, 12);
        TextView note = Ui.text(this, "电池净功率 ≈ 电压 × 电流。正电流表示充入，负电流表示放出。充电时不能当作整机耗电或充电头功率。\n\n监测期间会保持 CPU 唤醒以支持息屏采样，屏幕可正常关闭；这会增加耗电并影响原本的待机状态。深度休眠和厂商后台限制仍可能中断采样，请检查息屏记录设置；缺失时段不计入能耗。最近 6 小时记录保留在本次监测的内存中，进程结束后不保留；需要保存时请导出 CSV。", 11, Ui.MUTED, false);
        note.setLineSpacing(Ui.dp(this, 4), 1);
        help.addView(note);
        help.setVisibility(View.GONE);
        explain.setOnClickListener(v -> help.setVisibility(help.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        root.addView(help);
    }

    private void handleOverlayToggle(boolean checked) {
        if (rendering) return;
        if (checked && !Settings.canDrawOverlays(this)) {
            rendering = true; floating.setChecked(false); rendering = false;
            overlayPending = true;
            try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
            catch (ActivityNotFoundException missing) { overlayPending = false; toast("请在系统设置中允许瓦特计显示悬浮窗。"); }
        } else {
            Prefs.get(this).edit().putBoolean("overlay", checked).apply();
            if (checked && !MonitorStore.running) requestStart();
        }
    }

    private void chooseCustomRange() {
        LinearLayout form = Ui.column(this);
        int padding = Ui.dp(this, 22);
        form.setPadding(padding, padding, padding, 0);
        form.addView(Ui.text(this, "显示最近多少分钟？可输入 1–360。\n切换范围不会清除已记录的曲线。", 13, Ui.MUTED, false));
        Ui.space(form, 12);
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setHint("分钟");
        input.setText(Long.toString(Prefs.chartRange(this) == 0 ? 60 : Prefs.chartRange(this) / 60000));
        input.setSelectAllOnFocus(true);
        form.addView(input, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("自定义曲线范围").setView(form)
                .setNegativeButton("取消", null).setPositiveButton("应用", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int minutes = Integer.parseInt(input.getText().toString().trim());
                if (minutes < 1 || minutes > 360) { input.setError("请输入 1–360 分钟"); return; }
                Prefs.get(this).edit().putLong("chart_range", minutes * 60000L).apply();
                render(); dialog.dismiss();
            } catch (NumberFormatException invalid) { input.setError("请输入 1–360 分钟"); }
        }));
        dialog.show();
    }

    private static String rangeLabel(long range) {
        if (range == 0) return "全部（最多 6 小时）";
        long minutes = range / 60000;
        return minutes >= 60 && minutes % 60 == 0 ? minutes / 60 + " 小时" : minutes + " 分钟";
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private void gap(LinearLayout row, int size) {
        row.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, size), 1));
    }

    private LinearLayout card() {
        LinearLayout card = Ui.column(this);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 17), Ui.dp(this, 18), Ui.dp(this, 17));
        card.setBackground(Ui.outlined(this, Ui.CARD, 0xFF213049, 21));
        return card;
    }

    private TextView metric(LinearLayout parent, String label, String initial) {
        LinearLayout tile = card();
        tile.addView(Ui.text(this, label, 9, Ui.MUTED, false));
        Ui.space(tile, 8);
        TextView value = Ui.text(this, initial, 23, Ui.TEXT, true);
        value.setSingleLine(true);
        value.setAutoSizeTextTypeUniformWithConfiguration(14, 23, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        tile.addView(value);
        parent.addView(tile, new LinearLayout.LayoutParams(0, -2, 1));
        return value;
    }

    private TextView stat(LinearLayout parent, String label, String initial) {
        LinearLayout cell = Ui.column(this);
        TextView value = Ui.text(this, initial, 15, Ui.TEXT, true);
        value.setSingleLine(true);
        value.setAutoSizeTextTypeUniformWithConfiguration(10, 15, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        cell.addView(value);
        Ui.space(cell, 5);
        cell.addView(Ui.text(this, label, 10, Ui.MUTED, false));
        parent.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
        return value;
    }

    private Button button(String title, boolean primary) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(13);
        button.setTextColor(primary ? Ui.BG : Ui.TEXT);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        button.setBackground(Ui.surface(this, primary ? 0xFF6299FF : 0xFF1C2C46, 13));
        button.setStateListAnimator(null);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return button;
    }

    private void requestStart() {
        if (MonitorStore.running || startPending || backgroundIntroPending) return;
        if (!Prefs.get(this).getBoolean("background_guide_seen_v1", false)) showBackgroundGuide(true);
        else requestNotificationsAndStart();
    }

    private void showBackgroundGuide(boolean firstStart) {
        if (backgroundGuide != null) return;
        backgroundIntroPending = firstStart;
        backgroundGuideOpen = true;
        backgroundGuide = new BackgroundGuideDialog(this, firstStart,
                () -> finishBackgroundIntro(true), () -> finishBackgroundIntro(false), () -> {
                    backgroundIntroPending = false;
                    backgroundGuideOpen = false;
                    backgroundGuide = null;
                });
        backgroundGuide.show();
    }

    private void finishBackgroundIntro(boolean requestAccess) {
        backgroundIntroPending = false;
        backgroundGuideOpen = false;
        Prefs.get(this).edit().putBoolean("background_intro_seen", true)
                .putBoolean("background_guide_seen_v1", true).apply();
        requestBackgroundOnStart = requestAccess;
        requestNotificationsAndStart();
    }

    private void updateBackgroundStatus() {
        Boolean exempt = BackgroundSettings.exemption(this);
        String value = getString(exempt == null ? R.string.bg_status_unknown : exempt ? R.string.bg_status_yes : R.string.bg_status_no);
        backgroundStatus.setText(getString(R.string.bg_diagnostics, value));
        if (backgroundGuide != null) backgroundGuide.refresh();
    }

    private void explainScreenOffRecording() { showBackgroundGuide(false); }

    private void requestBackgroundAccess() { BackgroundSettings.requestExemption(this); }

    private void requestNotificationsAndStart() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            startPending = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else startMonitoring();
    }

    private void startMonitoring() {
        startPending = false;
        boolean requestAccess = requestBackgroundOnStart;
        requestBackgroundOnStart = false;
        try {
            startForegroundService(new Intent(this, PowerMonitorService.class).setAction(PowerMonitorService.ACTION_START));
            if (requestAccess) requestBackgroundAccess();
        }
        catch (RuntimeException blocked) { toast("无法启动监测，请保持应用在前台后重试。"); }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_NOTIFICATIONS && startPending) {
            if (results.length == 0 || results[0] != PackageManager.PERMISSION_GRANTED) {
                toast("通知权限未开启，通知栏无法显示功率。可在管理通知权限中开启。");
            }
            startMonitoring();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        updateBackgroundStatus();
        if (overlayPending) {
            overlayPending = false;
            if (Settings.canDrawOverlays(this)) {
                Prefs.get(this).edit().putBoolean("overlay", true).apply();
                if (!MonitorStore.running) requestStart();
            } else toast("尚未允许悬浮窗，仍可使用主界面和通知栏监测。");
        }
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("overlayPending", overlayPending);
        state.putBoolean("startPending", startPending);
        state.putBoolean("backgroundIntroPending", backgroundIntroPending);
        state.putBoolean("requestBackgroundOnStart", requestBackgroundOnStart);
        state.putBoolean("backgroundGuideOpen", backgroundGuideOpen);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (backgroundGuide != null) backgroundGuide.dismiss();
        super.onDestroy();
    }

    private void render() {
        boolean running = MonitorStore.running;
        PowerSample s = MonitorStore.latest;
        if (renderedRunning == null || renderedRunning != running) {
            start.setText(running ? "停止监测" : "开始监测");
            start.setBackground(Ui.surface(this, running ? 0xFF253C5E : 0xFF6299FF, 14));
            start.setTextColor(running ? Ui.TEXT : 0xFF07152A);
            renderedRunning = running;
        }
        boolean stale = running && s != null && SystemClock.elapsedRealtime() - s.elapsedMs > Math.max(5000, Prefs.interval(this) * 3);
        status.setText(running ? stale ? "更新延迟" : "● 实时" : "已停止");
        status.setTextColor(running && !stale ? Ui.GREEN : Ui.MUTED);
        if (s != null) {
            headline.setText(s.valid() && s.signedWatts > 0 ? "电池净充入功率" : "电池侧实时功率");
            String value = s.valid() ? Ui.number(s.magnitude(), 2) : "—";
            power.setText(value);
            power.setTextSize(value.length() > 6 ? 50 : value.length() > 5 ? 58 : 68);
            power.setTextColor(s.signedWatts > 0 ? Ui.CYAN : Ui.TEXT);
            String message = !s.issue.isEmpty() ? s.issue : s.direction();
            direction.setText(stale ? "采样延迟 · 显示上次读数" : running ? message : "监测已停止 · 保留最后读数");
            direction.setTextColor(s.valid() ? 0xFFA9BDDC : Ui.ORANGE);
            current.setText(Ui.number(s.currentMa, 0) + " mA");
            voltage.setText(Ui.number(s.voltageV, 3) + " V");
            capacity.setText((s.percent < 0 ? "—" : s.percent) + " %");
            temperature.setText(Ui.number(s.temperatureC, 1) + " °C");
            String raw = s.rawCurrent == Integer.MIN_VALUE ? "未提供" : Integer.toString(s.rawCurrent);
            diagnostics.setText("数据来源：Android BatteryManager / ACTION_BATTERY_CHANGED\n原始电流：" + raw + " · 采样源：" + s.source
                    + "\n有效采样 " + Ui.number(MonitorStore.stats.observedSeconds, 0) + " 秒"
                    + "\n" + (MonitorStore.error.isEmpty() ? "传感器更新速度由手机硬件决定。" : MonitorStore.error));
        } else {
            headline.setText("电池侧实时功率");
            power.setText("—"); power.setTextColor(Ui.TEXT);
            direction.setText(MonitorStore.error.isEmpty() ? "开启监测，看见手机的每一瓦" : MonitorStore.error);
            current.setText("— mA"); voltage.setText("— V"); capacity.setText("— %"); temperature.setText("— °C");
            diagnostics.setText(MonitorStore.error);
        }
        SessionStats stats = MonitorStore.stats;
        energy.setText((s == null ? "—" : Ui.number(stats.dischargeWh, 3)) + " Wh");
        coverage.setText("累计充入 " + (s == null ? "—" : Ui.number(stats.chargeWh, 3)) + " Wh");
        interval.setText(Prefs.interval(this) / 1000 + " 秒刷新");
        compact.setText(Prefs.get(this).getBoolean("compact", false) ? "精简 · 切换标准" : "标准 · 切换精简");
        int scale = Prefs.overlayScale(this);
        if (!sizingOverlay) {
            scaleValue.setText(scale + "%");
            if (scaleSlider.getProgress() != scale - OverlayStyle.MIN_PERCENT) scaleSlider.setProgress(scale - OverlayStyle.MIN_PERCENT);
            scaleSlider.setContentDescription("悬浮窗大小 " + scale + "%");
        }
        rendering = true; floating.setChecked(Prefs.overlay(this)); rendering = false;
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = nm.getNotificationChannel("live_power");
        boolean canNotify = nm.areNotificationsEnabled() && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
        permission.setText("通知栏：" + (canNotify ? "已允许" : "未开启，可在“管理通知权限”中开启")
                + "  /  悬浮窗：" + (Settings.canDrawOverlays(this) ? "已授权" : "未授权"));
        long range = Prefs.chartRange(this);
        if (renderedRange != range) {
            for (int i = 0; i < rangeButtons.length; i++) {
                boolean chosen = RANGE_VALUES[i] == range;
                rangeButtons[i].setBackground(Ui.surface(this, chosen ? 0xFF304B78 : 0xFF1A2840, 10));
                rangeButtons[i].setTextColor(chosen ? Ui.TEXT : Ui.MUTED);
                rangeButtons[i].setSelected(chosen);
                rangeButtons[i].setContentDescription(rangeLabel(RANGE_VALUES[i]) + (chosen ? "，已选中" : ""));
            }
        }
        PowerSample first = MonitorStore.history.first();
        long recorded = first == null || s == null ? 0 : Math.max(0, s.elapsedMs - first.elapsedMs);
        String recordedText = recorded >= 3600000 ? Ui.number(recorded / 3600000.0, 1) + " 小时"
                : recorded >= 60000 ? Ui.number(recorded / 60000.0, 1) + " 分钟" : recorded / 1000 + " 秒";
        chartWindowNote.setText(rangeLabel(range) + "  ·  已记录 " + recordedText + "  ·  按住查看");
        if (renderedSample != s || renderedRange != range) { chart.refreshData(); renderedSample = s; renderedRange = range; }
        ChartData.Series visible = chart.currentSeries();
        average.setText(Ui.number(visible.averageW(), 2) + " W");
        peak.setText(Ui.number(visible.peakW(), 2) + " W");
        updateChartMode(visible.mode());
    }

    private void updateChartMode(ChartData.Mode mode) {
        if (renderedMode == mode) return;
        renderedMode = mode;
        boolean mixed = mode == ChartData.Mode.MIXED;
        boolean charging = mode == ChartData.Mode.CHARGING;
        boolean discharging = mode == ChartData.Mode.DISCHARGING;
        chartTitle.setText(mixed ? "充放电功率" : charging ? "充电功率" : discharging ? "放电功率" : "电池净功率");
        chargeLegend.setVisibility(mixed || charging ? View.VISIBLE : View.GONE);
        dischargeLegend.setVisibility(mixed || discharging ? View.VISIBLE : View.GONE);
        neutralLegend.setVisibility(mixed || charging || discharging ? View.GONE : View.VISIBLE);
        chargeLegend.setText(mixed ? "● + 净充电" : "● 净充电 · 幅值 W");
        dischargeLegend.setText(mixed ? "● − 净放电" : "● 净放电 · 幅值 W");
        dischargeLegend.setGravity(mixed ? Gravity.RIGHT : Gravity.LEFT);
        neutralLegend.setText(mode == ChartData.Mode.ZERO ? "● 零净功率" : "等待有效读数");
    }

    private void chooseInterval() {
        int[] values = {1000, 2000, 5000};
        String[] labels = {"1 秒 · 实时", "2 秒 · 均衡", "5 秒 · 低频"};
        int current = Prefs.interval(this), selected = current == 2000 ? 1 : current == 5000 ? 2 : 0;
        new AlertDialog.Builder(this).setTitle("采样间隔")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    Prefs.get(this).edit().putInt("interval", values[which]).apply();
                    dialog.dismiss(); render();
                }).setNegativeButton("取消", null).show();
    }

    private void calibrate() {
        SharedPreferences p = Prefs.get(this);
        LinearLayout settings = Ui.column(this);
        int pad = Ui.dp(this, 22);
        settings.setPadding(pad, pad, pad, 0);
        settings.addView(Ui.text(this, "默认遵循 Android 标准。确认接口单位或双电芯映射后再修改；保存会重置本次统计。", 13, Ui.MUTED, false));
        Ui.space(settings, 14);
        settings.addView(Ui.text(this, "原始电流单位", 14, Ui.TEXT, true));
        RadioGroup unit = new RadioGroup(this);
        RadioButton micro = radio("µA（标准，除以 1000）", 101);
        RadioButton milli = radio("mA（部分厂商接口）", 102);
        unit.addView(micro); unit.addView(milli);
        unit.check(p.getBoolean("current_ma", false) ? 102 : 101);
        settings.addView(unit);
        Switch reverse = new Switch(this);
        reverse.setText("反转电流正负号");
        reverse.setTextColor(Ui.TEXT);
        reverse.setChecked(p.getBoolean("reverse", false));
        settings.addView(reverse, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));
        Ui.space(settings, 8);
        settings.addView(Ui.text(this, "电压倍率（不自动猜测）", 14, Ui.TEXT, true));
        RadioGroup factor = new RadioGroup(this);
        factor.addView(radio("1× · 系统原始电压（默认）", 201));
        factor.addView(radio("2× · 已确认接口为单节电压", 202));
        factor.check(p.getInt("voltage_factor", 1) == 2 ? 202 : 201);
        settings.addView(factor);
        ScrollView scroll = new ScrollView(this); scroll.addView(settings);
        new AlertDialog.Builder(this).setTitle("读数校准").setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("恢复默认", (dialog, which) -> saveCalibration(false, false, 1))
                .setPositiveButton("保存", (dialog, which) -> saveCalibration(unit.getCheckedRadioButtonId() == 102, reverse.isChecked(), factor.getCheckedRadioButtonId() == 202 ? 2 : 1)).show();
    }

    private RadioButton radio(String label, int id) {
        RadioButton button = new RadioButton(this);
        button.setId(id); button.setText(label); button.setTextSize(13); button.setTextColor(Ui.TEXT);
        return button;
    }

    private void saveCalibration(boolean ma, boolean reverse, int factor) {
        SharedPreferences p = Prefs.get(this);
        boolean changed = p.getBoolean("current_ma", false) != ma || p.getBoolean("reverse", false) != reverse || p.getInt("voltage_factor", 1) != factor;
        p.edit().putBoolean("current_ma", ma).putBoolean("reverse", reverse).putInt("voltage_factor", factor).apply();
        if (changed && !MonitorStore.running) MonitorStore.reset(SystemClock.elapsedRealtime());
        toast("校准设置已保存"); render();
    }

    private void openNotificationSettings() {
        try { startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())); }
        catch (ActivityNotFoundException missing) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
    }

    private void exportCsv() {
        if (MonitorStore.exportHistory.isEmpty()) { toast("开始监测并获得读数后即可导出。"); return; }
        String name = "WattScope_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/csv").putExtra(Intent.EXTRA_TITLE, name);
        try { startActivityForResult(create, REQUEST_CSV); }
        catch (ActivityNotFoundException missing) { toast("手机未提供文件保存器，无法导出。"); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQUEST_CSV || result != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
            if (out == null) throw new java.io.IOException("无法写入文件");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write('\uFEFF');
            writer.write("time_utc,elapsed_ms,net_power_W,absolute_power_W,current_mA,voltage_V,battery_percent,temperature_C,source,issue\n");
            SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            utc.setTimeZone(TimeZone.getTimeZone("UTC"));
            for (PowerSample s : MonitorStore.exportHistory) {
                writer.write(utc.format(new Date(s.wallTimeMs)) + "," + s.elapsedMs + "," + csvNumber(s.signedWatts)
                        + "," + csvNumber(s.magnitude()) + "," + csvNumber(s.currentMa) + "," + csvNumber(s.voltageV)
                        + "," + (s.percent < 0 ? "" : s.percent) + "," + csvNumber(s.temperatureC) + "," + s.source + "," + s.issue + "\n");
            }
            writer.flush();
            toast("CSV 已保存（最近 6 小时，最多 21601 个采样点）");
        } catch (Exception failed) { toast("保存失败，请换一个文件位置重试。"); }
    }

    private static String csvNumber(double value) {
        return Double.isFinite(value) ? String.format(Locale.US, "%.6f", value) : "";
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
