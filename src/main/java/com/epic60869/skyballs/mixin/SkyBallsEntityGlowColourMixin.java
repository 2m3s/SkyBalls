package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.core.EntityGlow;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The outline of an entity highlighted by a SkyBalls feature is drawn in that feature's colour. */
@Mixin(Entity.class)
public abstract class SkyBallsEntityGlowColourMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void skyballs$entityGlowColour(CallbackInfoReturnable<Integer> cir) {
        int colour = EntityGlow.colour((Entity) (Object) this);
        if (colour != -1) cir.setReturnValue(colour);
    }
}
