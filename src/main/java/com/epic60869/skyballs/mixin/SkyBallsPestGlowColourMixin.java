package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.garden.PestHighlight;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pest highlight: the outline is drawn in the chosen colour. */
@Mixin(Entity.class)
public abstract class SkyBallsPestGlowColourMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void skyballs$pestColour(CallbackInfoReturnable<Integer> cir) {
        if (PestHighlight.isPest((Entity) (Object) this)) cir.setReturnValue(PestHighlight.colour());
    }
}
