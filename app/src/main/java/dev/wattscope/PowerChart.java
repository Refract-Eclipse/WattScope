package dev.wattscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class PowerChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ChartData.Series series;
    private PowerSample cachedSample, selected;
    private long cachedRange = -1;
    private int cachedWidth, cachedGap;
    private float plotLeft, plotRight, plotTop, plotBottom;
    private double ceiling;
    private boolean touching;

    public PowerChart(Context context) {
        super(context);
        setContentDescription("电池功率曲线会根据所选范围自动选择单向幅值或双向正负显示。");
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClickable(true);
    }

    public void refreshData() {
        if (!touching) selected = null;
        cachedRange = -1;
        invalidate();
    }

    public ChartData.Series currentSeries() {
        ensureSeries();
        return series;
    }

    private void ensureSeries() {
        long range = Prefs.chartRange(getContext());
        int gap = Math.max(5000, Prefs.interval(getContext()) * 3);
        if (series == null || cachedSample != MonitorStore.latest || cachedRange != range || cachedWidth != getWidth() || gap != cachedGap) {
            int pixels = Math.max(48, (int) ((getWidth() - Ui.dp(getContext(), 50)) / getResources().getDisplayMetrics().density));
            series = ChartData.build(MonitorStore.history, range, pixels, gap);
            cachedSample = MonitorStore.latest; cachedRange = range; cachedWidth = getWidth(); cachedGap = gap;
            if (!touching) {
                String description = series.mode() == ChartData.Mode.MIXED
                        ? "所选范围包含充电和放电，零线上方为正值净充电，下方为负值净放电。"
                        : series.mode() == ChartData.Mode.CHARGING ? "所选范围只有净充电，显示充电功率幅值。"
                        : series.mode() == ChartData.Mode.DISCHARGING ? "所选范围只有净放电，显示放电功率幅值。"
                        : "所选范围暂无充放电方向，显示零值或等待有效数据。";
                setContentDescription(description + "按住可查看方向、时间和功率。");
            }
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        plotLeft = Ui.dp(getContext(), 43); plotRight = getWidth() - Ui.dp(getContext(), 7);
        plotTop = Ui.dp(getContext(), 15); plotBottom = getHeight() - Ui.dp(getContext(), 27);
        if (plotRight <= plotLeft || plotBottom <= plotTop) return;
        ensureSeries();
        boolean mixed = series.mode() == ChartData.Mode.MIXED;
        plotLeft = Ui.dp(getContext(), mixed ? 43 : 31);
        double maximum = Math.max(2, series.maxW * 1.15);
        double unit = Math.pow(10, Math.floor(Math.log10(maximum)));
        double scaled = maximum / unit;
        ceiling = (scaled <= 2 ? 2 : scaled <= 5 ? 5 : 10) * unit;
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", 0));
        paint.setTextSize(Ui.dp(getContext(), 10));
        paint.setStrokeWidth(Ui.dp(getContext(), 1));
        paint.setStyle(Paint.Style.FILL);
        int divisions = mixed ? 4 : 2;
        for (int i = 0; i <= divisions; i++) {
            double tick = ceiling * (2 - i) / 2;
            float y = plotTop + (plotBottom - plotTop) * i / divisions;
            paint.setColor(i == 2 ? 0xFF60738E : Ui.LINE);
            paint.setStrokeWidth(Ui.dp(getContext(), i == 2 ? 1.3f : 1));
            if (i == 2) paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{Ui.dp(getContext(), 4), Ui.dp(getContext(), 4)}, 0));
            canvas.drawLine(plotLeft, y, plotRight, y, paint);
            paint.setPathEffect(null);
            paint.setColor(i == 2 ? Ui.TEXT : Ui.MUTED);
            String label = i == 2 ? "0 W" : (mixed && tick > 0 ? "+" : "") + Ui.number(tick, Math.rint(tick) == tick ? 0 : 1);
            canvas.drawText(label, 0, y + Ui.dp(getContext(), 3), paint);
        }
        long endWall = MonitorStore.latest == null ? System.currentTimeMillis() : MonitorStore.latest.wallTimeMs;
        SimpleDateFormat times = new SimpleDateFormat(series.durationMs <= 300000 ? "HH:mm:ss" : "HH:mm", Locale.getDefault());
        paint.setColor(Ui.MUTED);
        canvas.drawText(times.format(new Date(endWall - series.durationMs)), plotLeft, getHeight() - Ui.dp(getContext(), 4), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(times.format(new Date(endWall - series.durationMs / 2)), (plotLeft + plotRight) / 2, getHeight() - Ui.dp(getContext(), 4), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(MonitorStore.running ? "现在" : "末次", plotRight, getHeight() - Ui.dp(getContext(), 4), paint);
        paint.setTextAlign(Paint.Align.LEFT);
        if (series.points.isEmpty()) {
            paint.setTextSize(Ui.dp(getContext(), 12)); paint.setColor(Ui.MUTED); paint.setTextAlign(Paint.Align.CENTER);
            String label = MonitorStore.history.isEmpty() ? "开始监测，绘制你的功率曲线" : "这个范围内没有可用的功率读数";
            canvas.drawText(label, (plotLeft + plotRight) / 2, (plotTop + plotBottom) / 2, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            return;
        }
        float zeroY = y(0);
        Path line = new Path(), fill = new Path();
        boolean hasSegment = false;
        float lastX = 0;
        PowerSample last = null;
        for (ChartData.Point point : series.points) {
            float px = x(point.sample.elapsedMs), py = y(point.sample.signedWatts);
            if (!hasSegment || point.breakBefore) {
                if (hasSegment) { fill.lineTo(lastX, zeroY); fill.close(); }
                line.moveTo(px, py);
                fill.moveTo(px, zeroY); fill.lineTo(px, py);
                hasSegment = true;
            } else { line.lineTo(px, py); fill.lineTo(px, py); }
            lastX = px; last = point.sample;
        }
        if (hasSegment) { fill.lineTo(lastX, zeroY); fill.close(); }
        // Drawing each half of the same signed path places the color transition
        // at the actual zero crossing, without connecting across missing samples.
        if (mixed) {
            drawSignedHalf(canvas, line, fill, plotTop, zeroY, Ui.CYAN, true);
            drawSignedHalf(canvas, line, fill, zeroY, plotBottom, Ui.ORANGE, false);
        } else {
            int tone = series.mode() == ChartData.Mode.CHARGING ? Ui.CYAN : series.mode() == ChartData.Mode.DISCHARGING ? Ui.ORANGE : Ui.MUTED;
            drawSignedHalf(canvas, line, fill, plotTop, plotBottom, tone, true);
        }
        canvas.save(); canvas.clipRect(plotLeft, plotTop, plotRight, plotBottom + 1);
        paint.setStyle(Paint.Style.FILL);
        PowerSample previous = null;
        for (ChartData.Point point : series.points) {
            paint.setColor(color(point.sample.signedWatts));
            if (point.breakBefore) canvas.drawCircle(x(point.sample.elapsedMs), y(point.sample.signedWatts), Ui.dp(getContext(), 2), paint);
            if (!point.breakBefore && previous != null && previous.signedWatts == 0 && point.sample.signedWatts == 0) {
                paint.setColor(Ui.MUTED); paint.setStrokeWidth(Ui.dp(getContext(), 2.3f));
                canvas.drawLine(x(previous.elapsedMs), zeroY, x(point.sample.elapsedMs), zeroY, paint);
            }
            previous = point.sample;
        }
        if (last != null) {
            int color = color(last.signedWatts);
            paint.setColor((color & 0x00FFFFFF) | 0x35000000);
            canvas.drawCircle(lastX, y(last.signedWatts), Ui.dp(getContext(), 7), paint);
            paint.setColor(color);
            canvas.drawCircle(lastX, y(last.signedWatts), Ui.dp(getContext(), 3), paint);
        }
        canvas.restore();
        if (selected != null && selected.elapsedMs >= series.startMs && selected.elapsedMs <= series.endMs) drawSelection(canvas, selected);
    }

    private void drawSignedHalf(Canvas canvas, Path line, Path fill, float top, float bottom, int color, boolean charging) {
        canvas.save(); canvas.clipRect(plotLeft, top, plotRight, bottom);
        paint.setStyle(Paint.Style.FILL);
        int strong = (color & 0x00FFFFFF) | 0x50000000;
        int faint = (color & 0x00FFFFFF) | 0x04000000;
        paint.setShader(new LinearGradient(0, top, 0, bottom, charging ? strong : faint, charging ? faint : strong, Shader.TileMode.CLAMP));
        canvas.drawPath(fill, paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Ui.dp(getContext(), 2.3f));
        paint.setStrokeJoin(Paint.Join.ROUND); paint.setStrokeCap(Paint.Cap.ROUND); paint.setColor(color);
        canvas.drawPath(line, paint); paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    private static int color(double signedWatts) {
        return signedWatts > 0 ? Ui.CYAN : signedWatts < 0 ? Ui.ORANGE : Ui.MUTED;
    }

    private String displayPower(PowerSample sample) {
        if (series.mode() != ChartData.Mode.MIXED) return Ui.number(sample.magnitude(), 2) + " W";
        return (sample.signedWatts > 0 ? "+" : "") + Ui.number(sample.signedWatts, 2) + " W";
    }

    private float x(long elapsedMs) { return plotLeft + (plotRight - plotLeft) * (float) ((elapsedMs - series.startMs) / (double) series.durationMs); }
    private float y(double watts) { return plotTop + (plotBottom - plotTop) * (float) series.verticalFraction(watts, ceiling); }

    private void drawSelection(Canvas canvas, PowerSample sample) {
        float cx = x(sample.elapsedMs), cy = y(sample.signedWatts);
        int tone = color(sample.signedWatts);
        paint.setColor((tone & 0x00FFFFFF) | 0x66000000); paint.setStrokeWidth(Ui.dp(getContext(), 1));
        canvas.drawLine(cx, plotTop, cx, plotBottom, paint);
        paint.setColor(Ui.TEXT); canvas.drawCircle(cx, cy, Ui.dp(getContext(), 4), paint);
        String direction = sample.signedWatts > 0 ? "净充电" : sample.signedWatts < 0 ? "净放电" : "零净功率";
        String label = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(sample.wallTimeMs)) + " · " + direction;
        String value = displayPower(sample);
        paint.setTextSize(Ui.dp(getContext(), 10));
        float labelWidth = paint.measureText(label);
        paint.setTextSize(Ui.dp(getContext(), 13));
        float padding = Ui.dp(getContext(), 10), width = Math.max(labelWidth, paint.measureText(value)) + padding * 2;
        float left = Math.max(0, Math.min(cx - width / 2, getWidth() - width));
        float height = Ui.dp(getContext(), 46);
        float top = cy - height - Ui.dp(getContext(), 8);
        if (top < 0) top = cy + Ui.dp(getContext(), 8);
        top = Math.max(0, Math.min(top, plotBottom - height));
        RectF bubble = new RectF(left, top, left + width, top + height);
        paint.setColor(0xFF213650); canvas.drawRoundRect(bubble, Ui.dp(getContext(), 9), Ui.dp(getContext(), 9), paint);
        paint.setColor(Ui.TEXT); paint.setTextSize(Ui.dp(getContext(), 10));
        canvas.drawText(label, left + padding, top + Ui.dp(getContext(), 16), paint);
        paint.setColor(tone); paint.setTextSize(Ui.dp(getContext(), 13));
        canvas.drawText(value, left + padding, top + Ui.dp(getContext(), 35), paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (series == null || series.points.isEmpty()) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            touching = true; getParent().requestDisallowInterceptTouchEvent(true);
            float fraction = Math.max(0, Math.min(1, (event.getX() - plotLeft) / (plotRight - plotLeft)));
            long target = series.startMs + (long) (fraction * series.durationMs);
            long nearest = Long.MAX_VALUE;
            for (PowerSample sample : MonitorStore.history) {
                if (!sample.valid() || sample.elapsedMs < series.startMs) continue;
                long distance = Math.abs(sample.elapsedMs - target);
                if (distance < nearest) { selected = sample; nearest = distance; }
            }
            if (selected != null) setContentDescription("选中" + selected.direction() + " " + displayPower(selected) + "，" + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(selected.wallTimeMs)));
            invalidate(); return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            touching = false; getParent().requestDisallowInterceptTouchEvent(false);
            if (event.getActionMasked() == MotionEvent.ACTION_UP) performClick();
            return true;
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
