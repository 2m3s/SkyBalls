package com.epic60869.skyballs.features.slayer;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsGlobalChat;
import com.epic60869.skyballs.SkyBallsLogin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The slayer kill time leaderboard (/sb leaderboard slayer): everyone's personal best kill time for each slayer boss
 * and tier, fastest first. {@link SlayerTimes} times your kills; this shares your bests with the SBC chat server
 * (which only accepts them from a checked Minecraft login) and shows the standings.
 *
 * <pre>
 * mod -> server: slayerPb {boss, timeMs}                a new personal best (after the login)
 *                slayerPbSync {bests: {boss: timeMs}}   every personal best, after each login
 *                slayerPbGet {boss}                     standings for one boss key
 *                slayerPbSummary {}                     every boss key with its record holder
 * server -> mod: slayerPbResult {ok, boss, timeMs, rank, improved, message, code}
 *                slayerPbSyncResult {ok, accepted, improved, rejected, code}
 *                slayerPbStandings {boss, entries: [{username, timeMs}] (top 10), you: {rank, timeMs}, error}
 *                slayerPbSummary {bosses: [{boss, leader, leaderTimeMs, entries, you: {rank, timeMs}}]}
 * </pre>
 *
 * Boss keys are {@link SlayerTimes}' ("Revenant Horror V", "Atoned Horror"); the server keeps the lower time per
 * account and boss, so resending a best is harmless.
 */
public final class SlayerLeaderboard {
    private static final List<String> BOSSES = List.of("Revenant Horror", "Atoned Horror", "Tarantula Broodfather",
        "Conjoined Brood", "Sven Packmaster", "Voidgloom Seraph", "Inferno Demonlord", "Riftstalker Bloodfiend");
    private static final List<String> TIERS = List.of("I", "II", "III", "IV", "V");
    private static final Pattern KEY = Pattern.compile("(?i)^(" + String.join("|", BOSSES) + ")(?: (I|II|III|IV|V))?$");
    /** How long to wait for standings before showing your own times instead. */
    private static final long ANSWER_TIMEOUT_MS = 4_000L;

    /** Boss keys from the last summary, for tab completion. */
    private static final List<String> LAST_SUMMARY = new ArrayList<>();
    /** The view waiting for an answer ("summary" or a boss key), or null. A late answer after the fallback is dropped. */
    private static String waitingFor;
    private static int waitToken;
    /** A report the server refused because the login had lapsed: resent once after logging in again. */
    private static JsonObject unansweredReport;
    private static boolean syncRetried;

    private SlayerLeaderboard() {}

    private static boolean sharing() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c != null && c.slayers.sharePbs;
    }

    // ---------------------------------------------------------------- reporting

    /** {@link SlayerTimes} recorded a new personal best: tell the server once logged in. */
    static void onPersonalBest(String boss, long millis) {
        if (!sharing()) return;
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "slayerPb");
        packet.addProperty("boss", boss);
        packet.addProperty("timeMs", millis);
        Minecraft.getInstance().execute(() -> SkyBallsLogin.whenLoggedIn(() -> sendReport(packet)));
    }

    private static void sendReport(JsonObject packet) {
        if (SkyBallsGlobalChat.send(packet)) unansweredReport = packet;
    }

    /**
     * Called after every successful login: sends all your personal bests, so ones set before this feature existed,
     * or while SBC was offline, still count.
     */
    public static void onLoggedIn() {
        syncRetried = false;
        sync();
    }

    private static void sync() {
        if (!sharing()) return;
        Map<String, Long> bests = SlayerTimes.personalBests();
        if (bests.isEmpty()) return;
        JsonObject all = new JsonObject();
        bests.forEach(all::addProperty);
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "slayerPbSync");
        packet.add("bests", all);
        SkyBallsGlobalChat.send(packet);
    }

    // ---------------------------------------------------------------- views

    /** The boss key written the way the server and {@link SlayerTimes} write it, or null if it isn't one. */
    public static String canonical(String input) {
        Matcher m = KEY.matcher(input == null ? "" : input.trim().replaceAll("\\s+", " "));
        if (!m.matches()) return null;
        String name = BOSSES.stream().filter(b -> b.equalsIgnoreCase(m.group(1))).findFirst().orElse(m.group(1));
        return m.group(2) == null ? name : name + " " + m.group(2).toUpperCase(Locale.ROOT);
    }

    /** Boss keys for tab completion: yours and the ones in the last summary, in boss and tier order. */
    public static List<String> suggestions() {
        LinkedHashSet<String> keys = new LinkedHashSet<>(SlayerTimes.personalBests().keySet());
        keys.addAll(LAST_SUMMARY);
        return keys.stream().filter(k -> canonical(k) != null).sorted(ORDER).toList();
    }

    /** /sb leaderboard slayer: every boss with its record holder and your time. */
    public static void showSummary() {
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "slayerPbSummary");
        request(packet, "summary");
    }

    /** /sb leaderboard slayer &lt;boss&gt;: the top 10 for one boss key. */
    public static void showBoss(String input) {
        String boss = canonical(input);
        if (boss == null) {
            say(Component.literal("\"" + input.trim() + "\" isn't a slayer boss. Try e.g. Revenant Horror V.").withStyle(ChatFormatting.RED));
            return;
        }
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "slayerPbGet");
        packet.addProperty("boss", boss);
        request(packet, boss);
    }

    /** Asks the server; shows your own times instead if it can't be reached or doesn't answer in time. */
    private static void request(JsonObject packet, String view) {
        int token = ++waitToken;
        waitingFor = view;
        if (!SkyBallsGlobalChat.send(packet)) {
            waitingFor = null;
            showLocal(view);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.delayedExecutor(ANSWER_TIMEOUT_MS, TimeUnit.MILLISECONDS).execute(() -> mc.execute(() -> {
            if (waitToken == token && waitingFor != null) {
                waitingFor = null;
                showLocal(view);
            }
        }));
    }

    /** Offline: your own personal bests, with a note that the global standings couldn't be loaded. */
    private static void showLocal(String view) {
        Map<String, Long> bests = SlayerTimes.personalBests();
        say(Component.literal("Slayer PBs").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        say(Component.literal("Couldn't load the global standings; showing your own times.").withStyle(ChatFormatting.YELLOW));
        List<String> keys = "summary".equals(view) ? bests.keySet().stream().sorted(ORDER).toList() : List.of(view);
        boolean any = false;
        for (String boss : keys) {
            Long time = bests.get(boss);
            if (time == null) continue;
            any = true;
            say(Component.literal(boss + ": ").withStyle(ChatFormatting.RED).append(Component.literal(SlayerTimes.format(time)).withStyle(ChatFormatting.GREEN)));
        }
        if (!any) {
            say(Component.literal("summary".equals(view) ? "You don't have any slayer times yet." : "You don't have a " + view + " time yet.")
                .withStyle(ChatFormatting.GRAY));
        }
    }

    // ---------------------------------------------------------------- packets from the server

    /** slayerPbResult, slayerPbSyncResult, slayerPbStandings and slayerPbSummary (on the game thread). */
    public static void handle(String type, JsonObject packet) {
        switch (type) {
            case "slayerPbResult" -> onReportResult(packet);
            case "slayerPbSyncResult" -> {
                if (!ok(packet) && "notLoggedIn".equals(string(packet, "code")) && !syncRetried) {
                    syncRetried = true;
                    SkyBallsLogin.forget();
                    SkyBallsLogin.whenLoggedIn(SlayerLeaderboard::sync);
                }
            }
            case "slayerPbStandings" -> {
                if (!string(packet, "boss").equalsIgnoreCase(waitingFor == null ? "" : waitingFor)) return;
                waitingFor = null;
                showStandings(packet);
            }
            case "slayerPbSummary" -> {
                rememberSummary(packet);
                if (!"summary".equals(waitingFor)) return;
                waitingFor = null;
                showSummary(packet);
            }
            default -> {}
        }
    }

    private static void onReportResult(JsonObject packet) {
        JsonObject answered = unansweredReport;
        unansweredReport = null;
        String message = string(packet, "message");
        if (!ok(packet)) {
            if ("notLoggedIn".equals(string(packet, "code")) && answered != null && !answered.has("retried")) {
                // Log in again and resend it once, so the time isn't lost (the next sync would also send it).
                answered.addProperty("retried", true);
                SkyBallsLogin.forget();
                SkyBallsLogin.whenLoggedIn(() -> sendReport(answered));
                return;
            }
            if (!message.isEmpty()) say(Component.literal(message).withStyle(ChatFormatting.RED));
            return;
        }
        // SlayerTimes already said "NEW PERSONAL BEST!"; add where that puts you on the SkyBalls board.
        if (packet.has("improved") && packet.get("improved").getAsBoolean() && !message.isEmpty()) {
            say(Component.literal(message).withStyle(style -> style.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent.RunCommand("/sb leaderboard slayer " + string(packet, "boss")))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Show the standings")))));
        }
    }

    private static void rememberSummary(JsonObject packet) {
        LAST_SUMMARY.clear();
        for (JsonObject row : rows(packet, "bosses")) {
            String boss = canonical(string(row, "boss"));
            if (boss != null) LAST_SUMMARY.add(boss);
        }
    }

    private static void showSummary(JsonObject packet) {
        say(Component.literal("Slayer PBs").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal(" (click a boss for its top 10)").withStyle(ChatFormatting.GRAY).withStyle(s -> s.withBold(false))));
        // Server rows, plus your own times the board doesn't have yet (e.g. just before the sync lands).
        Map<String, JsonObject> byBoss = new TreeMap<>(ORDER);
        for (JsonObject row : rows(packet, "bosses")) {
            String boss = canonical(string(row, "boss"));
            if (boss != null) byBoss.put(boss, row);
        }
        Map<String, Long> mine = SlayerTimes.personalBests();
        for (String boss : mine.keySet()) if (canonical(boss) != null) byBoss.putIfAbsent(boss, null);
        if (byBoss.isEmpty()) {
            say(Component.literal("Nobody has a slayer time yet. Kill a boss to set one!").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (Map.Entry<String, JsonObject> entry : byBoss.entrySet()) {
            String boss = entry.getKey();
            JsonObject row = entry.getValue();
            MutableComponent line = Component.literal(boss + ": ").withStyle(ChatFormatting.RED);
            if (row != null) {
                line.append(Component.literal(string(row, "leader") + " ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(SlayerTimes.format(row.get("leaderTimeMs").getAsLong())).withStyle(ChatFormatting.AQUA));
            } else {
                line.append(Component.literal("no times yet").withStyle(ChatFormatting.DARK_GRAY));
            }
            JsonObject you = row != null && row.has("you") && row.get("you").isJsonObject() ? row.getAsJsonObject("you") : null;
            if (you != null) {
                long rank = you.get("rank").getAsLong();
                line.append(Component.literal(rank == 1 ? "  (you!)" : "  You: ").withStyle(ChatFormatting.GRAY));
                if (rank != 1) {
                    line.append(Component.literal(SlayerTimes.format(you.get("timeMs").getAsLong())).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" #" + rank).withStyle(ChatFormatting.GRAY));
                }
            } else if (mine.containsKey(boss)) {
                line.append(Component.literal("  You: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(SlayerTimes.format(mine.get(boss))).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(sharing() ? " (not on the board yet)" : " (sharing is off)").withStyle(ChatFormatting.DARK_GRAY));
            }
            say(line.withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand("/sb leaderboard slayer " + boss))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Show the " + boss + " top 10")))));
        }
    }

    private static void showStandings(JsonObject packet) {
        String boss = string(packet, "boss");
        if (!string(packet, "error").isEmpty()) {
            say(Component.literal("\"" + boss + "\" isn't a slayer boss the leaderboard knows.").withStyle(ChatFormatting.RED));
            return;
        }
        say(Component.literal(boss + " kill times").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        List<JsonObject> entries = rows(packet, "entries");
        if (entries.isEmpty()) say(Component.literal("Nobody has a time for this boss yet.").withStyle(ChatFormatting.GRAY));
        String self = Minecraft.getInstance().getUser().getName();
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            JsonObject entry = entries.get(i);
            String user = string(entry, "username");
            ChatFormatting colour = i == 0 ? ChatFormatting.GOLD : i == 1 ? ChatFormatting.WHITE : i == 2 ? ChatFormatting.RED
                : user.equalsIgnoreCase(self) ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            say(Component.literal((i + 1) + ". " + user + " - " + SlayerTimes.format(entry.get("timeMs").getAsLong())).withStyle(colour));
        }
        if (packet.has("you") && packet.get("you").isJsonObject()) {
            JsonObject you = packet.getAsJsonObject("you");
            long rank = you.get("rank").getAsLong();
            if (rank > 10) say(Component.literal("You: #" + rank + " - " + SlayerTimes.format(you.get("timeMs").getAsLong())).withStyle(ChatFormatting.GREEN));
        } else {
            Long mine = SlayerTimes.personalBests().get(boss);
            if (mine != null) {
                say(Component.literal("You: " + SlayerTimes.format(mine) + (sharing() ? " (not on the board yet)" : " (sharing is off)"))
                    .withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Boss order as in the Slayer menu, then tier (no tier first). */
    private static final Comparator<String> ORDER = Comparator.comparingInt(SlayerLeaderboard::orderOf).thenComparing(Comparator.naturalOrder());

    private static int orderOf(String key) {
        Matcher m = KEY.matcher(key);
        if (!m.matches()) return Integer.MAX_VALUE;
        int boss = BOSSES.indexOf(canonical(m.group(1)));
        return boss * 10 + (m.group(2) == null ? 0 : TIERS.indexOf(m.group(2).toUpperCase(Locale.ROOT)) + 1);
    }

    private static List<JsonObject> rows(JsonObject packet, String key) {
        List<JsonObject> out = new ArrayList<>();
        if (!packet.has(key) || !packet.get(key).isJsonArray()) return out;
        JsonArray array = packet.getAsJsonArray(key);
        for (JsonElement e : array) if (e.isJsonObject()) out.add(e.getAsJsonObject());
        return out;
    }

    private static boolean ok(JsonObject packet) {
        return packet.has("ok") && packet.get("ok").getAsBoolean();
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static void say(Component text) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.gui.hud.getChat().addClientSystemMessage(Component.literal("[SB] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(text));
            }
        });
    }
}
