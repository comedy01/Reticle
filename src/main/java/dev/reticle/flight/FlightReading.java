package dev.reticle.flight;

import dev.reticle.geometry.Angles;

public record FlightReading(
        double horizontalSpeed,
        double verticalSpeed,
        double totalSpeed,
        double headingDegrees,
        double altitude,
        double groundDistance,
        double flightPathYawOffset,
        double flightPathPitchOffset,
        boolean pullUpWarning) {

    public static FlightReading of(
            double velX, double velY, double velZ,
            double lookYawDeg, double lookPitchDeg,
            double posY, double groundDistance,
            double pullUpThresholdSeconds) {

        double horizontalSpeed = FlightPolicy.horizontalSpeed(velX, velZ);
        double verticalSpeed = FlightPolicy.verticalSpeed(velY);
        double totalSpeed = FlightPolicy.totalSpeed(velX, velY, velZ);
        double heading = Angles.headingFromYaw(lookYawDeg);

        boolean hasVelocity = totalSpeed > 1.0E-3D;
        double velocityYaw = hasVelocity ? Angles.yawOfDirection(velX, velZ) : lookYawDeg;
        double velocityPitch = hasVelocity ? Angles.pitchOfDirection(velX, velY, velZ) : lookPitchDeg;

        double yawOffset = Angles.normalizeSignedDegrees(velocityYaw - lookYawDeg);
        double pitchOffset = velocityPitch - lookPitchDeg;

        boolean warning = FlightPolicy.pullUpWarning(groundDistance, verticalSpeed, pullUpThresholdSeconds);

        return new FlightReading(horizontalSpeed, verticalSpeed, totalSpeed, heading,
                posY, groundDistance, yawOffset, pitchOffset, warning);
    }
}
