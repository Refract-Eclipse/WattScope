package dev.wattscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Decorative background only; no fabricated measurement is represented by the arcs. */
final class MeterBackdrop extends Drawable {
    private final Context context;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    MeterBackdrop(Context context) { this.context = context; }
    @Override public void draw(Canvas canvas) {
        RectF bounds = new RectF(getBounds());
        float corner = Ui.dp(context, 25);
        Path clip = new Path(); clip.addRoundRect(bounds, corner, corner, Path.Direction.CW);
        canvas.save(); canvas.clipPath(clip);
        paint.setColor(0xFF172B4C); paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(bounds, paint);
        float cx = bounds.right - Ui.dp(context, 28), cy = bounds.top + bounds.height() * 0.36f;
        paint.setShader(new RadialGradient(cx, cy, Math.max(1, bounds.width() * 0.8f),
                new int[]{0x802F66D5, 0x00192B49}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Ui.dp(context, 1));
        paint.setColor(0x255E8AC8);
        for (int i = 0; i < 3; i++) {
            float r = Ui.dp(context, 65 + i * 33);
            canvas.drawCircle(cx + Ui.dp(context, 55), cy, r, paint);
        }
        paint.setColor(0xFF314D75);
        canvas.drawRoundRect(new RectF(bounds.left + 0.5f, bounds.top + 0.5f, bounds.right - 0.5f, bounds.bottom - 0.5f), corner, corner, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
