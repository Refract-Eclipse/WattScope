package dev.wattscope;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

public final class PowerMonitorService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ACTION_START = "dev.wattscope.START";
    public static final String ACTION_STOP = "dev.wattscope.STOP";
    private static final String ACTION_HIDE = "dev.wattscope.HIDE";
    private static final String CHANNEL = "live_power";
    private static final int NOTIFICATION = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BatteryReader reader;
    private NotificationManager notifications;
    private OverlayController overlay;
    private boolean foreground;
    private static final long WAKE_LOCK_TIMEOUT_MS = 120000;
    private static final long WAKE_LOCK_RENEW_MS = 60000;
    private PowerManager.WakeLock samplingWakeLock;
    private long nextWakeLockRenewalMs;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!MonitorStore.running) return;
            try { keepCpuAwake(); }
            catch (RuntimeException denied) {
                MonitorStore.error = "无法保持息屏采样，监测已停止，请重新打开应用后重试。";
                stopMonitoring();
                return;
            }
            int interval = Prefs.interval(PowerMonitorService.this);
            try {
                PowerSample sample = reader.read();
                MonitorStore.append(sample, interval);
                MonitorStore.error = "";
                overlay.sync();
                overlay.update(sample);
                notifications.notify(NOTIFICATION, notification(sample));
            } catch (RuntimeException failure) {
                MonitorStore.error = "采样暂时失败：" + failure.getClass().getSimpleName();
            }
            handler.postDelayed(this, interval);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        reader = new BatteryReader(this);
        overlay = new OverlayController(this);
        notifications = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "实时功率监测", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("显示电池侧功率、电流、电压；点按可返回应用。");
        channel.setShowBadge(false);
        notifications.createNotificationChannel(channel);
        Prefs.get(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) { stopMonitoring(); return START_NOT_STICKY; }
        if (ACTION_HIDE.equals(action)) {
            Prefs.get(this).edit().putBoolean("overlay", false).apply();
            return START_NOT_STICKY;
        }
        if (!MonitorStore.running) {
            MonitorStore.reset(SystemClock.elapsedRealtime());
            try {
                Notification initial = notification(null);
                if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                else startForeground(NOTIFICATION, initial);
                foreground = true;
                keepCpuAwake();
                MonitorStore.running = true;
                handler.removeCallbacks(tick);
                handler.post(tick);
            } catch (RuntimeException denied) {
                MonitorStore.error = "无法启动后台监测，请重新打开应用后重试。";
                stopMonitoring();
            }
        }
        return START_NOT_STICKY;
    }

    private Notification notification(PowerSample sample) {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, PowerMonitorService.class).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = sample == null ? "瓦特计 · 正在读取传感器" : Ui.power(sample) + " · " + sample.direction();
        String line = sample == null ? "即将显示电流、电压和电池温度" :
                Ui.number(sample.currentMa, 0) + " mA  ·  " + Ui.number(sample.voltageV, 3) + " V  ·  "
                + (sample.percent < 0 ? "—" : sample.percent) + "%  ·  " + Ui.number(sample.temperatureC, 1) + " °C";
        String explanation = sample != null && !sample.issue.isEmpty() ? sample.issue
                : "电池侧净功率估算；充电时不代表整机耗电或充电器输入功率。";
        explanation += "\n息屏持续采样会增加耗电；不需要记录时请停止监测。";
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_power).setContentTitle(title).setContentText(line)
                .setStyle(new Notification.BigTextStyle().bigText(line + "\n" + explanation))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .setShowWhen(false).setColor(Ui.GREEN).setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(null, "停止监测", stop).build());
        if (Prefs.overlay(this)) {
            PendingIntent hide = PendingIntent.getService(this, 2,
                    new Intent(this, PowerMonitorService.class).setAction(ACTION_HIDE), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(new Notification.Action.Builder(null, "隐藏悬浮窗", hide).build());
        }
        if (Build.VERSION.SDK_INT >= 31) builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return builder.build();
    }

    private void keepCpuAwake() {
        if (samplingWakeLock == null) {
            PowerManager manager = getSystemService(PowerManager.class);
            samplingWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":recording");
            samplingWakeLock.setReferenceCounted(false);
        }
        long now = SystemClock.elapsedRealtime();
        if (!samplingWakeLock.isHeld() || now >= nextWakeLockRenewalMs) {
            samplingWakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
            nextWakeLockRenewalMs = now + WAKE_LOCK_RENEW_MS;
        }
    }

    private void releaseCpu() {
        if (samplingWakeLock != null && samplingWakeLock.isHeld()) samplingWakeLock.release();
        nextWakeLockRenewalMs = 0;
    }

    private void stopMonitoring() {
        MonitorStore.running = false;
        handler.removeCallbacks(tick);
        releaseCpu();
        overlay.remove();
        if (foreground) stopForeground(STOP_FOREGROUND_REMOVE);
        foreground = false;
        stopSelf();
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if ("overlay_scale".equals(key)) overlay.applySizing(true);
        if ("overlay".equals(key) || "compact".equals(key)) {
            overlay.remove(); overlay.sync();
            if (foreground) notifications.notify(NOTIFICATION, notification(MonitorStore.latest));
        }
        if ("current_ma".equals(key) || "reverse".equals(key) || "voltage_factor".equals(key)) {
            MonitorStore.reset(SystemClock.elapsedRealtime());
        }
        if ("interval".equals(key) || "current_ma".equals(key) || "reverse".equals(key) || "voltage_factor".equals(key)) {
            handler.removeCallbacks(tick);
            if (MonitorStore.running) handler.post(tick);
        }
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        overlay.reconfigure(configuration);
    }

    @Override public void onDestroy() {
        MonitorStore.running = false;
        handler.removeCallbacksAndMessages(null);
        releaseCpu();
        if (overlay != null) overlay.remove();
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(this);
        if (foreground) stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
