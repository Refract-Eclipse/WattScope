package dev.wattscope;

public final class SessionStats {
    public double chargeWh, dischargeWh, peakW, observedSeconds, absoluteWs;
    public long startedElapsedMs;
    private PowerSample previous;

    public void reset(long now) {
        chargeWh = dischargeWh = observedSeconds = absoluteWs = 0;
        peakW = Double.NaN;
        startedElapsedMs = now;
        previous = null;
    }

    public void add(PowerSample sample, int expectedIntervalMs) {
        if (sample.valid()) peakW = Double.isFinite(peakW) ? Math.max(peakW, sample.magnitude()) : sample.magnitude();
        if (previous != null && previous.valid() && sample.valid()) {
            double seconds = (sample.elapsedMs - previous.elapsedMs) / 1000.0;
            // Never invent measurements during deep sleep, process pauses, or missing data.
            if (seconds > 0 && seconds <= Math.max(5.0, expectedIntervalMs / 1000.0 * 3.0)) {
                double charge = PowerMath.positiveWh(previous.signedWatts, sample.signedWatts, seconds);
                double discharge = PowerMath.positiveWh(-previous.signedWatts, -sample.signedWatts, seconds);
                chargeWh += charge;
                dischargeWh += discharge;
                absoluteWs += (charge + discharge) * 3600;
                observedSeconds += seconds;
            }
        }
        previous = sample;
    }

    public double averageW() { return observedSeconds > 0 ? absoluteWs / observedSeconds : Double.NaN; }
}
