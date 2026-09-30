package com.epic60869.skyballs.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The server's view distance, for Keep Terrain Loaded. */
@Mixin(ClientPacketListener.class)
public interface SkyBallsClientPacketListenerAccessor {
    @Accessor("serverChunkRadius")
    int skyballs$serverChunkRadius();
}
