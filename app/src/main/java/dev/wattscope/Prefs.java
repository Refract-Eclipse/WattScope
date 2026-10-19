package dev.wattscope;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private Prefs() { }
    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences("meter", Context.MODE_PRIVATE);
    }
    public static int interval(Context context) { return get(context).getInt("interval", 1000); }
    public static boolean overlay(Context context) { return get(context).getBoolean("overlay", false); }
    public static int overlayScale(Context context) { return OverlayStyle.clamp(get(context).getInt("overlay_scale", 100)); }
    public static long chartRange(Context context) { return ChartData.clampRange(get(context).getLong("chart_range", 60000)); }
}
