package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Storage overlay: its search box and button are added to the storage menu so they get keyboard input. */
@Mixin(Screen.class)
public interface SkyBallsScreenWidgetsInvoker {
    @Invoker("addWidget")
    <T extends GuiEventListener & NarratableEntry> T skyballs$addWidget(T widget);
}
