package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsMouseLock;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Farming > Mouse Lock: skips turning the camera while locked. The mouse movement is still used up, so the camera
 * doesn't jump when the lock ends. Wrapping the turn (instead of replacing the sensitivity) works alongside other
 * mods that change mouse handling.
 */
@Mixin(MouseHandler.class)
public class SkyBallsMouseHandlerMixin {
    @WrapWithCondition(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private boolean skyballs$mouseLock(LocalPlayer player, double yaw, double pitch) {
        return !SkyBallsMouseLock.isLocked();
    }
}
