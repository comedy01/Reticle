package dev.reticle.hud;

import dev.reticle.client.ReticleClient;
import dev.reticle.config.ReticleConfig;
import dev.reticle.flight.FlightReading;
import dev.reticle.geometry.Angles;
import dev.reticle.target.Iff;
import dev.reticle.target.TargetPolicy;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Draws the fighter-HUD overlay while gliding. Target boxes and the flight-path
 * marker are positioned by projecting an angular offset from the camera's look
 * direction through the current field of view (see {@link Projection}), not by
 * touching the render pipeline's matrices directly - simpler and stable across
 * Minecraft versions, at the cost of not accounting for camera roll (vanilla has none).
 */
public final class ReticleHudRenderer implements HudElement {
    public static final ReticleHudRenderer INSTANCE = new ReticleHudRenderer();

    private static final double MAX_GROUND_SCAN_DISTANCE = 256.0D;
    private static final double FOV_MARGIN_DEGREES = 5.0D;

    private ReticleHudRenderer() {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        render(guiGraphics, deltaTracker);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || HudCompat.isGuiHidden(mc)) {
            return;
        }
        ReticleConfig config = ReticleClient.config();
        if (!config.enabled() || !player.isFallFlying()) {
            return;
        }

        Camera camera = HudCompat.mainCamera(mc);
        Vec3 camPos = camera.position();
        double camYaw = camera.yRot();
        double camPitch = camera.xRot();

        Vec3 velocity = player.getDeltaMovement();
        double groundDistance = groundDistance(level, player, camPos);

        FlightReading reading = FlightReading.of(
                velocity.x, velocity.y, velocity.z,
                camYaw, camPitch,
                player.getY(), groundDistance,
                config.pullUpWarningSeconds());

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        double fovDegrees = mc.options.fov().get();
        double aspect = (double) width / (double) Math.max(1, height);
        double halfFovY = fovDegrees / 2.0D;
        double halfFovX = Math.toDegrees(Math.atan(Math.tan(Math.toRadians(halfFovY)) * aspect));

        int color = config.primaryColor();

        if (config.showInstruments()) {
            drawInstruments(guiGraphics, mc, reading, centerX, centerY, color);
        }
        if (config.showHeading()) {
            drawHeadingTape(guiGraphics, mc, reading.headingDegrees(), centerX, color);
        }
        if (config.showFlightPathMarker()) {
            drawFlightPathMarker(guiGraphics, reading, centerX, centerY, halfFovX, halfFovY, width, height, color);
        }
        if (config.showTargetBoxes()) {
            drawTargetBoxes(guiGraphics, level, player, camPos, camYaw, camPitch, halfFovX, halfFovY,
                    centerX, centerY, width, height, config);
        }
        if (config.showPullUpWarning() && reading.pullUpWarning()) {
            drawPullUpWarning(guiGraphics, mc, centerX, centerY);
        }

        drawBoresight(guiGraphics, centerX, centerY, color);
    }

    private static double groundDistance(ClientLevel level, LocalPlayer player, Vec3 from) {
        Vec3 to = from.add(0.0D, -MAX_GROUND_SCAN_DISTANCE, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }
        return from.distanceTo(hit.getLocation());
    }

    private static void drawBoresight(GuiGraphicsExtractor g, int cx, int cy, int color) {
        g.horizontalLine(cx - 6, cx - 2, cy, color);
        g.horizontalLine(cx + 2, cx + 6, cy, color);
        g.verticalLine(cx, cy - 6, cy - 2, color);
        g.verticalLine(cx, cy + 2, cy + 6, color);
    }

    private static void drawFlightPathMarker(GuiGraphicsExtractor g, FlightReading reading, int cx, int cy,
            double halfFovX, double halfFovY, int width, int height, int color) {
        double dx = Projection.angularToPixels(reading.flightPathYawOffset(), halfFovX, width / 2.0D);
        double dy = -Projection.angularToPixels(reading.flightPathPitchOffset(), halfFovY, height / 2.0D);
        int mx = (int) Math.round(cx + dx);
        int my = (int) Math.round(cy + dy);
        int r = 4;
        g.horizontalLine(mx - r - 3, mx - r, my, color);
        g.horizontalLine(mx + r, mx + r + 3, my, color);
        g.verticalLine(mx, my - r - 3, my - r, color);
        g.horizontalLine(mx - r, mx + r, my - r, color);
        g.horizontalLine(mx - r, mx + r, my + r, color);
        g.verticalLine(mx - r, my - r, my + r, color);
        g.verticalLine(mx + r, my - r, my + r, color);
    }

    private static void drawInstruments(GuiGraphicsExtractor g, Minecraft mc, FlightReading reading, int cx, int cy, int color) {
        String speed = String.format(Locale.ROOT, "SPD %5.1f", reading.horizontalSpeed());
        String vspeed = String.format(Locale.ROOT, "VSI %+5.1f", reading.verticalSpeed());
        String agl = Double.isFinite(reading.groundDistance())
                ? String.format(Locale.ROOT, "AGL %5.1f", reading.groundDistance())
                : "AGL  ----";
        String alt = String.format(Locale.ROOT, "ALT %5.0f", reading.altitude());

        g.text(mc.font, speed, cx - 120, cy - 10, color, false);
        g.text(mc.font, vspeed, cx - 120, cy + 2, color, false);
        g.text(mc.font, agl, cx + 70, cy - 10, color, false);
        g.text(mc.font, alt, cx + 70, cy + 2, color, false);
    }

    private static void drawHeadingTape(GuiGraphicsExtractor g, Minecraft mc, double headingDegrees, int cx, int color) {
        String heading = String.format(Locale.ROOT, "%03d", Math.round(headingDegrees) % 360);
        int textWidth = mc.font.width(heading);
        g.text(mc.font, heading, cx - textWidth / 2, 6, color, false);
        g.horizontalLine(cx - 20, cx + 20, 15, color);
    }

    private static void drawPullUpWarning(GuiGraphicsExtractor g, Minecraft mc, int cx, int cy) {
        boolean on = (System.currentTimeMillis() / 250L) % 2L == 0L;
        if (!on) {
            return;
        }
        String text = "PULL UP";
        int color = 0xFFFF3B30;
        int textWidth = mc.font.width(text);
        g.text(mc.font, text, cx - textWidth / 2, cy - 40, color, false);
    }

    private static void drawTargetBoxes(GuiGraphicsExtractor g, ClientLevel level, LocalPlayer player,
            Vec3 camPos, double camYaw, double camPitch, double halfFovX, double halfFovY,
            int cx, int cy, int width, int height, ReticleConfig config) {

        double range = config.targetRange();
        AABB scanBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != player && e.isAlive() && TargetPolicy.withinRange(e.distanceTo(player), range));

        for (LivingEntity entity : nearby) {
            Vec3 eyes = entity.getEyePosition();
            Vec3 toEntity = eyes.subtract(camPos);
            double distance = toEntity.length();
            if (distance < 0.5D) {
                continue;
            }
            Vec3 dir = toEntity.scale(1.0D / distance);
            double entityYaw = Angles.yawOfDirection(dir.x, dir.z);
            double entityPitch = Angles.pitchOfDirection(dir.x, dir.y, dir.z);
            double yawOffset = Angles.normalizeSignedDegrees(entityYaw - camYaw);
            double pitchOffset = entityPitch - camPitch;

            if (!Projection.isInFrontHemisphere(yawOffset) || !Projection.isInFrontHemisphere(pitchOffset)) {
                continue;
            }
            if (Math.abs(yawOffset) > halfFovX + FOV_MARGIN_DEGREES || Math.abs(pitchOffset) > halfFovY + FOV_MARGIN_DEGREES) {
                continue;
            }

            BlockHitResult hit = level.clip(new ClipContext(camPos, eyes, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                continue;
            }

            double dx = Projection.angularToPixels(yawOffset, halfFovX, width / 2.0D);
            double dy = -Projection.angularToPixels(pitchOffset, halfFovY, height / 2.0D);
            int sx = (int) Math.round(cx + dx);
            int sy = (int) Math.round(cy + dy);

            int size = (int) Math.max(6.0D, Math.min(28.0D, 400.0D / distance));
            int color = boxColor(entity, config);

            drawBoxCorners(g, sx, sy, size, color);
        }
    }

    private static void drawBoxCorners(GuiGraphicsExtractor g, int sx, int sy, int size, int color) {
        int tick = 4;
        g.horizontalLine(sx - size, sx - size + tick, sy - size, color);
        g.verticalLine(sx - size, sy - size, sy - size + tick, color);
        g.horizontalLine(sx + size - tick, sx + size, sy - size, color);
        g.verticalLine(sx + size, sy - size, sy - size + tick, color);
        g.horizontalLine(sx - size, sx - size + tick, sy + size, color);
        g.verticalLine(sx - size, sy + size - tick, sy + size, color);
        g.horizontalLine(sx + size - tick, sx + size, sy + size, color);
        g.verticalLine(sx + size, sy + size - tick, sy + size, color);
    }

    private static int boxColor(LivingEntity entity, ReticleConfig config) {
        return switch (classify(entity)) {
            case HOSTILE -> config.hostileColor();
            case PLAYER -> config.playerColor();
            case PASSIVE -> config.passiveColor();
        };
    }

    private static Iff classify(LivingEntity entity) {
        if (entity instanceof Player) {
            return Iff.PLAYER;
        }
        if (entity instanceof Enemy) {
            return Iff.HOSTILE;
        }
        return Iff.PASSIVE;
    }
}
