package com.epic60869.skyballs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * /sb casino: blackjack against the SBC chat server, which keeps everyone's balance (everyone starts with $100) and
 * deals the cards, so balances can't be edited in the mod. The mod only sends bets and moves and shows what the
 * server answers.
 *
 * Before playing, the connection proves which account it is ({@link SkyBallsLogin}).
 *
 * Packets (JSON over wss://tastyfish.org/mod-api/tf-chat):
 * <pre>
 * mod -> server: casinoAuth {serverId, username}, casinoState {}, casinoBet {amount},
 *                casinoAction {action: hit|stand|double}, casinoLeaderboard {}
 * server -> mod: casinoState {balance, hand, refillAt}, casinoLeaderboard {entries: [{username, minecraftUuid, balance}], you},
 *                casinoError {message}
 * </pre>
 */
public final class SkyBallsCasino {
    /** A card like "AS", "10H" or "QD"; "??" is the dealer's face-down card. */
    public record Hand(List<String> player, List<String> dealer, int playerTotal, int dealerTotal, long bet,
                       String state, long payout, boolean canDouble, String message) {
        public boolean playing() {
            return "playing".equals(state);
        }
    }

    public record Entry(String username, String uuid, long balance) {}

    // Last state the server sent.
    static long balance = -1;
    static Hand hand;
    static long refillAt;
    static List<Entry> leaderboard = List.of();
    static int yourRank;
    static String error = "";
    static long errorAt;
    static boolean waiting;

    /** The connection the state was asked for on (a reconnect asks again). */
    private static Object requestedOn;

    private SkyBallsCasino() {}

    /** Called while the table is open: logs in when needed, then asks for the state and leaderboard once. */
    static void ensureReady() {
        SkyBallsLogin.whenLoggedIn(() -> {
            Object connection = SkyBallsGlobalChat.currentConnection();
            if (connection == null || connection == requestedOn) return;
            requestedOn = connection;
            waiting = true;
            SkyBallsGlobalChat.send(packet("casinoState"));
            SkyBallsGlobalChat.send(packet("casinoLeaderboard"));
        });
    }

    static boolean loggedIn() {
        return SkyBallsLogin.loggedIn();
    }

    static String status() {
        if (!SkyBallsLogin.problem().isEmpty()) return SkyBallsLogin.problem();
        if (SkyBallsGlobalChat.currentConnection() == null) return "Connecting to SBC...";
        if (!loggedIn() || balance < 0) return "Logging in...";
        return "";
    }

    static void bet(long amount) {
        JsonObject p = packet("casinoBet");
        p.addProperty("amount", amount);
        sendWaiting(p);
    }

    static void action(String action) {
        JsonObject p = packet("casinoAction");
        p.addProperty("action", action);
        sendWaiting(p);
    }

    static void refreshLeaderboard() {
        if (loggedIn()) SkyBallsGlobalChat.send(packet("casinoLeaderboard"));
    }

    private static void sendWaiting(JsonObject p) {
        if (!loggedIn()) return;
        if (SkyBallsGlobalChat.send(p)) waiting = true;
    }

    private static JsonObject packet(String type) {
        JsonObject p = new JsonObject();
        p.addProperty("type", type);
        return p;
    }

    /** A casino packet from the server (on the game thread). */
    static void handle(String type, JsonObject packet) {
        switch (type) {
            case "casinoState" -> {
                SkyBallsLogin.confirmed(true, "");
                waiting = false;
                balance = getLong(packet, "balance", 0);
                refillAt = getLong(packet, "refillAt", 0);
                Hand previous = hand;
                hand = packet.has("hand") && packet.get("hand").isJsonObject() ? parseHand(packet.getAsJsonObject("hand")) : null;
                // A hand just finished: the balances on the board changed.
                if (previous != null && previous.playing() && (hand == null || !hand.playing())) refreshLeaderboard();
            }
            case "casinoLeaderboard" -> {
                List<Entry> entries = new ArrayList<>();
                if (packet.has("entries")) {
                    for (JsonElement e : packet.getAsJsonArray("entries")) {
                        JsonObject o = e.getAsJsonObject();
                        entries.add(new Entry(getString(o, "username"), getString(o, "minecraftUuid"), getLong(o, "balance", 0)));
                    }
                }
                leaderboard = entries;
                yourRank = packet.has("you") && packet.get("you").isJsonObject()
                    ? (int) getLong(packet.getAsJsonObject("you"), "rank", 0) : 0;
            }
            case "casinoError" -> {
                waiting = false;
                error = getString(packet, "message");
                errorAt = System.currentTimeMillis();
                if ("authFailed".equals(getString(packet, "code"))) SkyBallsLogin.confirmed(false, error);
                // The login didn't take (or the server restarted): log in again.
                if ("notLoggedIn".equals(getString(packet, "code"))) {
                    SkyBallsLogin.forget();
                    requestedOn = null;
                }
            }
            default -> {}
        }
    }

    private static Hand parseHand(JsonObject o) {
        return new Hand(cards(o, "player"), cards(o, "dealer"), (int) getLong(o, "playerTotal", 0),
            (int) getLong(o, "dealerTotal", 0), getLong(o, "bet", 0), getString(o, "state"), getLong(o, "payout", 0),
            o.has("canDouble") && o.get("canDouble").getAsBoolean(), getString(o, "message"));
    }

    private static List<String> cards(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray array = o.getAsJsonArray(key);
            for (JsonElement e : array) out.add(e.getAsString());
        }
        return out;
    }

    private static long getLong(JsonObject o, String key, long fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : fallback;
    }

    private static String getString(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
}
