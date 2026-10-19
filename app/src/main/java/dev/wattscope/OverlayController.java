package dev.wattscope;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

final class OverlayController {
    private final Service service;
    private final WindowManager manager;
    private LinearLayout root;
    private TextView watts, detail;
    private WindowManager.LayoutParams params;
    private boolean compact;

    OverlayController(Service service) {
        this.service = service;
        manager = (WindowManager) service.getSystemService(Service.WINDOW_SERVICE);
    }

    void sync() {
        if (!Prefs.overlay(service) || !Settings.canDrawOverlays(service)) { remove(); return; }
        if (root != null) { update(MonitorStore.latest); return; }
        compact = Prefs.get(service).getBoolean("compact", false);
        root = Ui.column(service);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackground(Ui.outlined(service, 0xF2121F35, 0xFF3D587B, 17));
        root.setElevation(Ui.dp(service, 8));
        root.setContentDescription("功率悬浮窗，拖动可移动，轻点打开应用，长按隐藏");
        watts = Ui.text(service, "— W", compact ? 18 : 23, Ui.GREEN, true);
        watts.setSingleLine(true);
        watts.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        detail = Ui.text(service, "等待读数", 10, Ui.MUTED, false);
        detail.setSingleLine(true);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(watts);
        if (!compact) {
            Ui.space(root, 4);
            root.addView(detail);
        }
        int width = Ui.dp(service, OverlayStyle.widthDp(compact, Prefs.overlayScale(service)));
        params = new WindowManager.LayoutParams(width, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.alpha = 0.94f;
        params.x = Prefs.get(service).getInt("overlay_x", Ui.dp(service, 12));
        params.y = Prefs.get(service).getInt("overlay_y", Ui.dp(service, 170));
        applySizing(false);
        clamp();
        installGestures();
        try { manager.addView(root, params); update(MonitorStore.latest); }
        catch (RuntimeException denied) {
            root = null;
            MonitorStore.error = "悬浮窗未能显示，请检查系统悬浮窗权限。";
        }
    }

    private void installGestures() {
        root.setOnClickListener(v -> {
            Intent open = new Intent(service, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try { service.startActivity(open); }
            catch (RuntimeException blocked) { MonitorStore.error = "请从通知或桌面打开瓦特计。"; }
        });
        root.setOnLongClickListener(v -> {
            Prefs.get(service).edit().putBoolean("overlay", false).apply();
            remove();
            return true;
        });
        root.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int originX, originY;
            boolean dragged;
            final int slop = ViewConfiguration.get(service).getScaledTouchSlop();
            @Override public boolean onTouch(View view, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    downX = e.getRawX(); downY = e.getRawY();
                    originX = params.x; originY = params.y; dragged = false;
                    return false; // Preserve native click, long press and accessibility actions.
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                    if (Math.hypot(dx, dy) > slop) dragged = true;
                    if (dragged) {
                        view.cancelLongPress();
                        params.x = originX + Math.round(dx); params.y = originY + Math.round(dy);
                        clamp();
                        try { manager.updateViewLayout(root, params); }
                        catch (RuntimeException removed) { remove(); }
                        return true;
                    }
                }
                if (e.getActionMasked() == MotionEvent.ACTION_UP && dragged) {
                    Prefs.get(service).edit().putInt("overlay_x", params.x).putInt("overlay_y", params.y).apply();
                    view.setPressed(false);
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) view.setPressed(false);
                return false;
            }
        });
    }

    private void clamp() {
        android.util.DisplayMetrics metrics = service.getResources().getDisplayMetrics();
        params.x = Math.max(0, Math.min(params.x, Math.max(0, metrics.widthPixels - params.width)));
        int height = root != null && root.getMeasuredHeight() > 0 ? root.getMeasuredHeight()
                : Ui.dp(service, (compact ? 48 : 72) * OverlayStyle.scale(Prefs.overlayScale(service)));
        params.y = Math.max(Ui.dp(service, 28), Math.min(params.y, Math.max(Ui.dp(service, 28), metrics.heightPixels - height - Ui.dp(service, 28))));
    }

    void applySizing(boolean attached) {
        if (root == null || params == null) return;
        int percent = Prefs.overlayScale(service);
        float scale = OverlayStyle.scale(percent);
        int padding = Ui.dp(service, 13 * scale), vertical = Ui.dp(service, 9 * scale);
        root.setPadding(padding, vertical, padding, vertical);
        watts.setTextSize(OverlayStyle.textSp(compact, percent));
        detail.setTextSize(10 * scale);
        int availableWidth = Math.max(Ui.dp(service, 80), service.getResources().getDisplayMetrics().widthPixels - Ui.dp(service, 12));
        params.width = Math.min(availableWidth, Ui.dp(service, OverlayStyle.widthDp(compact, percent)));
        root.measure(View.MeasureSpec.makeMeasureSpec(params.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        clamp();
        if (attached) {
            try { manager.updateViewLayout(root, params); }
            catch (RuntimeException removed) { remove(); }
        }
    }

    void update(PowerSample sample) {
        if (root == null) return;
        watts.setText(Ui.power(sample));
        watts.setTextColor(sample != null && sample.signedWatts > 0 ? Ui.CYAN : Ui.GREEN);
        detail.setText(sample == null ? "等待读数" : !sample.valid() ? "传感器读数不可用"
                : sample.direction() + " · " + Ui.number(Math.abs(sample.currentMa), 0) + " mA");
    }

    void reconfigure(Configuration configuration) {
        remove();
        sync();
    }

    void remove() {
        if (root != null) {
            try { manager.removeView(root); } catch (RuntimeException alreadyRemoved) { /* detached */ }
            root = null;
        }
    }
}
