package dev.wattscope;

/** Accessed only on the process main thread by Activity and Service. No exported data channel. */
public final class MonitorStore {
    private MonitorStore() { }
    public static boolean running;
    public static PowerSample latest;
    public static String error = "";
    public static final SampleHistory history = new SampleHistory();
    public static final SampleHistory exportHistory = history;
    public static final SessionStats stats = new SessionStats();

    public static void reset(long now) {
        latest = null;
        history.clear();
        stats.reset(now);
        error = "";
    }

    public static void append(PowerSample sample, int interval) {
        latest = sample;
        history.add(sample);
        stats.add(sample, interval);
    }
}
