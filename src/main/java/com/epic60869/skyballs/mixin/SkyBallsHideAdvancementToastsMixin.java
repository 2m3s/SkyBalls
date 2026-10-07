package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Always on: Minecraft's advancement and "new recipes unlocked" popups in the top corner are never shown. */
@Mixin(ToastManager.class)
public abstract class SkyBallsHideAdvancementToastsMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void skyballs$hideAdvancementToasts(Toast toast, CallbackInfo ci) {
        if (toast instanceof AdvancementToast || toast instanceof RecipeToast) ci.cancel();
    }
}
