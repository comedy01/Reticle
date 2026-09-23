package dev.reticle.geometry;

public final class Angles {
    private Angles() {
    }

    public static double headingFromYaw(double yawDegrees) {
        double heading = (yawDegrees + 180.0D) % 360.0D;
        return heading < 0.0D ? heading + 360.0D : heading;
    }

    public static double normalizeSignedDegrees(double degrees) {
        double d = degrees % 360.0D;
        if (d > 180.0D) {
            d -= 360.0D;
        }
        if (d < -180.0D) {
            d += 360.0D;
        }
        return d;
    }

    public static double yawOfDirection(double x, double z) {
        double heading = Math.toDegrees(Math.atan2(-x, z));
        return heading < 0.0D ? heading + 360.0D : heading;
    }

    public static double pitchOfDirection(double x, double y, double z) {
        double horizontal = Math.hypot(x, z);
        return Math.toDegrees(-Math.atan2(y, horizontal));
    }
}
