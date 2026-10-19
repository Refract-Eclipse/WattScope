package dev.wattscope;

import java.util.ArrayDeque;
import java.util.Iterator;

/** Bounded session history; removing expired samples has constant cost. */
public final class SampleHistory implements Iterable<PowerSample> {
    public static final long RETENTION_MS = 6L * 60 * 60 * 1000;
    public static final int MAX_SAMPLES = 21601;
    private final ArrayDeque<PowerSample> samples = new ArrayDeque<>();

    public void add(PowerSample sample) {
        samples.addLast(sample);
        while (!samples.isEmpty() && (samples.peekFirst().elapsedMs < sample.elapsedMs - RETENTION_MS
                || samples.size() > MAX_SAMPLES)) samples.removeFirst();
    }
    public boolean isEmpty() { return samples.isEmpty(); }
    public int size() { return samples.size(); }
    public PowerSample first() { return samples.peekFirst(); }
    public PowerSample last() { return samples.peekLast(); }
    public void clear() { samples.clear(); }
    @Override public Iterator<PowerSample> iterator() { return samples.iterator(); }
}
