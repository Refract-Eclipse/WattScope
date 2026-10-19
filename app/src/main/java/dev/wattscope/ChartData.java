package dev.wattscope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Time filtering and extrema-preserving display reduction, independent of Android. */
public final class ChartData {
    private ChartData() { }
    public enum Mode { EMPTY, ZERO, CHARGING, DISCHARGING, MIXED }
    public static final class Point {
        public final PowerSample sample;
        public final boolean breakBefore;
        Point(PowerSample sample, boolean breakBefore) { this.sample = sample; this.breakBefore = breakBefore; }
    }
    public static final class Series {
        public final ArrayList<Point> points = new ArrayList<>();
        public long startMs, endMs, durationMs;
        public int validCount;
        public double maxW;
        private double absoluteWs, observedSeconds;

        /** Time-weighted magnitude over valid intervals inside this chart window. */
        public double averageW() { return observedSeconds > 0 ? absoluteWs / observedSeconds : Double.NaN; }

        public double peakW() { return validCount > 0 ? maxW : Double.NaN; }
        public boolean hasCharging, hasDischarging;

        public Mode mode() {
            if (validCount == 0) return Mode.EMPTY;
            if (hasCharging && hasDischarging) return Mode.MIXED;
            if (hasCharging) return Mode.CHARGING;
            if (hasDischarging) return Mode.DISCHARGING;
            return Mode.ZERO;
        }

        /** 0 is the top of the plot, 1 the bottom. Single directions fill the plot. */
        public double verticalFraction(double signedWatts, double ceiling) {
            return mode() == Mode.MIXED ? 0.5 - signedWatts / (2 * ceiling)
                    : 1 - Math.abs(signedWatts) / ceiling;
        }
    }

    public static long clampRange(long requestedMs) {
        return requestedMs == 0 ? 0 : Math.max(60000, Math.min(SampleHistory.RETENTION_MS, requestedMs));
    }

    public static Series build(SampleHistory history, long requestedMs, int pixels, long maxGapMs) {
        Series out = new Series();
        PowerSample first = history.first(), last = history.last();
        out.endMs = last == null ? 0 : last.elapsedMs;
        long range = clampRange(requestedMs);
        out.durationMs = range == 0 ? Math.max(60000, first == null ? 60000 : out.endMs - first.elapsedMs) : range;
        out.startMs = out.endMs - out.durationMs;
        int buckets = Math.max(1, Math.min(1000, pixels));
        int lastBucket = -1;
        PowerSample previous = null;
        boolean breakNext = true;
        Bucket bucket = new Bucket();
        for (PowerSample sample : history) {
            if (sample.elapsedMs < out.startMs || sample.elapsedMs > out.endMs) continue;
            if (!sample.valid()) {
                bucket.flush(out);
                previous = null; breakNext = true; lastBucket = -1;
                continue;
            }
            out.validCount++;
            if (sample.signedWatts > 0) out.hasCharging = true;
            if (sample.signedWatts < 0) out.hasDischarging = true;
            out.maxW = Math.max(out.maxW, sample.magnitude());
            boolean gap = previous != null && sample.elapsedMs - previous.elapsedMs > maxGapMs;
            if (previous != null && !gap) {
                double seconds = (sample.elapsedMs - previous.elapsedMs) / 1000.0;
                if (seconds > 0) {
                    // Integrate before display reduction; crossings contribute both magnitudes.
                    out.absoluteWs += (PowerMath.positiveWh(previous.signedWatts, sample.signedWatts, seconds)
                            + PowerMath.positiveWh(-previous.signedWatts, -sample.signedWatts, seconds)) * 3600;
                    out.observedSeconds += seconds;
                }
            }
            int index = Math.min(buckets - 1, (int) ((sample.elapsedMs - out.startMs) * (double) buckets / out.durationMs));
            if (gap || index != lastBucket) {
                bucket.flush(out);
                if (gap) breakNext = true;
            }
            if (bucket.empty()) { bucket.breakBefore = breakNext; breakNext = false; }
            bucket.add(sample);
            lastBucket = index;
            previous = sample;
        }
        bucket.flush(out);
        return out;
    }

    private static final class Bucket {
        PowerSample first, last, min, max;
        boolean breakBefore;
        boolean empty() { return first == null; }
        void add(PowerSample sample) {
            if (first == null) first = min = max = sample;
            last = sample;
            if (sample.signedWatts < min.signedWatts) min = sample;
            if (sample.signedWatts > max.signedWatts) max = sample;
        }
        void flush(Series series) {
            if (first == null) return;
            List<PowerSample> selected = new ArrayList<>();
            selected.add(first); selected.add(min); selected.add(max); selected.add(last);
            Collections.sort(selected, Comparator.comparingLong(sample -> sample.elapsedMs));
            PowerSample previous = null;
            boolean firstPoint = true;
            for (PowerSample sample : selected) {
                if (sample == previous) continue;
                series.points.add(new Point(sample, firstPoint && breakBefore));
                firstPoint = false; previous = sample;
            }
            first = last = min = max = null;
        }
    }
}
