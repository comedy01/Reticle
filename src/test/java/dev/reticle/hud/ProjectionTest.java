package dev.reticle.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectionTest {
    private static Projection.ScreenOffset project(double x, double y, double z) {
        return Projection.project(x, y, z, 0, 0, 1, 0, 1, 0, 1, 0, 0,
                Math.tan(Math.toRadians(35.0D)), 16.0D / 9.0D, 320.0D, 180.0D);
    }

    @Test
    void pointOnViewAxisLandsAtCentre() {
        Projection.ScreenOffset p = project(0, 0, 10);
        assertEquals(0.0D, p.x(), 1.0E-9);
        assertEquals(0.0D, p.y(), 1.0E-9);
    }

    @Test
    void pointAtTopEdgeOfFovLandsAtTopOfScreen() {
        Projection.ScreenOffset p = project(0, Math.tan(Math.toRadians(35.0D)) * 10, 10);
        assertEquals(-180.0D, p.y(), 1.0E-6);
    }

    @Test
    void pointToTheRightProjectsRight() {
        Projection.ScreenOffset p = project(-1, 0, 10);
        assertEquals(true, p.x() > 0);
    }

    @Test
    void pointBehindCameraIsRejected() {
        assertNull(project(0, 0, -5));
    }
}
