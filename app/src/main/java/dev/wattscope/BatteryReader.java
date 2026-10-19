package dev.wattscope;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;

public final class BatteryReader {
    private final Context context;
    private final BatteryManager manager;

    public BatteryReader(Context context) {
        this.context = context.getApplicationContext();
        this.manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }

    public PowerSample read() {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        SharedPreferences p = Prefs.get(context);
        int raw = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        String source = "瞬时电流";
        if (raw == Integer.MIN_VALUE) {
            raw = property(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
            source = raw == Integer.MIN_VALUE ? "接口未提供电流" : "硬件平均电流";
        }
        int millivolts = extra(battery, BatteryManager.EXTRA_VOLTAGE, -1);
        int temperature = extra(battery, BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        int level = extra(battery, BatteryManager.EXTRA_LEVEL, -1);
        int scale = extra(battery, BatteryManager.EXTRA_SCALE, 100);
        int percent = level < 0 || scale <= 0 ? -1 : Math.min(100, (int) Math.round(100.0 * level / scale));
        double voltage = millivolts <= 0 ? Double.NaN : millivolts / 1000.0 * p.getInt("voltage_factor", 1);
        double current = PowerMath.currentMa(raw, p.getBoolean("current_ma", false), p.getBoolean("reverse", false));
        String issue = raw == Integer.MIN_VALUE ? "手机未开放电流传感器，无法估算功率。" : "";
        if (millivolts <= 0) issue = "系统未提供有效的电池电压。";
        if (!Double.isFinite(PowerMath.watts(voltage, current)) && issue.isEmpty()) {
            issue = "读数超出合理范围，请检查校准设置。";
        }
        return new PowerSample(System.currentTimeMillis(), SystemClock.elapsedRealtime(), voltage, current,
                temperature == Integer.MIN_VALUE ? Double.NaN : temperature / 10.0,
                percent, extra(battery, BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
                extra(battery, BatteryManager.EXTRA_PLUGGED, 0), raw, source, issue);
    }

    private int property(int id) {
        try { return manager == null ? Integer.MIN_VALUE : manager.getIntProperty(id); }
        catch (RuntimeException unsupported) { return Integer.MIN_VALUE; }
    }

    private static int extra(Intent intent, String key, int fallback) {
        return intent == null ? fallback : intent.getIntExtra(key, fallback);
    }
}
