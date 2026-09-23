package dev.reticle.hud;

import dev.reticle.client.ReticleClient;
import dev.reticle.config.ReticleConfig;
import dev.reticle.flight.FlightReading;
import dev.reticle.geometry.Angles;
import dev.reticle.sound.ReticleSounds;
import dev.reticle.target.Iff;
import dev.reticle.target.TargetPolicy;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReticleHudRenderer implements HudElement {
    public static final ReticleHudRenderer INSTANCE = new ReticleHudRenderer();

    private static final double MAX_GROUND_SCAN_DISTANCE = 256.0D;
    private static final double MIN_BOX_HALF = 4.0D;
    private static final double BOX_PADDING = 2.0D;

    private static final long LOCK_BUILDUP_MS = 650L;
    private static final int LOCK_START_COLOR = 0xFFFFFFFF;
    private static final int LOCK_FULL_COLOR = 0xFFFF2020;

    private static final double VISOR_SEMI_X = 0.53D;
    private static final double VISOR_SEMI_Y = 0.62D;
    private static final double VISOR_EXPONENT = 3.0D;
    private static final int VISOR_ROW_STEP = 2;
    private static final int VISOR_SHELL_COLOR = 0xE8050608;
    private static final double[] VISOR_BAND_SCALES = {0.97D, 0.94D, 0.90D, 0.85D};
    private static final int[] VISOR_BAND_ALPHAS = {0x90, 0x60, 0x38, 0x18};

    private static final int[] PITCH_LADDER_MARKS = {-60, -50, -40, -30, -20, -10, 10, 20, 30, 40, 50, 60};
    private static final double LADDER_MAX_OFFSET_DEGREES = 40.0D;
    private static final int LADDER_GAP_HALF = 132;
    private static final int LADDER_SEGMENT_LENGTH = 60;

    private static final int NIGHT_VISION_ENGAGE_LIGHT = 7;
    private static final int NIGHT_VISION_DISENGAGE_LIGHT = 9;
    private static final int NIGHT_VISION_TINT_COLOR = 0xFF2BFF4E;
    private static final int NIGHT_VISION_TINT_ALPHA = 0x10;
    private static final int NIGHT_VISION_FLICKER_ALPHA = 3;
    private static final int NIGHT_VISION_SCANLINE_ALPHA = 0x08;
    private static final int NIGHT_VISION_SCANLINE_STEP = 4;
    private static final double NIGHT_VISION_GAMMA_BOOST = 1.0D;

    private static int lockedEntityId = -1;
    private static long lockAcquiredAtMs = 0L;
    private static boolean fullLockSoundPlayed = false;
    private static SoundInstance pullUpSoundInstance = null;
    private static boolean hudWasActive = false;
    private static boolean nightVisionActive = false;
    private static Double savedGamma = null;

    private ReticleHudRenderer() {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        render(guiGraphics, deltaTracker);
    }

    public static boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || HudCompat.isGuiHidden(mc)) {
            return false;
        }
        ReticleConfig config = ReticleClient.config();
        return config.enabled() && config.hideVanillaCrosshair() && player.isFallFlying();
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
            resetFlightState();
            return;
        }
        if (!hudWasActive) {
            ReticleSounds.play(ReticleSounds.HUD_ON, 1.0F, 0.5F);
        }
        hudWasActive = true;

        Camera camera = HudCompat.mainCamera(mc);
        Vec3 camPos = camera.position();
        double camYaw = camera.yRot();
        double camPitch = camera.xRot();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

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

        double fovDegrees = camera.getFov();
        double aspect = (double) width / (double) Math.max(1, height);
        double halfFovY = fovDegrees / 2.0D;
        View view = View.of(camera, halfFovY, aspect, width, height);

        int color = config.primaryColor();

        if (config.showVisor()) {
            drawVisorOverlay(guiGraphics, width, height, color);
        }
        if (config.showNightVision()) {
            updateNightVisionState(level, player);
            if (nightVisionActive) {
                drawNightVision(guiGraphics, width, height);
            }
        } else {
            setNightVisionActive(false);
        }
        if (config.showPitchLadder()) {
            drawPitchLadder(guiGraphics, mc, camPitch, centerX, centerY, halfFovY, color);
        }
        if (config.showInstruments()) {
            drawInstruments(guiGraphics, mc, reading, centerX, centerY, color);
        }
        if (config.showHeading()) {
            drawHeadingTape(guiGraphics, mc, reading.headingDegrees(), centerX, color);
        }
        if (config.showFlightPathMarker()) {
            drawFlightPathMarker(guiGraphics, view, velocity, color);
        }
        if (config.showTargetBoxes()) {
            drawTargetBoxes(guiGraphics, mc, level, player, view, camPos, camYaw, camPitch, config, partialTick);
        } else {
            clearLock();
        }
        if (config.showPullUpWarning() && reading.pullUpWarning()) {
            drawPullUpWarning(guiGraphics, mc, centerX, centerY);
            maybePlayPullUpSound();
        } else {
            stopPullUpSound();
        }

        drawBoresight(guiGraphics, centerX, centerY, color);
    }

    private static void resetFlightState() {
        hudWasActive = false;
        setNightVisionActive(false);
        clearLock();
        stopPullUpSound();
    }

    private static void clearLock() {
        lockedEntityId = -1;
        lockAcquiredAtMs = 0L;
        fullLockSoundPlayed = false;
    }

    private static void maybePlayPullUpSound() {
        if (ReticleSounds.isActive(pullUpSoundInstance)) {
            return;
        }
        pullUpSoundInstance = ReticleSounds.playTracked(ReticleSounds.PULL_UP, 1.0F, 1.0F);
    }

    private static void stopPullUpSound() {
        ReticleSounds.stop(pullUpSoundInstance);
        pullUpSoundInstance = null;
    }

    private static double groundDistance(ClientLevel level, LocalPlayer player, Vec3 from) {
        Vec3 to = from.add(0.0D, -MAX_GROUND_SCAN_DISTANCE, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }
        return from.distanceTo(hit.getLocation());
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static void updateNightVisionState(ClientLevel level, LocalPlayer player) {
        int light = level.getMaxLocalRawBrightness(player.blockPosition());
        if (nightVisionActive) {
            if (light >= NIGHT_VISION_DISENGAGE_LIGHT) {
                setNightVisionActive(false);
            }
        } else if (light <= NIGHT_VISION_ENGAGE_LIGHT) {
            setNightVisionActive(true);
        }
    }

    private static void setNightVisionActive(boolean active) {
        if (active == nightVisionActive) {
            return;
        }
        nightVisionActive = active;
        OptionInstance<Double> gamma = Minecraft.getInstance().options.gamma();
        if (active) {
            savedGamma = gamma.get();
            gamma.set(NIGHT_VISION_GAMMA_BOOST);
        } else if (savedGamma != null) {
            gamma.set(savedGamma);
            savedGamma = null;
        }
    }

    private static void drawNightVision(GuiGraphicsExtractor g, int width, int height) {
        double flicker = Math.sin(System.currentTimeMillis() * 0.006D) * NIGHT_VISION_FLICKER_ALPHA;
        int tintAlpha = (int) Math.max(0, Math.min(255, NIGHT_VISION_TINT_ALPHA + flicker));
        g.fill(0, 0, width, height, withAlpha(NIGHT_VISION_TINT_COLOR, tintAlpha));

        int scanColor = withAlpha(0xFF000000, NIGHT_VISION_SCANLINE_ALPHA);
        for (int y = 0; y < height; y += NIGHT_VISION_SCANLINE_STEP) {
            g.fill(0, y, width, y + 1, scanColor);
        }
    }

    private static void drawVisorOverlay(GuiGraphicsExtractor g, int width, int height, int accentColor) {
        double cx = width / 2.0D;
        double cy = height / 2.0D;
        double semiX = width * VISOR_SEMI_X;
        double semiY = height * VISOR_SEMI_Y;

        g.fill(0, 0, width, height, withAlpha(accentColor, 0x0A));

        int rimColor = withAlpha(accentColor, 0x40);
        for (int y = 0; y < height; y += VISOR_ROW_STEP) {
            double ny = Math.abs((y + VISOR_ROW_STEP / 2.0D) - cy) / semiY;
            int y1 = Math.min(height, y + VISOR_ROW_STEP);

            int glassHalf = visorHalfWidth(ny, semiX, 1.0D);
            fillRowPair(g, 0, (int) cx - glassHalf, width, y, y1, VISOR_SHELL_COLOR);

            int outer = glassHalf;
            for (int band = 0; band < VISOR_BAND_SCALES.length; band++) {
                int inner = visorHalfWidth(ny, semiX, VISOR_BAND_SCALES[band]);
                fillRowPair(g, (int) cx - outer, (int) cx - inner, width, y, y1, VISOR_BAND_ALPHAS[band] << 24);
                outer = inner;
            }
            if (glassHalf > 0 && glassHalf < cx) {
                g.fill((int) cx - glassHalf, y, (int) cx - glassHalf + 1, y1, rimColor);
                g.fill((int) cx + glassHalf - 1, y, (int) cx + glassHalf, y1, rimColor);
            }
        }

        for (int i = 0; i < 3; i++) {
            int x0 = (int) (width * 0.16D) + i * 10;
            int y0 = (int) (height * 0.30D) - i * 3;
            for (int step = 0; step < 40; step++) {
                g.fill(x0 + step * 2, y0 - step, x0 + step * 2 + 2, y0 - step + 1, 0x0CFFFFFF);
            }
        }
    }

    private static int visorHalfWidth(double ny, double semiX, double scale) {
        double n = ny / scale;
        if (n >= 1.0D) {
            return 0;
        }
        double t = 1.0D - Math.pow(n, VISOR_EXPONENT);
        return (int) Math.round(semiX * scale * Math.pow(t, 1.0D / VISOR_EXPONENT));
    }

    private static void fillRowPair(GuiGraphicsExtractor g, int x0, int x1, int width, int y0, int y1, int color) {
        x0 = Math.max(0, x0);
        x1 = Math.min(width / 2, x1);
        if (x1 <= x0) {
            return;
        }
        g.fill(x0, y0, x1, y1, color);
        g.fill(width - x1, y0, width - x0, y1, color);
    }

    private static void drawPitchLadder(GuiGraphicsExtractor g, Minecraft mc, double camPitch,
            int cx, int cy, double halfFovY, int color) {
        drawHorizonLine(g, camPitch, cx, cy, halfFovY, color);
        for (int mark : PITCH_LADDER_MARKS) {
            double pitchOffset = mark - camPitch;
            if (Math.abs(pitchOffset) > halfFovY + LADDER_MAX_OFFSET_DEGREES) {
                continue;
            }
            double dy = -Projection.angularToPixels(pitchOffset, halfFovY, screenHalfHeight(g));
            int y = (int) Math.round(cy + dy);
            drawRung(g, mc, mark, cx, y, color);
        }
    }

    private static double screenHalfHeight(GuiGraphicsExtractor g) {
        return g.guiHeight() / 2.0D;
    }

    private static void drawHorizonLine(GuiGraphicsExtractor g, double camPitch, int cx, int cy, double halfFovY, int color) {
        double pitchOffset = -camPitch;
        if (Math.abs(pitchOffset) > halfFovY + LADDER_MAX_OFFSET_DEGREES) {
            return;
        }
        double dy = -Projection.angularToPixels(pitchOffset, halfFovY, screenHalfHeight(g));
        int y = (int) Math.round(cy + dy);

        g.horizontalLine(cx - LADDER_GAP_HALF - LADDER_SEGMENT_LENGTH, cx - LADDER_GAP_HALF, y, color);
        g.horizontalLine(cx + LADDER_GAP_HALF, cx + LADDER_GAP_HALF + LADDER_SEGMENT_LENGTH, y, color);
        g.verticalLine(cx - LADDER_GAP_HALF - LADDER_SEGMENT_LENGTH, y - 4, y, color);
        g.verticalLine(cx + LADDER_GAP_HALF + LADDER_SEGMENT_LENGTH, y - 4, y, color);
    }

    private static void drawRung(GuiGraphicsExtractor g, Minecraft mc, int mark, int cx, int y, int color) {
        int left0 = cx - LADDER_GAP_HALF - LADDER_SEGMENT_LENGTH;
        int left1 = cx - LADDER_GAP_HALF;
        int right0 = cx + LADDER_GAP_HALF;
        int right1 = cx + LADDER_GAP_HALF + LADDER_SEGMENT_LENGTH;
        boolean climb = mark > 0;

        if (climb) {
            g.horizontalLine(left0, left1, y, color);
            g.horizontalLine(right0, right1, y, color);
        } else {
            drawDashedHorizontal(g, left0, left1, y, color);
            drawDashedHorizontal(g, right0, right1, y, color);
        }

        int tickDir = climb ? 4 : -4;
        g.verticalLine(left0, Math.min(y, y + tickDir), Math.max(y, y + tickDir), color);
        g.verticalLine(right1, Math.min(y, y + tickDir), Math.max(y, y + tickDir), color);

        String label = Integer.toString(Math.abs(mark));
        int lw = mc.font.width(label);
        g.text(mc.font, label, left0 - lw - 4, y - 4, color, false);
        g.text(mc.font, label, right1 + 4, y - 4, color, false);
    }

    private static void drawDashedHorizontal(GuiGraphicsExtractor g, int x1, int x2, int y, int color) {
        int dash = 6;
        int gap = 4;
        int x = x1;
        while (x < x2) {
            int end = Math.min(x + dash, x2);
            g.horizontalLine(x, end, y, color);
            x = end + gap;
        }
    }

    private static void drawBoresight(GuiGraphicsExtractor g, int cx, int cy, int color) {
        g.horizontalLine(cx - 6, cx - 2, cy, color);
        g.horizontalLine(cx + 2, cx + 6, cy, color);
        g.verticalLine(cx, cy - 6, cy - 2, color);
        g.verticalLine(cx, cy + 2, cy + 6, color);
    }

    private static void drawFlightPathMarker(GuiGraphicsExtractor g, View view, Vec3 velocity, int color) {
        if (velocity.lengthSqr() < 1.0E-6D) {
            return;
        }
        Projection.ScreenOffset p = view.project(velocity);
        if (p == null) {
            return;
        }
        int mx = view.screenX(p.x());
        int my = view.screenY(p.y());
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

    private record Contact(LivingEntity entity, int sx, int sy, int halfW, int halfH, int color,
            double distance, double yawOffset, double pitchOffset) {
    }

    private static void drawTargetBoxes(GuiGraphicsExtractor g, Minecraft mc, ClientLevel level, LocalPlayer player,
            View view, Vec3 camPos, double camYaw, double camPitch, ReticleConfig config, float partialTick) {
        double range = config.targetRange();
        AABB scanBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != player && e.isAlive() && TargetPolicy.withinRange(e.distanceTo(player), range));

        List<Contact> contacts = new ArrayList<>();
        int stillLockedId = -1;
        int bestNewCandidateId = -1;
        double bestNewAngle = Double.MAX_VALUE;

        for (LivingEntity entity : nearby) {
            Vec3 lerpPos = entity.getPosition(partialTick);
            AABB box = entity.getBoundingBox().move(lerpPos.subtract(entity.position()));
            Vec3 centre = box.getCenter();
            Vec3 toEntity = centre.subtract(camPos);
            double distance = toEntity.length();
            if (distance < 0.5D) {
                continue;
            }

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            boolean visible = true;
            for (int corner = 0; corner < 8 && visible; corner++) {
                double wx = (corner & 1) == 0 ? box.minX : box.maxX;
                double wy = (corner & 2) == 0 ? box.minY : box.maxY;
                double wz = (corner & 4) == 0 ? box.minZ : box.maxZ;
                Projection.ScreenOffset p = view.project(wx - camPos.x, wy - camPos.y, wz - camPos.z);
                if (p == null) {
                    visible = false;
                } else {
                    minX = Math.min(minX, p.x());
                    minY = Math.min(minY, p.y());
                    maxX = Math.max(maxX, p.x());
                    maxY = Math.max(maxY, p.y());
                }
            }
            if (!visible || maxX < -view.halfWidth() || minX > view.halfWidth()
                    || maxY < -view.halfHeight() || minY > view.halfHeight()) {
                continue;
            }

            Vec3 eyes = entity.getEyePosition(partialTick);
            BlockHitResult hit = level.clip(new ClipContext(camPos, eyes, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                continue;
            }

            Vec3 dir = toEntity.scale(1.0D / distance);
            double yawOffset = Angles.normalizeSignedDegrees(Angles.yawOfDirection(dir.x, dir.z) - camYaw);
            double pitchOffset = Angles.pitchOfDirection(dir.x, dir.y, dir.z) - camPitch;

            int sx = view.screenX((minX + maxX) / 2.0D);
            int sy = view.screenY((minY + maxY) / 2.0D);
            int halfW = (int) Math.ceil(Math.max(MIN_BOX_HALF, (maxX - minX) / 2.0D + BOX_PADDING));
            int halfH = (int) Math.ceil(Math.max(MIN_BOX_HALF, (maxY - minY) / 2.0D + BOX_PADDING));
            int color = boxColor(entity, config);

            contacts.add(new Contact(entity, sx, sy, halfW, halfH, color, distance, yawOffset, pitchOffset));

            int entityId = entity.getId();
            if (entityId == lockedEntityId && TargetPolicy.isWithinCone(yawOffset, pitchOffset, TargetPolicy.RETAIN_CONE_DEGREES)) {
                stillLockedId = entityId;
            } else if (entityId != lockedEntityId) {
                double angle = Math.hypot(yawOffset, pitchOffset);
                if (angle < bestNewAngle && TargetPolicy.isWithinCone(yawOffset, pitchOffset, TargetPolicy.ACQUIRE_CONE_DEGREES)) {
                    bestNewAngle = angle;
                    bestNewCandidateId = entityId;
                }
            }
        }

        int finalLockId = stillLockedId != -1 ? stillLockedId : bestNewCandidateId;
        if (finalLockId != lockedEntityId) {
            lockAcquiredAtMs = System.currentTimeMillis();
            fullLockSoundPlayed = false;
        }
        lockedEntityId = finalLockId;

        for (Contact contact : contacts) {
            if (contact.entity().getId() == lockedEntityId) {
                drawLockedTarget(g, mc, contact);
            } else {
                drawContactMarker(g, contact);
            }
        }
    }

    private static void drawLockedTarget(GuiGraphicsExtractor g, Minecraft mc, Contact contact) {
        double progress = Math.min(1.0D, (System.currentTimeMillis() - lockAcquiredAtMs) / (double) LOCK_BUILDUP_MS);
        int color = lerpColor(LOCK_START_COLOR, LOCK_FULL_COLOR, progress);
        if (progress >= 1.0D && !fullLockSoundPlayed) {
            ReticleSounds.play(ReticleSounds.LOCK_ON, 1.0F, 0.6F);
            fullLockSoundPlayed = true;
        }

        drawBoxCorners(g, contact.sx(), contact.sy(), contact.halfW(), contact.halfH(), color);
        String label = String.format(Locale.ROOT, "%.0fm", contact.distance());
        int lw = mc.font.width(label);
        g.text(mc.font, label, contact.sx() - lw / 2, contact.sy() + contact.halfH() + 4, color, false);
    }

    private static int lerpColor(int from, int to, double t) {
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = (int) Math.round(fr + (tr - fr) * t);
        int gC = (int) Math.round(fg + (tg - fg) * t);
        int b = (int) Math.round(fb + (tb - fb) * t);
        int a = (to >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (gC << 8) | b;
    }

    private static void drawContactMarker(GuiGraphicsExtractor g, Contact contact) {
        drawBoxCorners(g, contact.sx(), contact.sy(), contact.halfW(), contact.halfH(), withAlpha(contact.color(), 0x80));
    }

    private static void drawBoxCorners(GuiGraphicsExtractor g, int sx, int sy, int halfW, int halfH, int color) {
        int tickX = Math.min(4, halfW);
        int tickY = Math.min(4, halfH);
        int left = sx - halfW;
        int right = sx + halfW;
        int top = sy - halfH;
        int bottom = sy + halfH;
        g.horizontalLine(left, left + tickX, top, color);
        g.verticalLine(left, top, top + tickY, color);
        g.horizontalLine(right - tickX, right, top, color);
        g.verticalLine(right, top, top + tickY, color);
        g.horizontalLine(left, left + tickX, bottom, color);
        g.verticalLine(left, bottom - tickY, bottom, color);
        g.horizontalLine(right - tickX, right, bottom, color);
        g.verticalLine(right, bottom - tickY, bottom, color);
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

    private record View(Vector3fc forward, Vector3fc up, Vector3fc left,
            double tanHalfFovY, double aspect, double halfWidth, double halfHeight) {
        static View of(Camera camera, double halfFovYDegrees, double aspect, int width, int height) {
            return new View(camera.forwardVector(), camera.upVector(), camera.leftVector(),
                    Math.tan(Math.toRadians(halfFovYDegrees)), aspect, width / 2.0D, height / 2.0D);
        }

        Projection.ScreenOffset project(Vec3 relative) {
            return project(relative.x, relative.y, relative.z);
        }

        Projection.ScreenOffset project(double x, double y, double z) {
            return Projection.project(x, y, z,
                    forward.x(), forward.y(), forward.z(),
                    up.x(), up.y(), up.z(),
                    left.x(), left.y(), left.z(),
                    tanHalfFovY, aspect, halfWidth, halfHeight);
        }

        int screenX(double offsetX) {
            return (int) Math.round(halfWidth + offsetX);
        }

        int screenY(double offsetY) {
            return (int) Math.round(halfHeight + offsetY);
        }
    }
}
