// Ported from Firmament (https://github.com/FirmamentMC/Firmament), mixins/customgui/PatchHandledScreen.java, plus the
// input hooks Firmament adds with its HandledScreenRiser.
// SPDX-FileCopyrightText: Linnea Gräf <nea@nea.moe>, Firmament Contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.storage.CustomGui;
import com.epic60869.skyballs.features.misc.storage.StorageOverlay;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class SkyBallsCustomGuiContainerMixin<T extends AbstractContainerMenu> implements CustomGui.Holder {
    @Shadow @Final protected T menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;

    @Unique private boolean skyballs$hasRememberedSlots;

    @Unique
    private CustomGui skyballs$gui() {
        return CustomGui.get((Screen) (Object) this);
    }

    @Override
    public void skyballs$fixSize() {
        CustomGui override = skyballs$gui();
        Screen self = (Screen) (Object) this;
        int width = override != null ? override.getBounds().getFirst().width() : imageWidth;
        int height = override != null ? override.getBounds().getFirst().height() : imageHeight;
        this.leftPos = (self.width - width) / 2;
        this.topPos = (self.height - height) / 2;
    }

    /** Where the storage overlay puts the screen (its slots are placed relative to this). */
    @Override
    public void skyballs$setPos(int x, int y) {
        this.leftPos = x;
        this.topPos = y;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void skyballs$onInit(CallbackInfo ci) {
        skyballs$fixSize();
        CustomGui override = skyballs$gui();
        if (override != null) override.onInit();
    }

    @WrapOperation(method = "extractSlots", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V"))
    private void skyballs$aroundSlotRender(AbstractContainerScreen<?> instance, GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, Operation<Void> original) {
        CustomGui override = skyballs$gui();
        if (override != null) override.beforeSlotRender(graphics, slot);
        original.call(instance, graphics, slot, mouseX, mouseY);
        if (override != null) override.afterSlotRender(graphics, slot);
    }

    @WrapWithCondition(method = "extractContents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private boolean skyballs$hideLabels(AbstractContainerScreen<T> instance, GuiGraphicsExtractor graphics, int xm, int ym) {
        return skyballs$gui() == null;
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void skyballs$isClickOutsideBounds(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null) cir.setReturnValue(override.isClickOutsideBounds(mx, my));
    }

    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
    private void skyballs$isPointWithinBounds(int left, int top, int w, int h, double xm, double ym, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null) cir.setReturnValue(override.isPointWithinBounds(left + this.leftPos, top + this.topPos, w, h, xm, ym));
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void skyballs$isPointOverSlot(Slot slot, double xm, double ym, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null) cir.setReturnValue(override.isPointOverSlot(slot, this.leftPos, this.topPos, xm, ym));
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void skyballs$moveSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        CustomGui override = skyballs$gui();
        if (override != null) {
            for (Slot slot : menu.slots) {
                if (!skyballs$hasRememberedSlots) ((CustomGui.RememberingSlot) slot).skyballs$rememberCoords();
                override.moveSlot(slot);
            }
            skyballs$hasRememberedSlots = true;
        } else if (skyballs$hasRememberedSlots) {
            for (Slot slot : menu.slots) ((CustomGui.RememberingSlot) slot).skyballs$restoreCoords();
            skyballs$hasRememberedSlots = false;
        }
    }

    // ---------------------------------------------------------------- input (Firmament adds these with a class riser)

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void skyballs$mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null && override.mouseClick(click, doubled)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void skyballs$mouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null && override.mouseReleased(click)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void skyballs$mouseDragged(MouseButtonEvent click, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null && override.mouseDragged(click, dx, dy)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void skyballs$mouseScrolled(double mouseX, double mouseY, double h, double v, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null && override.mouseScrolled(mouseX, mouseY, h, v)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void skyballs$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        CustomGui override = skyballs$gui();
        if (override != null && override.keyPressed(event)) cir.setReturnValue(true);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void skyballs$slotClicked(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        if (slot != null) StorageOverlay.onSlotClick(slot);
    }
}
