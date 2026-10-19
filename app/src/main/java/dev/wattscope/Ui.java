package dev.wattscope;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

final class Ui {
    static final int BG = Color.rgb(9, 15, 26);
    static final int CARD = Color.rgb(18, 28, 45);
    static final int TEXT = Color.rgb(238, 244, 255);
    static final int MUTED = Color.rgb(141, 158, 182);
    static final int GREEN = Color.rgb(132, 174, 255);
    static final int CYAN = Color.rgb(87, 219, 191);
    static final int LINE = Color.rgb(37, 52, 75);
    static final int ORANGE = Color.rgb(255, 193, 115);

    static int dp(Context c, float n) { return Math.round(n * c.getResources().getDisplayMetrics().density); }

    static GradientDrawable rounded(Context c, int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(c, radius));
        return shape;
    }

    static GradientDrawable outlined(Context c, int color, int stroke, float radius) {
        GradientDrawable shape = rounded(c, color, radius);
        shape.setStroke(dp(c, 1), stroke);
        return shape;
    }

    static android.graphics.drawable.Drawable surface(Context c, int color, float radius) {
        return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x2484AEFF),
                rounded(c, color, radius), rounded(c, Color.WHITE, radius));
    }

    static TextView text(Context c, String text, float size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setFontFeatureSettings("tnum");
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setIncludeFontPadding(false);
        return v;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static void space(LinearLayout parent, int heightDp) {
        parent.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(1, dp(parent.getContext(), heightDp)));
    }

    static String number(double n, int decimals) {
        return Double.isFinite(n) ? String.format(Locale.US, "%." + decimals + "f", n) : "—";
    }

    static String power(PowerSample s) { return s != null && s.valid() ? number(s.magnitude(), 2) + " W" : "— W"; }
}
