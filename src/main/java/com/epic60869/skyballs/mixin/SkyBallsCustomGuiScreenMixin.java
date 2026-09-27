// Ported from Firmament (https://github.com/FirmamentMC/Firmament), mixins/customgui/PatchGenericScreen.java.
// SPDX-FileCopyrightText: Linnea Gräf <nea@nea.moe>, Firmament Contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.storage.CustomGui;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom GUIs (the storage overlay): drawn first, the screen's own background skipped, and asked before closing. */
@Mixin(Screen.class)
public abstract class SkyBallsCustomGuiScreenMixin implements CustomGui.Holder {
    @Shadow @Final protected Minecraft minecraft;

    @Shadow public abstract boolean isInGameUi();

    @Shadow public abstract void extractTransparentBackground(GuiGraphicsExtractor graphics);

    @Shadow protected abstract void extractPanorama(GuiGraphicsExtractor graphics, float a);

    @Shadow protected abstract void extractBlurredBackground(GuiGraphicsExtractor graphics);

    @Shadow protected abstract void extractMenuBackground(GuiGraphicsExtractor graphics);

    @Unique private CustomGui skyballs$customGui;

    @Override
    public CustomGui skyballs$getCustomGui() {
        return skyballs$customGui;
    }

    @Override
    public void skyballs$setCustomGui(CustomGui gui) {
        this.skyballs$customGui = gui;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void skyballs$renderCustomGui(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (skyballs$customGui != null) skyballs$customGui.render(graphics, a, mouseX, mouseY);
    }

    @WrapWithCondition(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private boolean skyballs$replaceBackground(Screen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (skyballs$customGui != null) {
            skyballs$extractBackgroundCopy(graphics, a);
            return false;
        }
        return true;
    }

    /** Copy of Screen#extractBackground, so a container's own background (the chest texture) isn't drawn. */
    @Unique
    private void skyballs$extractBackgroundCopy(GuiGraphicsExtractor graphics, float a) {
        if (this.isInGameUi()) {
            this.extractTransparentBackground(graphics);
        } else {
            if (this.minecraft.level == null) this.extractPanorama(graphics, a);
            this.extractBlurredBackground(graphics);
            this.extractMenuBackground(graphics);
        }
        this.minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void skyballs$onVoluntaryExit(CallbackInfo ci) {
        if (skyballs$customGui != null && !skyballs$customGui.onVoluntaryExit()) ci.cancel();
    }
}
