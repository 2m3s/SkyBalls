package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Misc > Hypixel Button: a "Hypixel" button on the title screen, next to Multiplayer (which gets half as wide), that
 * connects straight to play.hypixel.net. Placed like SkyOcean's Quick Join button.
 */
public final class HypixelButton {
    private static final String ADDRESS = "play.hypixel.net";

    private HypixelButton() {}

    private static boolean enabled() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c != null && c.misc.hypixelButton;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof TitleScreen) || !enabled()) return;
            Button multiplayer = null;
            for (AbstractWidget widget : Screens.getWidgets(screen)) {
                if (widget instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("menu.multiplayer")) {
                    multiplayer = button;
                    break;
                }
            }
            if (multiplayer == null) return;
            Button multiplayerButton = multiplayer;
            int half = (multiplayerButton.getWidth() - 4) / 2;
            multiplayerButton.setWidth(half);
            Button hypixel = Button.builder(Component.literal("Hypixel"), b -> {
                    ServerData data = new ServerData("Hypixel", ADDRESS, ServerData.Type.OTHER);
                    // Not in the server list, so don't ask about the resource pack every time.
                    data.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                    ConnectScreen.startConnecting(screen, client, ServerAddress.parseString(ADDRESS), data, false, null);
                })
                .bounds(multiplayerButton.getX() + half + 4, multiplayerButton.getY(), half, multiplayerButton.getHeight())
                .tooltip(Tooltip.create(Component.literal("Join " + ADDRESS + " (SkyBalls)")))
                .build();
            Screens.getWidgets(screen).add(hypixel);
            // The title screen can move its buttons (e.g. while it fades in), so keep this one next to Multiplayer.
            ScreenEvents.afterTick(screen).register(s -> {
                hypixel.setX(multiplayerButton.getX() + multiplayerButton.getWidth() + 4);
                hypixel.setY(multiplayerButton.getY());
            });
        });
    }
}
