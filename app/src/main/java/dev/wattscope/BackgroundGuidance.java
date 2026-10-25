package dev.wattscope;

import java.util.Locale;

/** A suggestion only: brand is not proof of the OS or its current background policy. */
public enum BackgroundGuidance {
    VIVO(R.string.bg_vivo, R.string.bg_vivo_steps),
    XIAOMI(R.string.bg_xiaomi, R.string.bg_xiaomi_steps),
    OPPO(R.string.bg_oppo, R.string.bg_oppo_steps),
    ONEPLUS(R.string.bg_oneplus, R.string.bg_oneplus_steps),
    REALME(R.string.bg_realme, R.string.bg_realme_steps),
    HUAWEI(R.string.bg_huawei, R.string.bg_huawei_steps),
    HONOR(R.string.bg_honor, R.string.bg_honor_steps),
    SAMSUNG(R.string.bg_samsung, R.string.bg_samsung_steps),
    PIXEL(R.string.bg_pixel, R.string.bg_pixel_steps),
    OTHER(R.string.bg_other, R.string.bg_other_steps);

    public final int label, steps;
    BackgroundGuidance(int label, int steps) { this.label = label; this.steps = steps; }

    public static BackgroundGuidance detect(String manufacturer, String brand) {
        BackgroundGuidance specific = match(brand);
        return specific == OTHER ? match(manufacturer) : specific;
    }

    public static BackgroundGuidance override(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return valueOf(value); }
        catch (IllegalArgumentException unknown) { return null; }
    }

    private static BackgroundGuidance match(String value) {
        if (value == null) return OTHER;
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "vivo": case "iqoo": return VIVO;
            case "xiaomi": case "redmi": case "poco": return XIAOMI;
            case "oppo": return OPPO;
            case "oneplus": case "一加": return ONEPLUS;
            case "realme": case "真我": return REALME;
            case "huawei": case "华为": return HUAWEI;
            case "honor": case "荣耀": return HONOR;
            case "samsung": case "三星": return SAMSUNG;
            case "google": case "pixel": return PIXEL;
            default: return OTHER;
        }
    }
}
