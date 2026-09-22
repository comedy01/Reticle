package dev.reticle.target;

public final class TargetPolicy {
    private TargetPolicy() {
    }

    public static boolean withinRange(double distance, double maxRange) {
        return distance >= 0.0D && distance <= maxRange;
    }
}
