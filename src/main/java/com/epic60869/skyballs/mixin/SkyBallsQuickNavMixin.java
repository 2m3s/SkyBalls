// Ported from Skyblocker (https://github.com/SkyblockerMod/Skyblocker), mixins/QuickNavScreenMixin.java and QuickNavMixin.java, LGPL-3.0.
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.InventoryButtons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory Buttons: the unselected tabs are drawn during the background, before the menu's own background covers
 * their lower edge, and the selected tab after it, in front, like the creative inventory's tabs.
 */
@Mixin(Screen.class)
public abstract class SkyBallsQuickNavMixin {
    @Inject(method = "extractBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V"))
    private void skyballs$unselectedTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        InventoryButtons.renderUnselected((Screen) (Object) this, graphics, mouseX, mouseY, a);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.AFTER))
    private void skyballs$selectedTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        InventoryButtons.renderSelected((Screen) (Object) this, graphics, mouseX, mouseY, a);
    }
}
