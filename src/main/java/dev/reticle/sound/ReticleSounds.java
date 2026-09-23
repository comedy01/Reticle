package dev.reticle.sound;

import dev.reticle.client.ReticleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ReticleSounds {
    public static final SoundEvent PULL_UP = create("pull_up");
    public static final SoundEvent LOCK_ON = create("lock_on");
    public static final SoundEvent HUD_ON = create("hud_on");

    private ReticleSounds() {
    }

    private static SoundEvent create(String path) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ReticleClient.MOD_ID, path));
    }

    public static void play(SoundEvent sound, float pitch, float volume) {
        playTracked(sound, pitch, volume);
    }

    public static SoundInstance playTracked(SoundEvent sound, float pitch, float volume) {
        if (!ReticleClient.config().soundsEnabled()) {
            return null;
        }
        SoundInstance instance = SimpleSoundInstance.forUI(sound, pitch, volume);
        Minecraft.getInstance().getSoundManager().play(instance);
        return instance;
    }

    public static boolean isActive(SoundInstance instance) {
        return instance != null && Minecraft.getInstance().getSoundManager().isActive(instance);
    }

    public static void stop(SoundInstance instance) {
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }
}
