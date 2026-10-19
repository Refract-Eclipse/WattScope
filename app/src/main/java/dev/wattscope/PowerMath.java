package dev.wattscope;

/** Platform independent unit conversion and signed trapezoidal energy integration. */
public final class PowerMath {
    private PowerMath() { }

    public static double currentMa(int raw, boolean rawIsMa, boolean reverse) {
        if (raw == Integer.MIN_VALUE) return Double.NaN;
        double current = raw / (rawIsMa ? 1.0 : 1000.0);
        return reverse ? -current : current;
    }

    /** Signed watts: positive = net charge entering the battery. */
    public static double watts(double voltageV, double currentMa) {
        if (!Double.isFinite(voltageV) || !Double.isFinite(currentMa)
                || voltageV <= 0 || voltageV > 30 || Math.abs(currentMa) > 100000) {
            return Double.NaN;
        }
        return voltageV * currentMa / 1000.0;
    }

    /** Integrates only the positive half of a linearly varying power interval. */
    public static double positiveWh(double startW, double endW, double seconds) {
        if (!Double.isFinite(startW) || !Double.isFinite(endW)
                || !Double.isFinite(seconds) || seconds <= 0) return 0;
        if (startW <= 0 && endW <= 0) return 0;
        if (startW >= 0 && endW >= 0) return (startW + endW) * 0.5 * seconds / 3600.0;
        double positive = Math.max(startW, endW);
        double fraction = positive / (Math.abs(startW) + Math.abs(endW));
        return positive * 0.5 * seconds * fraction / 3600.0;
    }
}
