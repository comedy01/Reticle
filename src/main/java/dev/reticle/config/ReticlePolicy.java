package dev.reticle.config;

public final class ReticlePolicy {
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_SHOW_INSTRUMENTS = true;
    public static final boolean DEFAULT_SHOW_FLIGHT_PATH_MARKER = true;
    public static final boolean DEFAULT_SHOW_HEADING = true;
    public static final boolean DEFAULT_SHOW_TARGET_BOXES = true;
    public static final boolean DEFAULT_SHOW_PULL_UP_WARNING = true;

    public static final int DEFAULT_PRIMARY_COLOR = 0xFF39FF14;
    public static final int DEFAULT_HOSTILE_COLOR = 0xFFFF3B30;
    public static final int DEFAULT_PASSIVE_COLOR = 0xFF39FF14;
    public static final int DEFAULT_PLAYER_COLOR = 0xFF33CCFF;

    public static final double MIN_TARGET_RANGE = 8.0D;
    public static final double MAX_TARGET_RANGE = 128.0D;
    public static final double DEFAULT_TARGET_RANGE = 64.0D;

    public static final double MIN_PULL_UP_SECONDS = 1.0D;
    public static final double MAX_PULL_UP_SECONDS = 8.0D;
    public static final double DEFAULT_PULL_UP_SECONDS = 3.0D;

    private ReticlePolicy() {
    }

    public static double clampTargetRange(double value) {
        return clamp(value, MIN_TARGET_RANGE, MAX_TARGET_RANGE, DEFAULT_TARGET_RANGE);
    }

    public static double clampPullUpSeconds(double value) {
        return clamp(value, MIN_PULL_UP_SECONDS, MAX_PULL_UP_SECONDS, DEFAULT_PULL_UP_SECONDS);
    }

    public static int normalizeColor(int color) {
        return (color >>> 24) == 0 ? color | 0xFF000000 : color;
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
