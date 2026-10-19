package dev.wattscope;

public final class PowerSample {
    public final long wallTimeMs, elapsedMs;
    public final double voltageV, currentMa, signedWatts, temperatureC;
    public final int percent, status, plugged, rawCurrent;
    public final String source, issue;

    public PowerSample(long wallTimeMs, long elapsedMs, double voltageV, double currentMa,
                       double temperatureC, int percent, int status, int plugged,
                       int rawCurrent, String source, String issue) {
        this.wallTimeMs = wallTimeMs;
        this.elapsedMs = elapsedMs;
        this.voltageV = voltageV;
        this.currentMa = currentMa;
        this.signedWatts = PowerMath.watts(voltageV, currentMa);
        this.temperatureC = temperatureC;
        this.percent = percent;
        this.status = status;
        this.plugged = plugged;
        this.rawCurrent = rawCurrent;
        this.source = source;
        this.issue = issue;
    }

    public boolean valid() { return Double.isFinite(signedWatts); }
    public double magnitude() { return Math.abs(signedWatts); }

    public String direction() {
        if (!valid()) return "功率不可用";
        if (signedWatts > 0.0001) return "电池净充入";
        if (signedWatts < -0.0001) return "电池净放出";
        return "零净电流";
    }
}
