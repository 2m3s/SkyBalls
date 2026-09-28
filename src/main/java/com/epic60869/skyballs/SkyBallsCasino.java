package com.epic60869.skyballs;

import com.epic60869.skyballs.features.sbc.Flags;
import com.epic60869.skyballs.features.sbc.Sbc;
import com.epic60869.skyballs.features.sbc.SbcNet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /sb casino: blackjack, coinflip, dice, roulette, slots and higher/lower against the SBC chat server, which keeps
 * everyone's balance (everyone starts with $100), deals the cards and rolls the dice, so nothing can be edited in the
 * mod. The mod only sends bets and moves and shows (and animates) what the server answers.
 *
 * Before playing, the connection proves which account it is ({@link SkyBallsLogin}). The server takes at most 5
 * casino packets a second, so they go through {@link SbcNet#sendCasino}.
 *
 * Packets (JSON over wss://tastyfish.org/mod-api/tf-chat):
 * <pre>
 * mod -> server: casinoState {}, casinoBet {amount}, casinoAction {action: hit|stand|double},
 *                casinoCoinflip {amount, side}, casinoDice {amount, chance, direction}, casinoRoulette {amount, bet},
 *                casinoSlots {amount}, casinoHiLo {action, amount?}, casinoDaily {}, casinoStats {},
 *                casinoLeaderboard {by?}
 * server -> mod: casinoState {balance, hand, refillAt, daily, hilo, games}, casinoResult {game, bet, payout, outcome,
 *                detail, balance}, casinoHiLo {balance, game}, casinoDaily {reward, streak, nextAt, balance},
 *                casinoStats {...}, casinoLeaderboard {by, entries: [{username, minecraftUuid, balance, value}], you},
 *                casinoGift {amount, balance, by, reason, message}, casinoError {code, message}
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

    public record Entry(String username, String uuid, long balance, long value) {}

    /** The last coinflip/dice/roulette/slots result, and when it arrived (for the animation). */
    public record Result(String game, long bet, long payout, String outcome, JsonObject detail, long at) {
        public boolean won() {
            return "won".equals(outcome);
        }
    }

    /** A higher/lower game. {@code card} is 1 (ace) to 13 (king). */
    public record HiLo(long bet, int card, double multiplier, int rounds, List<Integer> history, String state, long payout,
                       long cashout, double higherMultiplier, double lowerMultiplier, String message, long at) {
        public boolean playing() {
            return "playing".equals(state);
        }
    }

    public record Daily(boolean available, int streak, long nextAt, long reward, long tomorrow) {}

    // Last state the server sent.
    static long balance = -1;
    static Hand hand;
    static long refillAt;
    static List<Entry> leaderboard = List.of();
    static int yourRank;
    static String error = "";
    static long errorAt;
    static boolean waiting;

    static Result lastResult;
    static HiLo hilo;
    static Daily daily;
    static List<String> games = List.of();
    static JsonObject stats;
    /** Leaderboards by what they rank (balance, biggestWin, wagered, streak). */
    static final Map<String, List<Entry>> BOARDS = new HashMap<>();
    static final Map<String, Long> BOARD_YOU = new HashMap<>();
    /** The daily reward we've already reminded about (its nextAt). */
    private static long remindedFor = -1;

    /** The connection the state was asked for on (a reconnect asks again). */
    private static Object requestedOn;

    private SkyBallsCasino() {}

    /** Called while the casino is open: logs in when needed, then asks for the state and leaderboard once. */
    static void ensureReady() {
        SkyBallsLogin.whenLoggedIn(() -> {
            Object connection = SkyBallsGlobalChat.currentConnection();
            if (connection == null || connection == requestedOn) return;
            requestedOn = connection;
            waiting = true;
            SbcNet.sendCasino(packet("casinoState"));
            SbcNet.sendCasino(packet("casinoLeaderboard"));
        });
    }

    static boolean loggedIn() {
        return SkyBallsLogin.loggedIn();
    }

    static String status() {
        if (!Flags.isEnabled("casino")) return Flags.message("casino");
        if (!SkyBallsLogin.problem().isEmpty()) return SkyBallsLogin.problem();
        if (SkyBallsGlobalChat.currentConnection() == null) return "SBC offline. Reconnecting...";
        if (!loggedIn() || balance < 0) return "Logging in...";
        return "";
    }

    /** Whether the server offers this game (flags, and the state's game list when it sends one). */
    static boolean gameEnabled(String game) {
        if (!Flags.isEnabled("casino." + game)) return false;
        return games.isEmpty() || games.contains(game);
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

    static void coinflip(long amount, String side) {
        JsonObject p = packet("casinoCoinflip");
        p.addProperty("amount", amount);
        p.addProperty("side", side);
        sendWaiting(p);
    }

    static void dice(long amount, int chance, String direction) {
        JsonObject p = packet("casinoDice");
        p.addProperty("amount", amount);
        p.addProperty("chance", chance);
        p.addProperty("direction", direction);
        sendWaiting(p);
    }

    static void roulette(long amount, String bet) {
        JsonObject p = packet("casinoRoulette");
        p.addProperty("amount", amount);
        p.addProperty("bet", bet);
        sendWaiting(p);
    }

    static void slots(long amount) {
        JsonObject p = packet("casinoSlots");
        p.addProperty("amount", amount);
        sendWaiting(p);
    }

    static void hilo(String action, long amount) {
        JsonObject p = packet("casinoHiLo");
        p.addProperty("action", action);
        if (amount > 0) p.addProperty("amount", amount);
        sendWaiting(p);
    }

    static void claimDaily() {
        if (!Flags.check("casino.daily")) return;
        if (!SbcNet.online()) {
            Sbc.error(SbcNet.OFFLINE);
            return;
        }
        sendWaiting(packet("casinoDaily"));
    }

    static void requestStats() {
        SbcNet.sendCasino(packet("casinoStats"));
    }

    static void refreshLeaderboard() {
        if (loggedIn()) SbcNet.sendCasino(packet("casinoLeaderboard"));
    }

    static void requestBoard(String by) {
        JsonObject p = packet("casinoLeaderboard");
        p.addProperty("by", by);
        SbcNet.sendCasino(p);
    }

    private static void sendWaiting(JsonObject p) {
        if (!SbcNet.online()) return;
        SbcNet.sendCasino(p);
        waiting = true;
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
                if (Sbc.hasObj(packet, "daily")) daily = parseDaily(packet.getAsJsonObject("daily"));
                if (Sbc.hasObj(packet, "hilo")) hilo = parseHiLo(packet.getAsJsonObject("hilo"), hilo);
                if (packet.has("games") && packet.get("games").isJsonArray()) {
                    List<String> list = new ArrayList<>();
                    for (JsonElement e : packet.getAsJsonArray("games")) {
                        if (e.isJsonPrimitive()) list.add(e.getAsString());
                        else if (e.isJsonObject() && (!e.getAsJsonObject().has("enabled") || Sbc.bool(e.getAsJsonObject(), "enabled"))) list.add(Sbc.str(e.getAsJsonObject(), "id"));
                    }
                    games = List.copyOf(list);
                }
                // A hand just finished: the balances on the board changed.
                if (previous != null && previous.playing() && (hand == null || !hand.playing())) refreshLeaderboard();
                remindDaily();
            }
            case "casinoResult" -> {
                waiting = false;
                balance = getLong(packet, "balance", balance);
                refillAt = getLong(packet, "refillAt", refillAt);
                lastResult = new Result(Sbc.str(packet, "game"), getLong(packet, "bet", 0), getLong(packet, "payout", 0),
                    Sbc.str(packet, "outcome").isEmpty() ? (Sbc.bool(packet, "won") ? "won" : "lost") : Sbc.str(packet, "outcome"),
                    Sbc.obj(packet, "detail"), System.currentTimeMillis());
            }
            case "casinoHiLo" -> {
                waiting = false;
                balance = getLong(packet, "balance", balance);
                hilo = Sbc.hasObj(packet, "game") ? parseHiLo(packet.getAsJsonObject("game"), hilo) : null;
            }
            case "casinoDaily" -> {
                waiting = false;
                long reward = getLong(packet, "reward", 0);
                balance = getLong(packet, "balance", balance);
                daily = new Daily(false, (int) getLong(packet, "streak", 0), getLong(packet, "nextAt", 0), reward, getLong(packet, "tomorrow", 0));
                Sbc.say(Component.literal("Claimed your daily $" + Sbc.number(reward) + "! Streak: " + daily.streak() + " day" + (daily.streak() == 1 ? "" : "s") + ".")
                    .withStyle(ChatFormatting.GREEN));
            }
            case "casinoStats" -> {
                stats = packet;
                balance = getLong(packet, "balance", balance);
                if (Sbc.hasObj(packet, "daily")) daily = parseDaily(packet.getAsJsonObject("daily"));
            }
            case "casinoGift" -> {
                balance = getLong(packet, "balance", balance);
                String message = Sbc.str(packet, "message");
                long amount = getLong(packet, "amount", 0);
                if (message.isBlank()) {
                    String by = Sbc.str(packet, "by");
                    String reason = Sbc.str(packet, "reason");
                    message = (amount >= 0 ? "You received $" + Sbc.number(amount) : "$" + Sbc.number(-amount) + " was taken from you")
                        + (by.isBlank() ? "" : " by " + by) + (reason.isBlank() ? "" : ": " + reason) + ".";
                }
                Sbc.say(Component.literal("[Casino] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(message).withStyle(amount >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
            }
            case "casinoLeaderboard" -> {
                List<Entry> entries = new ArrayList<>();
                if (packet.has("entries")) {
                    for (JsonElement e : packet.getAsJsonArray("entries")) {
                        JsonObject o = e.getAsJsonObject();
                        long entryBalance = getLong(o, "balance", 0);
                        entries.add(new Entry(getString(o, "username"), getString(o, "minecraftUuid"), entryBalance, getLong(o, "value", entryBalance)));
                    }
                }
                String by = Sbc.str(packet, "by");
                long you = packet.has("you") && packet.get("you").isJsonObject() ? getLong(packet.getAsJsonObject("you"), "rank", 0) : 0;
                if (by.isEmpty() || by.equals("balance")) {
                    leaderboard = entries;
                    yourRank = (int) you;
                }
                BOARDS.put(by.isEmpty() ? "balance" : by, entries);
                BOARD_YOU.put(by.isEmpty() ? "balance" : by, you);
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

    /** "Your casino daily reward is ready", once per reward, if the reminder is on. */
    private static void remindDaily() {
        Daily d = daily;
        if (d == null || !d.available() || !Sbc.config().casino.dailyReminder || !Flags.isEnabled("casino.daily")) return;
        if (remindedFor == d.nextAt()) return;
        remindedFor = d.nextAt();
        Sbc.say(Component.literal("Your casino daily reward" + (d.reward() > 0 ? " ($" + Sbc.number(d.reward()) + ")" : "") + " is ready! ")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal("[Claim]").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/sb casino daily"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Claim your daily reward"))))));
    }

    /** While playing: when the next daily reward becomes available, ask for the state (which reminds you). */
    public static void tick() {
        Daily d = daily;
        if (d == null || d.available() || d.nextAt() <= 0 || System.currentTimeMillis() < d.nextAt()) return;
        daily = new Daily(true, d.streak(), d.nextAt(), d.tomorrow() > 0 ? d.tomorrow() : d.reward(), d.tomorrow());
        remindDaily();
    }

    private static Daily parseDaily(JsonObject o) {
        return new Daily(Sbc.bool(o, "available"), (int) getLong(o, "streak", 0), getLong(o, "nextAt", 0),
            getLong(o, "reward", 0), getLong(o, "tomorrow", 0));
    }

    private static HiLo parseHiLo(JsonObject o, HiLo previous) {
        if (o == null || o.size() == 0) return null;
        List<Integer> history = new ArrayList<>();
        for (JsonElement e : Sbc.arr(o, "history")) {
            try {
                history.add(e.isJsonObject() ? (int) getLong(e.getAsJsonObject(), "card", 0) : e.getAsInt());
            } catch (Exception ignored) {}
        }
        int card = (int) getLong(o, "card", 0);
        // Only animate a new card, not the same state sent again.
        long at = previous != null && previous.card() == card && previous.rounds() == getLong(o, "rounds", 0)
            && previous.state().equals(Sbc.str(o, "state")) ? previous.at() : System.currentTimeMillis();
        return new HiLo(getLong(o, "bet", 0), card, Sbc.dbl(o, "multiplier", 1), (int) getLong(o, "rounds", 0), history,
            Sbc.str(o, "state"), getLong(o, "payout", 0), getLong(o, "cashout", 0), Sbc.dbl(o, "higherMultiplier", 0),
            Sbc.dbl(o, "lowerMultiplier", 0), Sbc.str(o, "message"), at);
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
        return Sbc.lng(o, key, fallback);
    }

    private static String getString(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
}
