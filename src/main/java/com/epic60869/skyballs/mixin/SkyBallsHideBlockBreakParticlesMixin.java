package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Misc > Random > Hide Block Break Particles, like SkyHanni's: no burst of block bits when a block breaks (yours or
 * anyone's), and no chips flying off the block you're mining.
 */
@Mixin(ClientLevel.class)
public abstract class SkyBallsHideBlockBreakParticlesMixin {
    private static boolean skyballs$hide() {
        SkyBallsConfig config = SkyBallsConfig.current();
        return config != null && config.misc.random.hideBlockBreakParticles;
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void skyballs$hideDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (skyballs$hide()) ci.cancel();
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void skyballs$hideBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (skyballs$hide()) ci.cancel();
    }
}
