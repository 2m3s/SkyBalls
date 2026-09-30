package com.epic60869.skyballs.features.sbc;

import com.epic60869.skyballs.SkyBallsGlobalChat;
import com.epic60869.skyballs.SkyBallsLogin;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Sending to the mod server: plain packets, and packets that need the Mojang login. */
public final class SbcNet {
    public static final String OFFLINE = "SBC offline: can't reach the SkyBalls server right now. It reconnects by itself.";

    private SbcNet() {}

    public static boolean online() {
        return SkyBallsGlobalChat.currentConnection() != null;
    }

    /** Sends now if connected; otherwise says "SBC offline" and returns false. */
    public static boolean send(JsonObject packet) {
        if (SkyBallsGlobalChat.send(packet)) return true;
        Sbc.error(OFFLINE);
        return false;
    }

    /** Sends without telling the player when offline (for background requests). */
    public static boolean sendQuietly(JsonObject packet) {
        return SkyBallsGlobalChat.send(packet);
    }

    /**
     * Sends a packet that needs the login (friends, ignore list, settings, cosmetics...), logging in first if
     * needed. Returns false (after saying why) when the server can't be reached.
     */
    public static boolean sendAuthed(JsonObject packet) {
        if (!online()) {
            SkyBallsGlobalChat.ensureConnected();
            Sbc.error(OFFLINE);
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            boolean wasLoggedIn = SkyBallsLogin.loggedIn();
            SkyBallsLogin.whenLoggedIn(() -> SkyBallsGlobalChat.send(packet));
            if (wasLoggedIn) return;
            CompletableFuture.delayedExecutor(8, TimeUnit.SECONDS).execute(() -> mc.execute(() -> {
                if (!SkyBallsLogin.loggedIn() && !SkyBallsLogin.problem().isEmpty()) {
                    Sbc.error("Couldn't log in to the SkyBalls server: " + SkyBallsLogin.problem());
                }
            }));
        });
        return true;
    }
}
