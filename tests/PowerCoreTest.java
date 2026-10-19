package dev.wattscope;

/** Run with the JDK only; no emulator or testing framework is required. */
public final class PowerCoreTest {
    private static int checks;
    private static void near(String label, double expected, double actual) {
        checks++;
        if (!Double.isFinite(actual) || Math.abs(expected - actual) > 1e-9) {
            throw new AssertionError(label + ": expected " + expected + ", actual " + actual);
        }
    }
    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static PowerSample sample(long ms, double watts) {
        return new PowerSample(ms, ms, 4, watts * 250, 25, 50, 3, 0, 0, "test", "");
    }
    public static void main(String[] args) {
        near("microamps to mA", -1000, PowerMath.currentMa(-1000000, false, false));
        near("OEM mA input", 1250, PowerMath.currentMa(1250, true, false));
        near("manual polarity correction", 900, PowerMath.currentMa(-900000, false, true));
        yes("unsupported current remains unavailable", Double.isNaN(PowerMath.currentMa(Integer.MIN_VALUE, false, false)));
        near("zero is a valid measurement", 0, PowerMath.watts(4, 0));
        near("4V discharge at 1A", -4, PowerMath.watts(4, -1000));
        near("4V charge at 2A", 8, PowerMath.watts(4, 2000));
        yes("missing voltage is unavailable", Double.isNaN(PowerMath.watts(0, -1000)));
        yes("missing current is unavailable", Double.isNaN(PowerMath.watts(4, Double.NaN)));
        yes("invalid current range", Double.isNaN(PowerMath.watts(4, 100001)));
        near("constant energy", 4.0 / 60, PowerMath.positiveWh(4, 4, 60));
        near("negative interval excludes charge", 0, PowerMath.positiveWh(-4, -4, 60));
        near("charge triangle across zero", 2.0 / 3600, PowerMath.positiveWh(-2, 2, 4));
        near("reverse crossing triangle", 2.0 / 3600, PowerMath.positiveWh(2, -2, 4));
        near("missing values contribute no energy", 0, PowerMath.positiveWh(Double.NaN, 1, 2));
        near("negative time contributes no energy", 0, PowerMath.positiveWh(2, 2, -1));
        SessionStats stats = new SessionStats();
        stats.reset(0);
        stats.add(sample(0, -4), 1000);
        stats.add(sample(1000, -4), 1000);
        near("sampled discharge energy", 4.0 / 3600, stats.dischargeWh);
        near("time weighted mean", 4, stats.averageW());
        stats.add(sample(61000, -4), 1000);
        near("sleep gaps excluded", 1, stats.observedSeconds);
        near("gap adds no energy", 4.0 / 3600, stats.dischargeWh);
        stats.add(sample(62000, Double.NaN), 1000);
        stats.add(sample(63000, -4), 1000);
        near("missing sample breaks energy continuity", 1, stats.observedSeconds);
        stats.reset(0);
        stats.add(sample(0, -2), 2000);
        stats.add(sample(4000, 2), 2000);
        near("separate discharge across polarity change", 2.0 / 3600, stats.dischargeWh);
        near("separate charge across polarity change", 2.0 / 3600, stats.chargeWh);
        near("absolute mean across zero crossing", 1, stats.averageW());
        near("peak magnitude", 2, stats.peakW);
        yes("positive watts labeled charging", sample(0, 1).direction().equals("电池净充入"));
        yes("negative watts labeled discharging", sample(0, -1).direction().equals("电池净放出"));
        System.out.println("PASS: " + checks + " power conversion and session energy checks");
    }
}
