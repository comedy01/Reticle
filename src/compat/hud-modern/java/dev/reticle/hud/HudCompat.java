package dev.reticle.hud;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

final class HudCompat {
    private HudCompat() {
    }

    static Camera mainCamera(Minecraft mc) {
        return mc.gameRenderer.mainCamera();
    }

    static boolean isGuiHidden(Minecraft mc) {
        return mc.gui.hud.isHidden();
    }
}
