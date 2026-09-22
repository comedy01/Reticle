package dev.reticle.flight;

public final class FlightPolicy {
    public static final double TICKS_PER_SECOND = 20.0D;

    private FlightPolicy() {
    }

    public static double horizontalSpeed(double velX, double velZ) {
        return Math.hypot(velX, velZ) * TICKS_PER_SECOND;
    }

    public static double verticalSpeed(double velY) {
        return velY * TICKS_PER_SECOND;
    }

    public static double totalSpeed(double velX, double velY, double velZ) {
        return Math.sqrt(velX * velX + velY * velY + velZ * velZ) * TICKS_PER_SECOND;
    }

    public static double timeToImpactSeconds(double groundDistance, double verticalSpeedBlocksPerSecond) {
        if (!Double.isFinite(groundDistance) || groundDistance < 0.0D) {
            return Double.POSITIVE_INFINITY;
        }
        if (verticalSpeedBlocksPerSecond >= -0.05D) {
            return Double.POSITIVE_INFINITY;
        }
        return groundDistance / -verticalSpeedBlocksPerSecond;
    }

    public static boolean pullUpWarning(double groundDistance, double verticalSpeedBlocksPerSecond, double thresholdSeconds) {
        return timeToImpactSeconds(groundDistance, verticalSpeedBlocksPerSecond) <= thresholdSeconds;
    }
}
