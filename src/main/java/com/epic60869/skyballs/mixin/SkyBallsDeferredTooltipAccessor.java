package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Clears the tooltip waiting to be drawn this frame (the chat image preview replaces a link's hover text). */
@Mixin(GuiGraphicsExtractor.class)
public interface SkyBallsDeferredTooltipAccessor {
    @Accessor("deferredTooltip")
    void skyballs$setDeferredTooltip(Runnable tooltip);
}
