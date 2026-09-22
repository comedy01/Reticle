package dev.reticle.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.reticle.client.gui.ReticleSettingsScreen;
import net.minecraft.client.Minecraft;

public final class ReticleModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ReticleSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
