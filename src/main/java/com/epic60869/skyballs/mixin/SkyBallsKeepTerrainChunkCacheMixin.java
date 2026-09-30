// Ported from Skysoft (https://github.com/Akinsoft/Skysoft), mixin/KeepTerrainLoadedClientChunkCacheMixin.java, LGPL-3.0.
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.KeepTerrainLoaded;
import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep Terrain Loaded: the client's chunk storage reaches your render distance, not just the server's. When the storage
 * shrinks, vanilla forgets the chunks that no longer fit without unloading them, so they're dropped properly first
 * (otherwise Sodium keeps drawing them after their buffers are gone and the game crashes).
 */
@Mixin(ClientChunkCache.class)
public abstract class SkyBallsKeepTerrainChunkCacheMixin {
    @ModifyVariable(method = "updateViewRadius", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int skyballs$expandTerrainCache(int viewDistance) {
        int distance = viewDistance;
        try {
            distance = KeepTerrainLoaded.storageViewDistance(viewDistance);
        } catch (Exception ignored) {
        }
        try {
            KeepTerrainLoaded.beforeResize((ClientChunkCache) (Object) this, distance);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Keep Terrain Loaded: " + e);
        }
        return distance;
    }

    @Inject(method = "updateViewCenter", at = @At("TAIL"))
    private void skyballs$trackCenter(int x, int z, CallbackInfo ci) {
        KeepTerrainLoaded.onViewCenter((ClientChunkCache) (Object) this, x, z);
    }
}
