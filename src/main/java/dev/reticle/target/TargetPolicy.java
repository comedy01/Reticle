package dev.reticle.target;

public final class TargetPolicy {
    public static final double ACQUIRE_CONE_DEGREES = 6.0D;

    public static final double RETAIN_CONE_DEGREES = 10.0D;

    private TargetPolicy() {
    }

    public static boolean withinRange(double distance, double maxRange) {
        return distance >= 0.0D && distance <= maxRange;
    }

    public static boolean isWithinCone(double yawOffsetDegrees, double pitchOffsetDegrees, double coneDegrees) {
        return Math.hypot(yawOffsetDegrees, pitchOffsetDegrees) <= coneDegrees;
    }
}
