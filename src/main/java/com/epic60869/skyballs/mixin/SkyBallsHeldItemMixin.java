package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.HeldItemModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Misc > Held Item Model: transforms the first-person item. The collector copies the pose, so popping at the end is safe. */
@Mixin(ItemInHandRenderer.class)
public abstract class SkyBallsHeldItemMixin {
    /**
     * Misc > Held Item Model > Swing Rotation: scales every turn in the swing (vanilla turns 45 + up to -20 degrees
     * around Y, up to -20 around Z, up to -80 around X, then -45 back around Y; at rest they cancel out, so scaling
     * them all keeps the resting pose and only shrinks the swing).
     */
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "applyItemArmAttackTransform", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;"))
    private org.joml.Quaternionf skyballs$swingRotation(org.joml.Quaternionf rotation) {
        float scale = HeldItemModel.swingRotation();
        if (scale == 1f) return rotation;
        // Same axis, angle times the setting.
        org.joml.AxisAngle4f axisAngle = new org.joml.AxisAngle4f(rotation);
        axisAngle.angle *= scale;
        return new org.joml.Quaternionf(axisAngle);
    }

    /** Misc > Held Item Model > No Swing Animation: the first-person hand and item never swing. */
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackAnim(F)F"))
    private float skyballs$noSwing(float attack) {
        return HeldItemModel.noSwing() ? 0f : attack;
    }

    private boolean skyballs$pushed;

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void skyballs$transformHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        skyballs$pushed = false;
        if (!context.firstPerson() || stack.isEmpty()) return;
        HeldItemModel.Transform transform = HeldItemModel.transform(stack);
        if (transform == null) return;
        pose.pushPose();
        HeldItemModel.apply(pose, transform, context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        skyballs$pushed = true;
    }

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void skyballs$restoreHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (skyballs$pushed) pose.popPose();
        skyballs$pushed = false;
    }
}
