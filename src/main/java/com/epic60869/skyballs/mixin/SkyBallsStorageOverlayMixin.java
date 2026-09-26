package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.StorageOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Storage Overlay (Case Opening and Terminals > Hide Menu use SkyBallsHideMenuMixin): while it shows, the real menu (items, highlights, tooltips) isn't drawn underneath it. */
@Mixin(AbstractContainerScreen.class)
public abstract class SkyBallsStorageOverlayMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void skyballs$hideMenuUnderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var screen = (AbstractContainerScreen<?>) (Object) this;
        if (StorageOverlay.applies(screen)) ci.cancel();
    }
}
