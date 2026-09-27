package com.epic60869.skyballs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Discord leaderboards: the SBC chat server says which leaderboards are running and what they count; the mod watches
 * your chat for those things and reports them, and the server keeps the standings (shown in Discord and with
 * /sb leaderboard).
 *
 * <pre>
 * server -> mod: leaderboardsActive {boards: [{id, name, endsAt, trackers: [
 *                    {kind: "drop", items: ["Summoning Eye", ...]},            RARE DROP! / PET DROP! ... messages
 *                    {kind: "chat", pattern: "^...$", amountGroup: "amount"}   any chat message (regex)
 *                ]}]}
 *                leaderboardStandings {boardId, name, endsAt, entries: [{username, amount}], you: {rank, amount}}
 *                leaderboardProgressResult {boardId, ok, total, rank, message}
 * mod -> server: leaderboardProgress {boardId, amount, detail, message}  (needs the casinoAuth login)
 *                leaderboardGet {boardId}
 * </pre>
 */
public final class SkyBallsLeaderboards {
    /** Hypixel's drop messages: "RARE DROP! Summoning Eye (+312% ✯ Magic Find)", "PET DROP! Ender Dragon". */
    private static final Pattern DROP = Pattern.compile(
        "^(?:(?:VERY |CRAZY |INSANE |PRAY TO RNGESUS )?RARE DROP!|PET DROP!|INSANE DROP!)\\s+\\(?(?<item>.+?)\\)?(?:\\s+x(?<amount>[\\d,]+))?(?:\\s+\\(\\+[\\d,.]+%? ?.*Magic Find\\))?\\s*$");
    private static final int MAX_PATTERN_LENGTH = 300;

    private record Tracker(String kind, List<String> items, Pattern pattern, String amountGroup) {}

    private record Board(String id, String name, long endsAt, List<Tracker> trackers) {}

    private static volatile List<Board> boards = List.of();
    /** Test boards made with /sb lbdebug addlocal: counted in the mod only, never reported. */
    private static final List<Board> LOCAL_BOARDS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final String LOCAL_PREFIX = "local:";

    private SkyBallsLeaderboards() {}

    public static void init() {
        com.epic60869.skyballs.features.core.SkyBallsChat.onChat(message -> onChat(message.text()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : com.epic60869.skyballs.custom.util.Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("leaderboard")
                    .executes(c -> list())
                    .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(boards.stream().map(Board::name).toList(), b))
                        .executes(c -> standings(StringArgumentType.getString(c, "name"))))));
                // Testing tools, only for the owner account (like /sb viewboth).
                if (com.epic60869.skyballs.reports.Reports.isOwner()) dispatcher.register(ClientCommands.literal(root).then(debugCommand()));
            }
        });
    }

    // ---------------------------------------------------------------- counting

    private static void onChat(String text) {
        process(text, true);
    }

    /**
     * Counts a chat line for every running board (and local test board). Returns what it matched, as
     * "board: +amount detail"; reports them to the server when {@code send} is true (local boards never are).
     */
    private static List<String> process(String text, boolean send) {
        List<String> matched = new ArrayList<>();
        List<Board> running = new ArrayList<>(boards);
        running.addAll(LOCAL_BOARDS);
        if (running.isEmpty() || text == null || text.isBlank()) return matched;
        long now = System.currentTimeMillis();
        String line = text.trim();
        Matcher drop = DROP.matcher(line);
        String dropItem = drop.matches() ? drop.group("item").trim() : null;
        long dropAmount = dropItem != null && drop.group("amount") != null ? parse(drop.group("amount")) : 1;
        for (Board board : running) {
            if (board.endsAt() > 0 && now > board.endsAt()) continue;
            for (Tracker tracker : board.trackers()) {
                long amount = 0;
                String detail = null;
                if ("drop".equals(tracker.kind()) && dropItem != null) {
                    for (String item : tracker.items()) {
                        if (item.equalsIgnoreCase(dropItem)) {
                            amount = dropAmount;
                            detail = dropItem;
                            break;
                        }
                    }
                } else if ("chat".equals(tracker.kind()) && tracker.pattern() != null) {
                    Matcher m = tracker.pattern().matcher(line);
                    if (m.find()) {
                        amount = 1;
                        if (tracker.amountGroup() != null) {
                            try {
                                amount = parse(m.group(tracker.amountGroup()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        detail = m.group();
                    }
                }
                if (detail == null) continue;
                matched.add(board.name() + ": +" + amount + " " + detail);
                if (send && !board.id().startsWith(LOCAL_PREFIX)) report(board, amount, detail, line);
            }
        }
        return matched;
    }

    /** One thing counted: sent once logged in (the server only trusts a checked Minecraft login). */
    private static void report(Board board, long amount, String detail, String message) {
        if (amount <= 0) return;
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "leaderboardProgress");
        packet.addProperty("boardId", board.id());
        packet.addProperty("amount", amount);
        packet.addProperty("detail", detail);
        packet.addProperty("message", message.length() > 256 ? message.substring(0, 256) : message);
        Minecraft.getInstance().execute(() -> SkyBallsLogin.whenLoggedIn(() -> SkyBallsGlobalChat.send(packet)));
    }

    private static long parse(String number) {
        if (number == null) return 1;
        try {
            return Long.parseLong(number.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ---------------------------------------------------------------- commands

    private static int list() {
        List<Board> running = boards;
        if (running.isEmpty()) {
            say(Component.literal("No leaderboards are running right now.").withStyle(ChatFormatting.GRAY));
            return 1;
        }
        MutableComponent text = Component.literal("Running leaderboards: ").withStyle(ChatFormatting.GRAY);
        for (int i = 0; i < running.size(); i++) {
            Board board = running.get(i);
            if (i > 0) text.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            text.append(Component.literal(board.name()).withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/sb leaderboard " + board.name()))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal("Show the standings")))));
        }
        say(text);
        return 1;
    }

    private static int standings(String name) {
        Board board = boards.stream().filter(b -> b.name().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
        if (board == null) {
            say(Component.literal("No running leaderboard called \"" + name + "\".").withStyle(ChatFormatting.RED));
            return 1;
        }
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "leaderboardGet");
        packet.addProperty("boardId", board.id());
        if (!SkyBallsGlobalChat.send(packet)) {
            say(Component.literal("Connecting to SBC, try again in a moment.").withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    // ---------------------------------------------------------------- packets from the server

    /** leaderboardsActive, leaderboardStandings and leaderboardProgressResult (on the game thread). */
    public static void handle(String type, JsonObject packet) {
        switch (type) {
            case "leaderboardsActive" -> boards = parseBoards(packet);
            case "leaderboardStandings" -> showStandings(packet);
            case "leaderboardProgressResult" -> {
                boolean ok = packet.has("ok") && packet.get("ok").getAsBoolean();
                String message = string(packet, "message");
                if (!message.isEmpty()) say(Component.literal(message).withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
                if (!ok && "notLoggedIn".equals(string(packet, "code"))) SkyBallsLogin.forget();
            }
            default -> {}
        }
    }

    private static List<Board> parseBoards(JsonObject packet) {
        List<Board> out = new ArrayList<>();
        if (!packet.has("boards") || !packet.get("boards").isJsonArray()) return out;
        for (JsonElement e : packet.getAsJsonArray("boards")) {
            try {
                JsonObject o = e.getAsJsonObject();
                List<Tracker> trackers = new ArrayList<>();
                if (o.has("trackers")) {
                    for (JsonElement t : o.getAsJsonArray("trackers")) {
                        JsonObject tracker = t.getAsJsonObject();
                        String kind = string(tracker, "kind").toLowerCase(Locale.ROOT);
                        List<String> items = new ArrayList<>();
                        if (tracker.has("items") && tracker.get("items").isJsonArray()) {
                            for (JsonElement item : tracker.getAsJsonArray("items")) items.add(item.getAsString().trim());
                        }
                        Pattern pattern = null;
                        String source = string(tracker, "pattern");
                        if (!source.isEmpty() && source.length() <= MAX_PATTERN_LENGTH) {
                            try {
                                pattern = Pattern.compile(source);
                            } catch (PatternSyntaxException ignored) {}
                        }
                        String group = string(tracker, "amountGroup");
                        trackers.add(new Tracker(kind, items, pattern, group.isEmpty() ? null : group));
                    }
                }
                out.add(new Board(string(o, "id"), string(o, "name"), o.has("endsAt") ? o.get("endsAt").getAsLong() : 0, trackers));
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static void showStandings(JsonObject packet) {
        String name = string(packet, "name");
        say(Component.literal(name + " leaderboard").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        JsonArray entries = packet.has("entries") && packet.get("entries").isJsonArray() ? packet.getAsJsonArray("entries") : new JsonArray();
        if (entries.isEmpty()) {
            say(Component.literal("Nobody has anything counted yet.").withStyle(ChatFormatting.GRAY));
        }
        String self = Minecraft.getInstance().getUser().getName();
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            JsonObject entry = entries.get(i).getAsJsonObject();
            String user = string(entry, "username");
            ChatFormatting colour = i == 0 ? ChatFormatting.GOLD : i == 1 ? ChatFormatting.WHITE : i == 2 ? ChatFormatting.RED
                : user.equalsIgnoreCase(self) ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            say(Component.literal((i + 1) + ". " + user + " - " + String.format(Locale.ENGLISH, "%,d", entry.get("amount").getAsLong()))
                .withStyle(colour));
        }
        if (packet.has("you") && packet.get("you").isJsonObject()) {
            JsonObject you = packet.getAsJsonObject("you");
            long rank = you.has("rank") ? you.get("rank").getAsLong() : 0;
            if (rank > 10) {
                say(Component.literal("You: #" + rank + " - " + String.format(Locale.ENGLISH, "%,d", you.get("amount").getAsLong()))
                    .withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // ---------------------------------------------------------------- /sb lbdebug (owner only)

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> debugCommand() {
        return ClientCommands.literal("lbdebug")
            .executes(c -> debugHelp())
            .then(ClientCommands.literal("status").executes(c -> debugStatus()))
            .then(ClientCommands.literal("dry").then(ClientCommands.argument("line", StringArgumentType.greedyString())
                .executes(c -> debugLine(StringArgumentType.getString(c, "line"), false))))
            .then(ClientCommands.literal("send").then(ClientCommands.argument("line", StringArgumentType.greedyString())
                .executes(c -> debugLine(StringArgumentType.getString(c, "line"), true))))
            .then(ClientCommands.literal("addlocal").then(ClientCommands.argument("item", StringArgumentType.greedyString())
                .executes(c -> debugAddLocal(StringArgumentType.getString(c, "item")))))
            .then(ClientCommands.literal("clearlocal").executes(c -> {
                LOCAL_BOARDS.clear();
                say(Component.literal("Local test boards removed.").withStyle(ChatFormatting.GRAY));
                return 1;
            }));
    }

    private static int debugHelp() {
        for (String line : List.of(
            "/sb lbdebug status - connection, login and the boards the server sent",
            "/sb lbdebug dry <chat line> - what that line would count (nothing is sent)",
            "/sb lbdebug send <chat line> - count it and report it to the server for real",
            "/sb lbdebug addlocal <item> - a local drop board for testing matching (never reported)",
            "/sb lbdebug clearlocal - remove the local test boards",
            "Example: /sb lbdebug dry RARE DROP! Summoning Eye")) {
            say(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int debugStatus() {
        boolean connected = SkyBallsGlobalChat.currentConnection() != null;
        String problem = SkyBallsLogin.problem();
        say(Component.literal("Connected to SBC: " + connected + ", logged in: " + SkyBallsLogin.loggedIn()
            + (problem.isEmpty() ? "" : " (last login error: " + problem + ")")).withStyle(ChatFormatting.YELLOW));
        List<Board> all = new ArrayList<>(boards);
        all.addAll(LOCAL_BOARDS);
        if (all.isEmpty()) {
            say(Component.literal("No boards: the server hasn't sent leaderboardsActive, or none are running.").withStyle(ChatFormatting.GRAY));
        }
        long now = System.currentTimeMillis();
        for (Board board : all) {
            String ends = board.endsAt() <= 0 ? "no end" : board.endsAt() < now ? "ENDED" : "ends in " + (board.endsAt() - now) / 60_000L + "m";
            say(Component.literal(board.name() + " [" + board.id() + "] " + ends).withStyle(ChatFormatting.GOLD));
            for (Tracker t : board.trackers()) {
                String what = "drop".equals(t.kind()) ? "items " + t.items()
                    : "pattern " + (t.pattern() == null ? "(invalid or missing)" : t.pattern().pattern())
                        + (t.amountGroup() == null ? "" : ", amount group " + t.amountGroup());
                say(Component.literal("  " + t.kind() + ": " + what).withStyle(ChatFormatting.GRAY));
            }
        }
        return 1;
    }

    private static int debugLine(String line, boolean send) {
        List<String> matched = process(line, send);
        if (matched.isEmpty()) {
            say(Component.literal("No board counts that line.").withStyle(ChatFormatting.RED));
        } else {
            for (String m : matched) say(Component.literal((send ? "Sent " : "Would count ") + m).withStyle(ChatFormatting.GREEN));
            if (send && !SkyBallsLogin.loggedIn()) {
                say(Component.literal("Logging in first; the reports go out once that is done.").withStyle(ChatFormatting.GRAY));
            }
        }
        return 1;
    }

    private static int debugAddLocal(String item) {
        String name = "Local test " + (LOCAL_BOARDS.size() + 1);
        LOCAL_BOARDS.add(new Board(LOCAL_PREFIX + name, name, 0, List.of(new Tracker("drop", List.of(item.trim()), null, null))));
        say(Component.literal("Added " + name + ", counting drops of " + item.trim() + " (local only, never reported).")
            .withStyle(ChatFormatting.GREEN));
        return 1;
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
