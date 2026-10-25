package dev.wattscope;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

/** Public Android settings only. Opening a page never records a permission grant. */
final class BackgroundSettings {
    private BackgroundSettings() { }

    static Boolean exemption(Context context) {
        try {
            PowerManager manager = context.getSystemService(PowerManager.class);
            return manager == null ? null : manager.isIgnoringBatteryOptimizations(context.getPackageName());
        } catch (RuntimeException unavailable) { return null; }
    }

    static void requestExemption(Activity activity) {
        if (Boolean.TRUE.equals(exemption(activity))) return;
        if (launch(activity, packageIntent(activity, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))) return;
        notify(activity, R.string.bg_fallback_list);
        openOptimizationList(activity);
    }

    static void openOptimizationList(Activity activity) {
        if (launch(activity, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) return;
        notify(activity, R.string.bg_fallback_app);
        openApp(activity);
    }

    static void openApp(Activity activity) {
        if (launch(activity, packageIntent(activity, Settings.ACTION_APPLICATION_DETAILS_SETTINGS))) return;
        notify(activity, R.string.bg_fallback_phone);
        openPhone(activity);
    }

    static void openPhone(Activity activity) {
        if (!launch(activity, new Intent(Settings.ACTION_SETTINGS))) notify(activity, R.string.bg_fallback_unavailable);
    }

    private static Intent packageIntent(Activity activity, String action) {
        return new Intent(action, Uri.parse("package:" + activity.getPackageName()));
    }

    private static boolean launch(Activity activity, Intent intent) {
        try { activity.startActivity(intent); return true; }
        catch (ActivityNotFoundException | SecurityException unavailable) { return false; }
    }

    private static void notify(Activity activity, int text) {
        Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
    }
}
