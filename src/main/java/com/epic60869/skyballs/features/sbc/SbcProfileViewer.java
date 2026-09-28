package com.epic60869.skyballs.features.sbc;

import com.epic60869.skyballs.custom.util.Compat;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * /sb pv &lt;player&gt; [profile]: asks the SkyBalls server for a player's SkyBlock profile (it does the Hypixel API
 * calls with its own key, at most 6 a minute) and shows it in {@link SbcPvScreen}.
 */
public final class SbcProfileViewer {
    private static final int PER_MINUTE = 6;
    private static final Deque<Long> SENT = new ArrayDeque<>();
    private static int nextRequest = 1;
    /** The viewer waiting for an answer (it may not be on screen yet: screens open on the next tick). */
    private static SbcPvScreen current;

    private SbcProfileViewer() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root)
                    .then(ClientCommands.literal("pv")
                        .executes(c -> SbcCommands.run(() -> open(Minecraft.getInstance().getUser().getName(), null)))
                        .then(ClientCommands.argument("player", StringArgumentType.word())
                            .suggests((c, b) -> SharedSuggestionProvider.suggest(SbcSocial.onlineNames(), b))
                            .executes(c -> SbcCommands.run(() -> open(StringArgumentType.getString(c, "player"), null)))
                            .then(ClientCommands.argument("profile", StringArgumentType.word())
                                .executes(c -> SbcCommands.run(() -> open(StringArgumentType.getString(c, "player"),
                                    StringArgumentType.getString(c, "profile"))))))));
            }
        });
    }

    /** Opens the viewer for {@code username} ({@code profile} null = their selected profile). */
    public static void open(String username, String profile) {
        if (!Flags.check("pv")) return;
        SbcPvScreen screen = new SbcPvScreen(username);
        current = screen;
        Compat.queueOpenScreen(screen);
        request(screen, username, profile);
    }

    /** Asks for a profile for the open screen; false (with a message on the screen) when it can't right now. */
    static boolean request(SbcPvScreen screen, String username, String profile) {
        if (!SbcNet.online()) {
            screen.fail(SbcNet.OFFLINE);
            return false;
        }
        long now = System.currentTimeMillis();
        while (!SENT.isEmpty() && now - SENT.peekFirst() > 60_000L) SENT.pollFirst();
        if (SENT.size() >= PER_MINUTE) {
            screen.fail("Slow down: you can look up " + PER_MINUTE + " profiles a minute. Try again in "
                + Sbc.duration(60_000L - (now - SENT.peekFirst())) + ".");
            return false;
        }
        SENT.addLast(now);
        String id = "pv" + nextRequest++;
        screen.waitFor(id);
        JsonObject p = Sbc.packet("pv");
        p.addProperty("username", username);
        if (profile != null && !profile.isBlank()) p.addProperty("profile", profile);
        p.addProperty("requestId", id);
        SbcNet.sendQuietly(p);
        return true;
    }

    static void handle(JsonObject packet) {
        SbcPvScreen screen = current;
        if (screen != null) screen.receive(packet);
    }
}
