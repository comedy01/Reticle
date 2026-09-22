package dev.reticle.client;

import dev.reticle.config.ReticleConfig;

import java.nio.file.Path;

public final class ReticleClient {
    public static final String MOD_ID = "reticle";

    private static ReticleConfig config = new ReticleConfig();
    private static Path configPath;

    private ReticleClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(ReticleConfig.FILE_NAME);
        config = ReticleConfig.load(configPath);
    }

    public static ReticleConfig config() {
        return config;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
