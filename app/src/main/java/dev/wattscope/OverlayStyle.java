package dev.wattscope;

public final class OverlayStyle {
    private OverlayStyle() { }
    public static final int MIN_PERCENT = 70, MAX_PERCENT = 200;
    public static int clamp(int percent) { return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent)); }
    public static float scale(int percent) { return clamp(percent) / 100f; }
    public static float widthDp(boolean compact, int percent) { return (compact ? 114 : 164) * scale(percent); }
    public static float textSp(boolean compact, int percent) { return (compact ? 18 : 23) * scale(percent); }
}
