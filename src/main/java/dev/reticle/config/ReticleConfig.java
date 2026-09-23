package dev.reticle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ReticleConfig {
    public static final String FILE_NAME = "reticle.json";

    private static final Logger LOGGER = LoggerFactory.getLogger("reticle");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("enabled")
    private boolean enabled = ReticlePolicy.DEFAULT_ENABLED;

    @SerializedName("showInstruments")
    private boolean showInstruments = ReticlePolicy.DEFAULT_SHOW_INSTRUMENTS;

    @SerializedName("showFlightPathMarker")
    private boolean showFlightPathMarker = ReticlePolicy.DEFAULT_SHOW_FLIGHT_PATH_MARKER;

    @SerializedName("showHeading")
    private boolean showHeading = ReticlePolicy.DEFAULT_SHOW_HEADING;

    @SerializedName("showTargetBoxes")
    private boolean showTargetBoxes = ReticlePolicy.DEFAULT_SHOW_TARGET_BOXES;

    @SerializedName("showPullUpWarning")
    private boolean showPullUpWarning = ReticlePolicy.DEFAULT_SHOW_PULL_UP_WARNING;

    @SerializedName("showPitchLadder")
    private boolean showPitchLadder = ReticlePolicy.DEFAULT_SHOW_PITCH_LADDER;

    @SerializedName("showVisor")
    private boolean showVisor = ReticlePolicy.DEFAULT_SHOW_VISOR;

    @SerializedName("hideVanillaCrosshair")
    private boolean hideVanillaCrosshair = ReticlePolicy.DEFAULT_HIDE_VANILLA_CROSSHAIR;

    @SerializedName("soundsEnabled")
    private boolean soundsEnabled = ReticlePolicy.DEFAULT_SOUNDS_ENABLED;

    @SerializedName("showNightVision")
    private boolean showNightVision = ReticlePolicy.DEFAULT_SHOW_NIGHT_VISION;

    @SerializedName("primaryColor")
    private int primaryColor = ReticlePolicy.DEFAULT_PRIMARY_COLOR;

    @SerializedName("hostileColor")
    private int hostileColor = ReticlePolicy.DEFAULT_HOSTILE_COLOR;

    @SerializedName("passiveColor")
    private int passiveColor = ReticlePolicy.DEFAULT_PASSIVE_COLOR;

    @SerializedName("playerColor")
    private int playerColor = ReticlePolicy.DEFAULT_PLAYER_COLOR;

    @SerializedName("targetRange")
    private double targetRange = ReticlePolicy.DEFAULT_TARGET_RANGE;

    @SerializedName("pullUpWarningSeconds")
    private double pullUpWarningSeconds = ReticlePolicy.DEFAULT_PULL_UP_SECONDS;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public boolean showInstruments() {
        return showInstruments;
    }

    public void setShowInstruments(boolean value) {
        showInstruments = value;
    }

    public boolean showFlightPathMarker() {
        return showFlightPathMarker;
    }

    public void setShowFlightPathMarker(boolean value) {
        showFlightPathMarker = value;
    }

    public boolean showHeading() {
        return showHeading;
    }

    public void setShowHeading(boolean value) {
        showHeading = value;
    }

    public boolean showTargetBoxes() {
        return showTargetBoxes;
    }

    public void setShowTargetBoxes(boolean value) {
        showTargetBoxes = value;
    }

    public boolean showPullUpWarning() {
        return showPullUpWarning;
    }

    public void setShowPullUpWarning(boolean value) {
        showPullUpWarning = value;
    }

    public boolean showPitchLadder() {
        return showPitchLadder;
    }

    public void setShowPitchLadder(boolean value) {
        showPitchLadder = value;
    }

    public boolean showVisor() {
        return showVisor;
    }

    public void setShowVisor(boolean value) {
        showVisor = value;
    }

    public boolean hideVanillaCrosshair() {
        return hideVanillaCrosshair;
    }

    public void setHideVanillaCrosshair(boolean value) {
        hideVanillaCrosshair = value;
    }

    public boolean soundsEnabled() {
        return soundsEnabled;
    }

    public void setSoundsEnabled(boolean value) {
        soundsEnabled = value;
    }

    public boolean showNightVision() {
        return showNightVision;
    }

    public void setShowNightVision(boolean value) {
        showNightVision = value;
    }

    public int primaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(int value) {
        primaryColor = ReticlePolicy.normalizeColor(value);
    }

    public int hostileColor() {
        return hostileColor;
    }

    public void setHostileColor(int value) {
        hostileColor = ReticlePolicy.normalizeColor(value);
    }

    public int passiveColor() {
        return passiveColor;
    }

    public void setPassiveColor(int value) {
        passiveColor = ReticlePolicy.normalizeColor(value);
    }

    public int playerColor() {
        return playerColor;
    }

    public void setPlayerColor(int value) {
        playerColor = ReticlePolicy.normalizeColor(value);
    }

    public double targetRange() {
        return targetRange;
    }

    public void setTargetRange(double value) {
        targetRange = ReticlePolicy.clampTargetRange(value);
    }

    public double pullUpWarningSeconds() {
        return pullUpWarningSeconds;
    }

    public void setPullUpWarningSeconds(double value) {
        pullUpWarningSeconds = ReticlePolicy.clampPullUpSeconds(value);
    }

    public void resetToDefaults() {
        enabled = ReticlePolicy.DEFAULT_ENABLED;
        showInstruments = ReticlePolicy.DEFAULT_SHOW_INSTRUMENTS;
        showFlightPathMarker = ReticlePolicy.DEFAULT_SHOW_FLIGHT_PATH_MARKER;
        showHeading = ReticlePolicy.DEFAULT_SHOW_HEADING;
        showTargetBoxes = ReticlePolicy.DEFAULT_SHOW_TARGET_BOXES;
        showPullUpWarning = ReticlePolicy.DEFAULT_SHOW_PULL_UP_WARNING;
        showPitchLadder = ReticlePolicy.DEFAULT_SHOW_PITCH_LADDER;
        showVisor = ReticlePolicy.DEFAULT_SHOW_VISOR;
        hideVanillaCrosshair = ReticlePolicy.DEFAULT_HIDE_VANILLA_CROSSHAIR;
        soundsEnabled = ReticlePolicy.DEFAULT_SOUNDS_ENABLED;
        showNightVision = ReticlePolicy.DEFAULT_SHOW_NIGHT_VISION;
        primaryColor = ReticlePolicy.DEFAULT_PRIMARY_COLOR;
        hostileColor = ReticlePolicy.DEFAULT_HOSTILE_COLOR;
        passiveColor = ReticlePolicy.DEFAULT_PASSIVE_COLOR;
        playerColor = ReticlePolicy.DEFAULT_PLAYER_COLOR;
        targetRange = ReticlePolicy.DEFAULT_TARGET_RANGE;
        pullUpWarningSeconds = ReticlePolicy.DEFAULT_PULL_UP_SECONDS;
    }

    private void sanitize() {
        setPrimaryColor(primaryColor);
        setHostileColor(hostileColor);
        setPassiveColor(passiveColor);
        setPlayerColor(playerColor);
        setTargetRange(targetRange);
        setPullUpWarningSeconds(pullUpWarningSeconds);
    }

    public static ReticleConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            ReticleConfig fresh = new ReticleConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ReticleConfig loaded = GSON.fromJson(reader, ReticleConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            ReticleConfig fresh = new ReticleConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(
                    file,
                    file.resolveSibling(file.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
