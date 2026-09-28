package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.garden.PestHighlight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pest highlight: pests in the Garden get the glowing outline. */
@Mixin(Minecraft.class)
public abstract class SkyBallsPestGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void skyballs$pestGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (PestHighlight.isPest(entity)) cir.setReturnValue(true);
    }
}
