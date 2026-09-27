// Ported from Skysoft (https://github.com/Akinsoft/Skysoft), src/main/java/com/skysoft/mixin/LivingEntitySwingMixin.java.
// SPDX-License-Identifier: LGPL-3.0-only
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.helditem.HeldItemSwing;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Misc > Held Item > Swing: how long your own swing takes. High priority so it applies after other mods set it. */
@Mixin(value = LivingEntity.class, priority = 2000)
public abstract class SkyBallsSwingSpeedMixin {
    @ModifyReturnValue(method = "getCurrentSwingDuration", at = @At("RETURN"))
    private int skyballs$swingDuration(int original) {
        return HeldItemSwing.duration((LivingEntity) (Object) this, original);
    }
}
