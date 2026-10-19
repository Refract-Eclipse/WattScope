package dev.wattscope;

public final class DisplaySettingsTest {
    private static int checks;
    private static void yes(String name, boolean value) {
        checks++;
        if (!value) throw new AssertionError(name);
    }
    private static void near(String name, double expected, double value) {
        yes(name, Math.abs(expected - value) < 0.0001);
    }
    private static PowerSample sample(long ms, double watts) {
        return new PowerSample(ms, ms, 4, watts * 250, 25, 50, 3, 0, 0, "test", "");
    }
    private static boolean contains(ChartData.Series series, double value) {
        for (ChartData.Point point : series.points) if (Math.abs(point.sample.magnitude() - value) < 0.0001) return true;
        return false;
    }
    private static int segments(ChartData.Series series) {
        int count = 0;
        for (ChartData.Point point : series.points) if (point.breakBefore) count++;
        return count;
    }
    private static boolean signedContains(ChartData.Series series, double value) {
        for (ChartData.Point point : series.points) if (Math.abs(point.sample.signedWatts - value) < 0.0001) return true;
        return false;
    }
    public static void main(String[] args) {
        SampleHistory history = new SampleHistory();
        for (int i = 0; i <= 300; i++) history.add(sample(i * 1000, i == 100 ? 100 : -2));
        ChartData.Series minute = ChartData.build(history, 60000, 120, 5000);
        yes("one minute cutoff is exact", minute.startMs == 240000 && minute.endMs == 300000);
        yes("both cutoff boundary samples included", minute.validCount == 61);
        yes("expired peak does not affect visible scale", minute.maxW == 2);
        yes("time range does not delete stored history", history.size() == 301);
        yes("five minutes restores older samples", ChartData.build(history, 300000, 120, 5000).validCount == 301);
        yes("custom duration applied", ChartData.build(history, 120000, 120, 5000).validCount == 121);
        yes("all spans recorded session", ChartData.build(history, 0, 120, 5000).durationMs == 300000);
        yes("all never produces a zero-width axis", ChartData.build(new SampleHistory(), 0, 120, 5000).durationMs == 60000);
        history.clear();
        for (int i = 0; i < 1000; i++) history.add(sample(i, i == 422 ? 73 : i == 423 ? 0.1 : 2));
        ChartData.Series reduced = ChartData.build(history, 60000, 80, 5000);
        yes("render reduction preserves narrow peaks", contains(reduced, 73));
        yes("render reduction preserves minima", contains(reduced, 0.1));
        yes("first sample preserved", reduced.points.get(0).sample.elapsedMs == 0);
        yes("last sample preserved", reduced.points.get(reduced.points.size() - 1).sample.elapsedMs == 999);
        yes("display point count bounded", reduced.points.size() <= 4 * 80);
        yes("display reduction retains export resolution", history.size() == 1000);
        history.clear();
        history.add(sample(0, 2)); history.add(sample(1000, 2)); history.add(sample(100000, 2)); history.add(sample(101000, 2));
        yes("sleep gap is not connected", segments(ChartData.build(history, 120000, 120, 5000)) == 2);
        history.clear();
        history.add(sample(0, 2)); history.add(sample(1000, Double.NaN)); history.add(sample(2000, 2));
        ChartData.Series missing = ChartData.build(history, 60000, 120, 5000);
        yes("missing readings break the plotted line", segments(missing) == 2);
        yes("missing readings excluded from scale and count", missing.validCount == 2 && missing.maxW == 2);
        history.clear();
        history.add(sample(0, 2)); history.add(sample(SampleHistory.RETENTION_MS, 2));
        yes("retention boundary included", history.size() == 2);
        history.add(sample(SampleHistory.RETENTION_MS + 1, 2));
        yes("expired records removed", history.size() == 2 && history.first().elapsedMs == SampleHistory.RETENTION_MS);
        history.clear();
        for (int i = 0; i < SampleHistory.MAX_SAMPLES + 4; i++) history.add(sample(i, 2));
        yes("memory record cap enforced", history.size() == SampleHistory.MAX_SAMPLES);
        yes("record cap retains newest data", history.first().elapsedMs == 4);
        near("invalid short range clamped", 60000, ChartData.clampRange(-1));
        near("oversize range clamped", SampleHistory.RETENTION_MS, ChartData.clampRange(Long.MAX_VALUE));
        near("all-range preference retained", 0, ChartData.clampRange(0));
        near("minimum overlay scale", 70, OverlayStyle.clamp(0));
        near("maximum overlay scale", 200, OverlayStyle.clamp(999));
        near("standard default width", 164, OverlayStyle.widthDp(false, 100));
        near("compact default width", 114, OverlayStyle.widthDp(true, 100));
        near("large width follows scale", 328, OverlayStyle.widthDp(false, 200));
        near("small text follows scale", 16.1, OverlayStyle.textSp(false, 70));
        near("compact large text follows scale", 36, OverlayStyle.textSp(true, 200));
        history.clear();
        for (int i = 0; i < 1000; i++) history.add(sample(i, i == 410 ? 12 : i == 411 ? -11 : i == 412 ? 0.1 : 2));
        ChartData.Series bipolar = ChartData.build(history, 60000, 1, 5000);
        yes("negative discharge extremum survives reduction", signedContains(bipolar, -11));
        yes("positive charge extremum survives reduction", signedContains(bipolar, 12));
        yes("symmetric plot scale uses absolute peak", bipolar.maxW == 12);
        long previousTime = Long.MIN_VALUE;
        boolean ordered = true;
        for (ChartData.Point point : bipolar.points) {
            if (point.sample.elapsedMs < previousTime) ordered = false;
            previousTime = point.sample.elapsedMs;
        }
        yes("signed extrema remain chronological", ordered);
        yes("display processing does not change raw polarity", history.size() == 1000 && signedContains(bipolar, -11));
        history.clear(); history.add(sample(0, 0));
        yes("zero power remains a valid neutral sample", signedContains(ChartData.build(history, 60000, 120, 5000), 0));
        history.clear(); history.add(sample(0, 2)); history.add(sample(6000, -3));
        yes("opposite signs across a sleep gap are not joined", segments(ChartData.build(history, 60000, 120, 5000)) == 2);
        history.clear(); history.add(sample(0, 2)); history.add(sample(1000, Double.NaN)); history.add(sample(2000, -3));
        yes("opposite signs across missing data are not joined", segments(ChartData.build(history, 60000, 120, 5000)) == 2);
        history.clear(); history.add(sample(0, 2)); history.add(sample(1000, 0)); history.add(sample(2000, -3));
        ChartData.Series crossing = ChartData.build(history, 60000, 120, 5000);
        yes("measured zero at a polarity crossing is retained", crossing.validCount == 3 && signedContains(crossing, 0) && segments(crossing) == 1);
        history.clear();
        history.add(sample(0, -2)); history.add(sample(60000, 2)); history.add(sample(300000, 0)); history.add(sample(360000, 4));
        ChartData.Series chargeOnly = ChartData.build(history, 300000, 120, 5000);
        yes("five-minute range excludes older discharge", chargeOnly.mode() == ChartData.Mode.CHARGING);
        yes("charge-only range hides discharge presence", chargeOnly.hasCharging && !chargeOnly.hasDischarging);
        near("charge-only peak uses top of full plot", 0, chargeOnly.verticalFraction(4, 4));
        near("charge-only zero is bottom baseline", 1, chargeOnly.verticalFraction(0, 4));
        near("charge-only half power uses plot midpoint", 0.5, chargeOnly.verticalFraction(2, 4));
        ChartData.Series wide = ChartData.build(history, 3600000, 120, 5000);
        yes("wider range restores mixed mode", wide.mode() == ChartData.Mode.MIXED);
        near("mixed charge is above zero", 0, wide.verticalFraction(4, 4));
        near("mixed zero is central", 0.5, wide.verticalFraction(0, 4));
        near("mixed discharge is below zero", 1, wide.verticalFraction(-4, 4));
        yes("returning to narrow range restores charge-only mode", ChartData.build(history, 300000, 120, 5000).mode() == ChartData.Mode.CHARGING);
        history.clear();
        history.add(sample(0, 3)); history.add(sample(60000, -2)); history.add(sample(300000, 0)); history.add(sample(360000, -4));
        ChartData.Series dischargeOnly = ChartData.build(history, 300000, 120, 5000);
        yes("discharge-only range ignores charge outside window", dischargeOnly.mode() == ChartData.Mode.DISCHARGING);
        yes("discharge-only range hides charge presence", !dischargeOnly.hasCharging && dischargeOnly.hasDischarging);
        near("discharge-only uses positive magnitude and full height", 0, dischargeOnly.verticalFraction(-4, 4));
        near("discharge-only zero stays at plot bottom", 1, dischargeOnly.verticalFraction(0, 4));
        near("discharge-only half magnitude uses plot midpoint", 0.5, dischargeOnly.verticalFraction(-2, 4));
        history.clear(); history.add(sample(0, 0)); history.add(sample(1000, Double.NaN));
        ChartData.Series zeroOnly = ChartData.build(history, 60000, 120, 5000);
        yes("zero and missing samples do not invent a direction", zeroOnly.mode() == ChartData.Mode.ZERO && !zeroOnly.hasCharging && !zeroOnly.hasDischarging);
        near("zero-only baseline stays at bottom", 1, zeroOnly.verticalFraction(0, 4));
        history.clear();
        yes("empty range has no charge/discharge mode", ChartData.build(history, 60000, 120, 5000).mode() == ChartData.Mode.EMPTY);
        history.add(sample(0, Double.NaN));
        yes("all missing remains empty rather than zero", ChartData.build(history, 60000, 120, 5000).mode() == ChartData.Mode.EMPTY);
        history.clear(); history.add(sample(0, 2));
        yes("direction follows net current rather than battery status", ChartData.build(history, 60000, 120, 5000).mode() == ChartData.Mode.CHARGING);
        history.add(sample(1, -3));
        yes("both raw polarities produce mixed mode before reduction", ChartData.build(history, 60000, 1, 5000).mode() == ChartData.Mode.MIXED);
        history.clear(); history.add(sample(0, -2)); history.add(sample(1, 2)); history.add(sample(60000, 2));
        yes("opposite direction on exact window boundary still visible", ChartData.build(history, 60000, 120, 5000).mode() == ChartData.Mode.MIXED);
        history.add(sample(60001, 2));
        yes("rolling expiry automatically restores single-direction mode", ChartData.build(history, 60000, 120, 5000).mode() == ChartData.Mode.CHARGING);
        System.out.println("PASS: " + checks + " history, automatic chart modes, polarity and overlay size checks");
    }
}
