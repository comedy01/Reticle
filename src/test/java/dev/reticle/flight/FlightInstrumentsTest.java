package dev.reticle.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.reticle.geometry.Angles;
import org.junit.jupiter.api.Test;

class FlightInstrumentsTest {

    @Test
    void horizontalSpeedConvertsTicksToSeconds() {
        assertEquals(20.0, FlightPolicy.horizontalSpeed(1.0, 0.0), 1.0E-9);
        assertEquals(0.0, FlightPolicy.horizontalSpeed(0.0, 0.0), 1.0E-9);
    }

    @Test
    void verticalSpeedKeepsSign() {
        assertEquals(-40.0, FlightPolicy.verticalSpeed(-2.0), 1.0E-9);
        assertEquals(40.0, FlightPolicy.verticalSpeed(2.0), 1.0E-9);
    }

    @Test
    void headingFromYawMatchesCompass() {
        assertEquals(180.0, Angles.headingFromYaw(0.0), 1.0E-9);
        assertEquals(0.0, Angles.headingFromYaw(180.0), 1.0E-9);
        assertEquals(270.0, Angles.headingFromYaw(90.0), 1.0E-9);
        assertEquals(90.0, Angles.headingFromYaw(-90.0), 1.0E-9);
    }

    @Test
    void yawOfDirectionMatchesKnownDirections() {
        assertEquals(0.0, Angles.yawOfDirection(0.0, 1.0), 1.0E-9);   // +Z is south, yaw 0
        assertEquals(90.0, Angles.yawOfDirection(-1.0, 0.0), 1.0E-9); // -X is west, yaw 90
    }

    @Test
    void pitchOfDirectionMatchesLookConvention() {
        assertEquals(-90.0, Angles.pitchOfDirection(0.0, 1.0, 0.0), 1.0E-9);
        assertEquals(90.0, Angles.pitchOfDirection(0.0, -1.0, 0.0), 1.0E-9);
        assertEquals(0.0, Angles.pitchOfDirection(0.0, 0.0, 1.0), 1.0E-9);
    }

    @Test
    void normalizeSignedDegreesWraps() {
        assertEquals(-170.0, Angles.normalizeSignedDegrees(190.0), 1.0E-9);
        assertEquals(170.0, Angles.normalizeSignedDegrees(-190.0), 1.0E-9);
        assertEquals(0.0, Angles.normalizeSignedDegrees(360.0), 1.0E-9);
    }

    @Test
    void timeToImpactInfiniteWhenClimbingOrNoGround() {
        assertTrue(Double.isInfinite(FlightPolicy.timeToImpactSeconds(Double.POSITIVE_INFINITY, -10.0)));
        assertTrue(Double.isInfinite(FlightPolicy.timeToImpactSeconds(50.0, 5.0)));
    }

    @Test
    void timeToImpactComputesWhenDescending() {
        assertEquals(5.0, FlightPolicy.timeToImpactSeconds(50.0, -10.0), 1.0E-9);
    }

    @Test
    void pullUpWarningTriggersNearGround() {
        assertTrue(FlightPolicy.pullUpWarning(20.0, -10.0, 3.0));
        assertFalse(FlightPolicy.pullUpWarning(100.0, -10.0, 3.0));
        assertFalse(FlightPolicy.pullUpWarning(20.0, 5.0, 3.0));
    }

    @Test
    void flightReadingUsesVelocityDirectionWhenMoving() {
        FlightReading reading = FlightReading.of(-1.0, 0.0, 0.0, 90.0, 0.0, 100.0, 50.0, 3.0);
        assertEquals(0.0, reading.flightPathYawOffset(), 1.0E-6);
    }

    @Test
    void flightReadingFallsBackToLookDirectionWhenStationary() {
        FlightReading reading = FlightReading.of(0.0, 0.0, 0.0, 45.0, 10.0, 100.0, Double.POSITIVE_INFINITY, 3.0);
        assertEquals(0.0, reading.flightPathYawOffset(), 1.0E-9);
        assertEquals(0.0, reading.flightPathPitchOffset(), 1.0E-9);
    }
}
