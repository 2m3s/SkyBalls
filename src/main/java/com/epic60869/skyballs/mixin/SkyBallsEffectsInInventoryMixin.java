// Ported from Skysoft (https://github.com/Akinsoft/Skysoft), mixin/EffectsInInventoryMixin.java, LGPL-3.0.
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsConfig;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Misc > Hide Status Effects: no potion effect list beside inventories. */
@Mixin(EffectsInInventory.class)
public abstract class SkyBallsEffectsInInventoryMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void skyballs$hideStatusEffects(CallbackInfo ci) {
        SkyBallsConfig c = SkyBallsConfig.current();
        if (c != null && c.misc.hideStatusEffects) ci.cancel();
    }
}
