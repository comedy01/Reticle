package dev.reticle.hud;

public final class Projection {
    private static final double NEAR = 0.05D;

    private Projection() {
    }

    public record ScreenOffset(double x, double y) {
    }

    public static ScreenOffset project(double tx, double ty, double tz,
            double fx, double fy, double fz,
            double ux, double uy, double uz,
            double lx, double ly, double lz,
            double tanHalfFovY, double aspect, double halfWidth, double halfHeight) {
        double forward = tx * fx + ty * fy + tz * fz;
        if (forward < NEAR) {
            return null;
        }
        double right = -(tx * lx + ty * ly + tz * lz);
        double up = tx * ux + ty * uy + tz * uz;
        double x = right / (forward * tanHalfFovY * aspect) * halfWidth;
        double y = -up / (forward * tanHalfFovY) * halfHeight;
        return new ScreenOffset(x, y);
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
