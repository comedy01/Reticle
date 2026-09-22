package dev.reticle.hud;

/**
 * Maps an angular offset from the camera's look direction to a HUD pixel offset,
 * as a pinhole-camera projection driven by the current field of view. Used for both
 * the flight-path marker and target boxes so they line up with what's on screen
 * without touching the render pipeline's projection matrices directly.
 */
public final class Projection {
    private Projection() {
    }

    public static double angularToPixels(double angularOffsetDegrees, double halfFovDegrees, double halfExtentPixels) {
        double clampedOffset = Math.max(-89.0D, Math.min(89.0D, angularOffsetDegrees));
        double t = Math.tan(Math.toRadians(clampedOffset)) / Math.tan(Math.toRadians(halfFovDegrees));
        return t * halfExtentPixels;
    }

    public static boolean isInFrontHemisphere(double angularOffsetDegrees) {
        return Math.abs(angularOffsetDegrees) < 90.0D;
    }
}
