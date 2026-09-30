// Ported from Skysoft (https://github.com/Akinsoft/Skysoft), mixin/KeepTerrainLoadedClientChunkCacheMixin.java, LGPL-3.0.
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.KeepTerrainLoaded;
import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keep Terrain Loaded: the client's chunk storage reaches your render distance, not just the server's. */
@Mixin(ClientChunkCache.class)
public abstract class SkyBallsKeepTerrainChunkCacheMixin {
    @ModifyVariable(method = "updateViewRadius", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int skyballs$expandTerrainCache(int viewDistance) {
        try {
            return KeepTerrainLoaded.storageViewDistance(viewDistance);
        } catch (Exception e) {
            return viewDistance;
        }
    }
}
