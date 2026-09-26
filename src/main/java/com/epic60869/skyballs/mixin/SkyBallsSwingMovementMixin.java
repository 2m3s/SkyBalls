package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.HeldItemModel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Misc > Held Item Model > Swing X / Y / Z: scales how far the item moves during a swing (0 keeps only the rotation).
 * High priority so this wrapper is applied last and runs outermost: other mods' animation wrappers on the same call
 * (NoammAddons, Odin, ...) that call translate themselves instead of the original would otherwise skip it.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 2000)
public abstract class SkyBallsSwingMovementMixin {
    @WrapOperation(method = "swingArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private void skyballs$swingMovement(PoseStack pose, float x, float y, float z, Operation<Void> original) {
        float[] scale = HeldItemModel.swingScale();
        original.call(pose, x * scale[0], y * scale[1], z * scale[2]);
    }
}
