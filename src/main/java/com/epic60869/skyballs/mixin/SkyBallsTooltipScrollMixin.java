package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.ScrollableTooltips;
import com.epic60869.skyballs.features.misc.storage.StorageOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mouse wheel over a screen. Misc > Tooltip Scroll: the wheel pans a visible tooltip (ScrollableTooltips decides
 * whether before or after the screen). The storage overlay gets the wheel directly, ahead of other mods' scroll
 * hooks: an item list panel under it (SkyBlock Item List) would otherwise cancel the scroll.
 */
@Mixin(MouseHandler.class)
public class SkyBallsTooltipScrollMixin {
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean skyballs$scrollTooltip(Screen screen, double mouseX, double mouseY, double horizontal, double vertical, Operation<Boolean> original) {
        return ScrollableTooltips.onMouseScroll(horizontal, vertical, () -> {
            if (StorageOverlay.isOverlayScreen(screen) && screen.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
            return original.call(screen, mouseX, mouseY, horizontal, vertical);
        });
    }
}
