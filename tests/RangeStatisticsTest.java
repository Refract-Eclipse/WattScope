package dev.wattscope;

/** Window statistics must match chart filtering and leave session energy untouched. */
public final class RangeStatisticsTest {
    private static int checks;
    private static void yes(String label, boolean value) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void near(String label, double expected, double actual) {
        yes(label + ": expected " + expected + ", got " + actual,
                Double.isFinite(actual) && Math.abs(expected - actual) < 1e-9);
    }
    private static PowerSample sample(long ms, double watts) {
        return new PowerSample(ms, ms, 4, watts * 250, 25, 50, 3, 0, 0, "test", "");
    }
    private static ChartData.Series window(SampleHistory history, long range) {
        return ChartData.build(history, range, 120, 5000);
    }
    public static void main(String[] args) {
        SampleHistory history = new SampleHistory();
        SessionStats session = new SessionStats();
        session.reset(0);
        for (int i = 0; i <= 300; i++) {
            PowerSample value = sample(i * 1000, i == 100 ? 100 : -2);
            history.add(value); session.add(value, 1000);
        }
        double charge = session.chargeWh, discharge = session.dischargeWh;
        double fullAverage = session.averageW(), fullPeak = session.peakW;
        ChartData.Series minute = window(history, 60000);
        near("minute mean excludes old high power", 2, minute.averageW());
        near("minute peak excludes old high power", 2, minute.peakW());
        ChartData.Series five = window(history, 300000);
        near("five-minute mean matches full valid interval", fullAverage, five.averageW());
        near("five-minute peak restores older peak", 100, five.peakW());
        near("all mean includes retained records", fullAverage, window(history, 0).averageW());
        near("all peak includes retained records", 100, window(history, 0).peakW());
        near("custom range excludes old peak", 2, window(history, 120000).peakW());
        near("switching back restores minute mean", 2, window(history, 60000).averageW());
        near("charge total survives range changes", charge, session.chargeWh);
        near("discharge total survives range changes", discharge, session.dischargeWh);
        near("full session average is not mutated", fullAverage, session.averageW());
        near("full session peak is not mutated", fullPeak, session.peakW);
        yes("range changes preserve raw history", history.size() == 301);

        history.clear();
        history.add(sample(0, 2)); history.add(sample(1000, 10)); history.add(sample(5000, 10));
        near("irregular intervals use time weighting", 9.2, window(history, 60000).averageW());
        near("positive peak remains magnitude", 10, window(history, 60000).peakW());
        history.clear();
        history.add(sample(0, -2)); history.add(sample(1000, -10)); history.add(sample(5000, -10));
        near("discharge mean remains nonnegative", 9.2, window(history, 60000).averageW());
        near("discharge peak remains nonnegative", 10, window(history, 60000).peakW());
        history.clear(); history.add(sample(0, -2)); history.add(sample(4000, 2));
        near("zero crossing averages absolute power", 1, window(history, 60000).averageW());
        near("zero crossing peak uses magnitude", 2, window(history, 60000).peakW());

        history.clear();
        history.add(sample(0, 100)); history.add(sample(1000, 100));
        history.add(sample(100000, 2)); history.add(sample(101000, 2));
        near("sleep time excluded from mean denominator", 51, window(history, 120000).averageW());
        history.clear();
        history.add(sample(0, 100)); history.add(sample(1000, Double.NaN));
        history.add(sample(2000, 2)); history.add(sample(3000, 2));
        near("missing reading breaks average continuity", 2, window(history, 60000).averageW());
        near("isolated valid record still affects peak", 100, window(history, 60000).peakW());

        history.clear(); history.add(sample(0, 100)); history.add(sample(1000, 2)); history.add(sample(61000, 2));
        near("old peak outside exact cutoff excluded", 2, window(history, 60000).peakW());
        yes("no interpolation through large gap", Double.isNaN(window(history, 60000).averageW()));
        history.clear(); history.add(sample(0, 9)); history.add(sample(60000, 2));
        near("exact cutoff peak included", 9, window(history, 60000).peakW());
        history.add(sample(60001, 2));
        near("rolling cutoff expires old peak", 2, window(history, 60000).peakW());
        near("rolling cutoff expires old mean", 2, window(history, 60000).averageW());
        history.clear(); history.add(sample(0, 10)); history.add(sample(2000, 2)); history.add(sample(61000, 2));
        near("outside-window neighbor cannot affect mean", 2, ChartData.build(history, 60000, 120, 60000).averageW());
        near("outside-window neighbor cannot affect peak", 2, ChartData.build(history, 60000, 120, 60000).peakW());

        history.clear();
        yes("empty mean unavailable", Double.isNaN(window(history, 60000).averageW()));
        yes("empty peak unavailable", Double.isNaN(window(history, 60000).peakW()));
        history.add(sample(0, Double.NaN));
        yes("missing-only mean unavailable", Double.isNaN(window(history, 60000).averageW()));
        yes("missing-only peak unavailable", Double.isNaN(window(history, 60000).peakW()));
        history.clear(); history.add(sample(0, 0));
        yes("single sample has no average interval", Double.isNaN(window(history, 60000).averageW()));
        near("single zero sample has valid peak", 0, window(history, 60000).peakW());
        history.add(sample(1000, 0));
        near("measured zero interval has zero average", 0, window(history, 60000).averageW());
        near("measured zero interval has zero peak", 0, window(history, 60000).peakW());

        history.clear();
        for (int i = 0; i < 1000; i++) history.add(sample(i, i == 422 ? 73 : i == 423 ? -66 : 2));
        ChartData.Series narrow = ChartData.build(history, 60000, 1, 5000);
        ChartData.Series wide = ChartData.build(history, 60000, 1000, 5000);
        near("display reduction cannot change mean", wide.averageW(), narrow.averageW());
        near("display reduction cannot change peak", wide.peakW(), narrow.peakW());
        near("raw brief peak preserved in statistics", 73, narrow.peakW());
        System.out.println("PASS: " + checks + " selected-window statistics and unchanged session energy checks");
    }
}
