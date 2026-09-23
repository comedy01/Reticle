package dev.reticle.fabric;

import dev.reticle.client.ReticleClient;
import dev.reticle.hud.ReticleHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public final class ReticleFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ReticleClient.init(FabricLoader.getInstance().getConfigDir());
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(ReticleClient.MOD_ID, "flight_hud"),
                ReticleHudRenderer.INSTANCE);
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (guiGraphics, deltaTracker) -> {
            if (ReticleHudRenderer.isActive()) {
                return;
            }
            original.extractRenderState(guiGraphics, deltaTracker);
        });
    }
}
