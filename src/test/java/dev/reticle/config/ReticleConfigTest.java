package dev.reticle.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReticleConfigTest {
    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve("config").resolve(ReticleConfig.FILE_NAME);
    }

    private void write(String json) throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), json, StandardCharsets.UTF_8);
    }

    @Test
    void defaults() {
        ReticleConfig config = new ReticleConfig();
        assertTrue(config.enabled());
        assertTrue(config.showTargetBoxes());
        assertEquals(ReticlePolicy.DEFAULT_PRIMARY_COLOR, config.primaryColor());
        assertEquals(ReticlePolicy.DEFAULT_TARGET_RANGE, config.targetRange());
        assertEquals(ReticlePolicy.DEFAULT_PULL_UP_SECONDS, config.pullUpWarningSeconds());
    }

    @Test
    void settersClamp() {
        ReticleConfig config = new ReticleConfig();

        config.setEnabled(false);
        assertFalse(config.enabled());

        config.setShowTargetBoxes(false);
        assertFalse(config.showTargetBoxes());

        config.setPrimaryColor(0x00FF0000);
        assertEquals(0xFFFF0000, config.primaryColor());

        config.setTargetRange(500.0);
        assertEquals(ReticlePolicy.MAX_TARGET_RANGE, config.targetRange());
        config.setTargetRange(-5.0);
        assertEquals(ReticlePolicy.MIN_TARGET_RANGE, config.targetRange());

        config.setPullUpWarningSeconds(100.0);
        assertEquals(ReticlePolicy.MAX_PULL_UP_SECONDS, config.pullUpWarningSeconds());
        config.setPullUpWarningSeconds(0.0);
        assertEquals(ReticlePolicy.MIN_PULL_UP_SECONDS, config.pullUpWarningSeconds());
    }

    @Test
    void resetToDefaults() {
        ReticleConfig config = new ReticleConfig();
        config.setEnabled(false);
        config.setShowTargetBoxes(false);
        config.setTargetRange(10.0);
        config.resetToDefaults();
        assertTrue(config.enabled());
        assertTrue(config.showTargetBoxes());
        assertEquals(ReticlePolicy.DEFAULT_TARGET_RANGE, config.targetRange());
    }

    @Test
    void saveAndLoad() throws IOException {
        ReticleConfig config = new ReticleConfig();
        config.setEnabled(false);
        config.setTargetRange(30.0);
        config.setPullUpWarningSeconds(5.0);
        config.save(file());

        ReticleConfig reloaded = ReticleConfig.load(file());
        assertFalse(reloaded.enabled());
        assertEquals(30.0, reloaded.targetRange());
        assertEquals(5.0, reloaded.pullUpWarningSeconds());
    }

    @Test
    void saveCreatesDirectories() throws IOException {
        new ReticleConfig().save(file());
        assertTrue(Files.isRegularFile(file()));
        assertFalse(Files.exists(file().resolveSibling(ReticleConfig.FILE_NAME + ".tmp")));
    }

    @Test
    void missingFile() {
        ReticleConfig config = ReticleConfig.load(file());
        assertEquals(ReticlePolicy.DEFAULT_TARGET_RANGE, config.targetRange());
        assertTrue(Files.isRegularFile(file()));
    }

    @Test
    void outOfRangeValues() throws IOException {
        write("{\"targetRange\": 999, \"pullUpWarningSeconds\": -4}");
        ReticleConfig config = ReticleConfig.load(file());
        assertEquals(ReticlePolicy.MAX_TARGET_RANGE, config.targetRange());
        assertEquals(ReticlePolicy.MIN_PULL_UP_SECONDS, config.pullUpWarningSeconds());
    }

    @Test
    void nonFiniteNumbers() throws IOException {
        write("{\"targetRange\": NaN}");
        ReticleConfig config = ReticleConfig.load(file());
        assertEquals(ReticlePolicy.DEFAULT_TARGET_RANGE, config.targetRange());
    }

    @Test
    void brokenFileIsMovedAside() throws IOException {
        write("{ this is not json");
        ReticleConfig config = ReticleConfig.load(file());
        assertEquals(ReticlePolicy.DEFAULT_TARGET_RANGE, config.targetRange());

        Path backup = file().resolveSibling(ReticleConfig.FILE_NAME + ".broken");
        assertEquals("{ this is not json", Files.readString(backup, StandardCharsets.UTF_8));
    }

    @Test
    void emptyFile() throws IOException {
        write("");
        ReticleConfig config = ReticleConfig.load(file());
        assertEquals(ReticlePolicy.DEFAULT_TARGET_RANGE, config.targetRange());
    }
}
