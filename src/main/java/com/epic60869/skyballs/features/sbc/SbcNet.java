package com.epic60869.skyballs.features.sbc;

import com.epic60869.skyballs.SkyBallsGlobalChat;
import com.epic60869.skyballs.SkyBallsLogin;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sending to the mod server: plain packets, packets that need the Mojang login, and casino packets, which the
 * server only accepts 5 a second (they're queued and sent as fast as that allows).
 */
public final class SbcNet {
    public static final String OFFLINE = "SBC offline: can't reach the SkyBalls server right now. It reconnects by itself.";
    private static final int CASINO_PER_SECOND = 5;

    private static final Deque<JsonObject> CASINO_QUEUE = new ArrayDeque<>();
    private static final Deque<Long> CASINO_SENT = new ArrayDeque<>();

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
     * Sends a packet that needs the login (friends, ignore list, casino, settings, cosmetics...), logging in first if
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

    /** Queues a casino packet (logged in); the queue is sent at most 5 packets a second. */
    public static void sendCasino(JsonObject packet) {
        synchronized (CASINO_QUEUE) {
            if (CASINO_QUEUE.size() >= 20) CASINO_QUEUE.pollFirst();
            CASINO_QUEUE.addLast(packet);
        }
    }

    static void tick() {
        synchronized (CASINO_QUEUE) {
            if (CASINO_QUEUE.isEmpty()) return;
            if (!online()) return;
            if (!SkyBallsLogin.loggedIn()) {
                SkyBallsLogin.whenLoggedIn(null);
                return;
            }
            long now = System.currentTimeMillis();
            while (!CASINO_SENT.isEmpty() && now - CASINO_SENT.peekFirst() >= 1000L) CASINO_SENT.pollFirst();
            while (!CASINO_QUEUE.isEmpty() && CASINO_SENT.size() < CASINO_PER_SECOND) {
                if (!SkyBallsGlobalChat.send(CASINO_QUEUE.peekFirst())) return;
                CASINO_QUEUE.pollFirst();
                CASINO_SENT.addLast(now);
            }
        }
    }

    static void clearQueues() {
        synchronized (CASINO_QUEUE) {
            CASINO_QUEUE.clear();
        }
    }
}
