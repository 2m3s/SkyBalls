package com.epic60869.skyballs;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

public final class SkyBallsGlobalChat {
    private static final String CONFIGURED_RELAY_URL =
        System.getProperty("skyballs.chat.url", "").trim();
    private static final String[] RELAY_URLS = CONFIGURED_RELAY_URL.isBlank()
        ? new String[] {"wss://tastyfish.org/mod-api/tf-chat"}
        : new String[] {CONFIGURED_RELAY_URL};

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final AtomicBoolean CONNECTING = new AtomicBoolean(false);
    private static final AtomicLong REQUEST_IDS = new AtomicLong();
    private static volatile WebSocket socket;
    private static volatile long reconnectAt = 0L;
    private static volatile String username = "Unknown";
    private static volatile int relayIndex = 0;
    private static final Queue<String> PENDING_MESSAGES = new ArrayDeque<>();
    private static volatile boolean inSkyBallsChannel = false;

    public static boolean isInSkyBallsChannel() { return inSkyBallsChannel; }

    public static void enterSkyBallsChannel() {
        inSkyBallsChannel = true;
        mcMessage(Component.literal("You are now in the SkyBalls channel")
            .withStyle(Style.EMPTY.withColor(0x55FFFF).withBold(true))
            .append(Component.literal(" — anything you type will be sent to SkyBalls chat.")
                .withStyle(Style.EMPTY.withColor(0xAAAAAA))));
    }

    public static void leaveSkyBallsChannel() {
        inSkyBallsChannel = false;
        mcMessage(Component.literal("You have left the SkyBalls channel.")
            .withStyle(Style.EMPTY.withColor(0xFFAA00).withBold(true)));
    }

    private SkyBallsGlobalChat() {}

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        username = mc.getUser().getName();
        connect();
    }

    private static volatile long whoAskedAt;

    /**
     * /sb who: asks the chat server who is online with the mod. The server answers
     * {"type":"online","players":[{"username":"...","nickname":"..."}]}.
     */
    public static void requestWho() {
        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[SB] Connecting to SkyBalls chat, try again in a moment.").withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "who");
        ws.sendText(GSON.toJson(packet), true);
        long asked = System.currentTimeMillis();
        whoAskedAt = asked;
        java.util.concurrent.CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
            if (whoAskedAt == asked) {
                whoAskedAt = 0;
                mcMessage(Component.literal("[SB] The SkyBalls chat server didn't answer; it may not support /sb who yet.").withStyle(Style.EMPTY.withColor(0xAAAAAA)));
            }
        });
    }

    private static void showWho(JsonObject packet) {
        whoAskedAt = 0;
        com.google.gson.JsonArray players = packet.has("players") && packet.get("players").isJsonArray() ? packet.getAsJsonArray("players") : new com.google.gson.JsonArray();
        MutableComponent message = Component.literal("[SB] ").withStyle(net.minecraft.ChatFormatting.DARK_GREEN)
            .append(Component.literal(players.size() + " online with SkyBalls: ").withStyle(net.minecraft.ChatFormatting.YELLOW));
        boolean first = true;
        for (var element : players) {
            String username;
            String nickname = "";
            if (element.isJsonObject()) {
                JsonObject o = element.getAsJsonObject();
                username = o.has("username") ? o.get("username").getAsString() : "?";
                if (o.has("nickname") && !o.get("nickname").isJsonNull()) nickname = o.get("nickname").getAsString();
            } else {
                username = element.getAsString();
            }
            username = username.replaceAll("[^A-Za-z0-9_]", "");
            if (!first) message.append(Component.literal(", ").withStyle(net.minecraft.ChatFormatting.GRAY));
            first = false;
            MutableComponent name = Component.literal(username).withStyle(net.minecraft.ChatFormatting.WHITE);
            if (!nickname.isBlank() && !nickname.equalsIgnoreCase(username) && !SkyBallsNickFilter.isBlocked(nickname)) {
                name.append(Component.literal(" (" + nickname + ")").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            message.append(name);
        }
        mcMessage(message);
    }

    public static void sendBotCommand(String command) {
        String clean = String.valueOf(command == null ? "" : command).trim();
        if (clean.isEmpty() || !clean.startsWith("!")) return;

        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[SB] Bot command is still connecting...")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        JsonObject packet = new JsonObject();
        packet.addProperty("type", "command");
        packet.addProperty("username", username);
        packet.addProperty("command", clean.substring(0, Math.min(clean.length(), 500)));
        ws.sendText(GSON.toJson(packet), true);
        mcMessage(Component.literal("[SB] Sending " + clean).withStyle(Style.EMPTY.withColor(0xAAAAAA)));
    }

    public static void send(String message) {
        String clean = String.valueOf(message == null ? "" : message).trim();
        if (clean.isEmpty()) return;

        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            synchronized (PENDING_MESSAGES) {
                if (PENDING_MESSAGES.size() >= 20) PENDING_MESSAGES.poll();
                PENDING_MESSAGES.offer(clean.substring(0, Math.min(clean.length(), 500)));
            }
            connect();
            mcMessage(Component.literal("[SB] Global chat is connecting; your message will be sent when connected.")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        sendNow(ws, clean);
    }

    private static void sendNow(WebSocket ws, String clean) {
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "message");
        packet.addProperty("username", username);
        packet.addProperty("minecraftUuid", Minecraft.getInstance().getUser().getProfileId().toString());
        packet.addProperty("message", clean.substring(0, Math.min(clean.length(), 500)));
        int[] level = ownLevel();
        if (level != null) {
            packet.addProperty("level", level[0]);
            packet.addProperty("levelColor", String.format("#%06X", level[1] & 0xFFFFFF));
        }
        ws.sendText(GSON.toJson(packet), true);
    }

    private static final java.util.regex.Pattern TAB_LEVEL = java.util.regex.Pattern.compile("^\\[(\\d+)\\] (\\w+)");

    /** Your SkyBlock level and its colour, read from your own tab-list entry ("[279] name"), or null. */
    private static int[] ownLevel() {
        String me = Minecraft.getInstance().getUser().getName();
        for (var info : SkyBallsTabWidgetManager.players()) {
            Component name = com.epic60869.skyballs.custom.util.Compat.rawTabName(info);
            if (name == null) continue;
            String text = net.minecraft.ChatFormatting.stripFormatting(name.getString()).trim();
            java.util.regex.Matcher m = TAB_LEVEL.matcher(text);
            if (!m.find() || !m.group(2).equalsIgnoreCase(me)) continue;
            int level = Integer.parseInt(m.group(1));
            // Colour of the level number itself.
            final int[] colour = {0xAAAAAA};
            final boolean[] found = {false};
            name.visit((style, value) -> {
                if (!found[0] && value.chars().anyMatch(Character::isDigit)) {
                    if (style.getColor() != null) colour[0] = style.getColor().getValue();
                    found[0] = true;
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
            return new int[]{level, colour[0]};
        }
        return null;
    }

    private static void flushPending(WebSocket ws) {
        synchronized (PENDING_MESSAGES) {
            while (!PENDING_MESSAGES.isEmpty()) {
                sendNow(ws, PENDING_MESSAGES.poll());
            }
        }
    }

    public static void requestDiscord(String action, JsonObject data) {
        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[SB] Discord is still connecting...")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        JsonObject packet = data == null ? new JsonObject() : data.deepCopy();
        packet.addProperty("type", "discord");
        packet.addProperty("action", action);
        packet.addProperty("requestId", Long.toString(REQUEST_IDS.incrementAndGet()));
        ws.sendText(GSON.toJson(packet), true);
    }

    public static void sendDiscordDm(String target, String message) {
        String cleanTarget = String.valueOf(target == null ? "" : target).trim();
        String cleanMessage = String.valueOf(message == null ? "" : message).trim();

        if (cleanTarget.isEmpty()) {
            mcMessage(Component.literal("[SB] Usage: /sb dm <discord-user> <message/link>")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        if (cleanMessage.isEmpty()) {
            mcMessage(Component.literal("[SB] The Discord DM cannot be empty.")
                .withStyle(Style.EMPTY.withColor(0xFF5555)));
            return;
        }

        JsonObject packet = new JsonObject();
        packet.addProperty("type", "dm");
        packet.addProperty("username", username);
        packet.addProperty("target", cleanTarget);
        packet.addProperty("message", cleanMessage.substring(0, Math.min(cleanMessage.length(), 1900)));

        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[SB] Discord link is still connecting. Try again in a moment.")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        ws.sendText(GSON.toJson(packet), true);
    }

    /** The open connection, or null; identifies it (a reconnect is a new one, needing a new casino login). */
    public static Object currentConnection() {
        WebSocket ws = socket;
        return ws == null || ws.isInputClosed() || ws.isOutputClosed() ? null : ws;
    }

    public static void ensureConnected() {
        if (currentConnection() == null) connect();
    }

    /** Sends a packet if connected (returns false and starts connecting otherwise). */
    public static boolean send(JsonObject packet) {
        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            return false;
        }
        ws.sendText(GSON.toJson(packet), true);
        return true;
    }

    private static void connect() {
        if (!CONNECTING.compareAndSet(false, true)) return;

        String relayUrl = RELAY_URLS[Math.min(relayIndex, RELAY_URLS.length - 1)];

        HTTP.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(URI.create(relayUrl), new Listener())
            .whenComplete((ws, error) -> {
                CONNECTING.set(false);
                if (error != null) {
                    socket = null;
                    if (relayIndex + 1 < RELAY_URLS.length) {
                        relayIndex++;
                    }
                    mcMessage(Component.literal("[SB] Global chat connection failed: "
                        + shortError(error) + " — retrying.")
                        .withStyle(Style.EMPTY.withColor(0xFF5555)));
                    scheduleReconnect();
                    return;
                }

                relayIndex = 0;
                socket = ws;

                JsonObject hello = new JsonObject();
                hello.addProperty("type", "hello");
                hello.addProperty("username", username);
                // Packets this mod understands; without "rankAnnounce" the server sends a plain bot message instead.
                JsonArray features = new JsonArray();
                features.add("rankAnnounce");
                hello.add("features", features);
                hello.addProperty("minecraftUuid", Minecraft.getInstance().getUser().getProfileId().toString());
                hello.addProperty("modVersion", net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("skyballs")
                    .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("dev"));
                ws.sendText(GSON.toJson(hello), true);
                flushPending(ws);
            });
    }

    private static String shortError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    private static void scheduleReconnect() {
        reconnectAt = System.currentTimeMillis() + 5000L;
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (socket == null && reconnectAt > 0 && now >= reconnectAt) {
            reconnectAt = 0;
            connect();
        }
    }

    private static Component linkify(Component text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https?://\\S+");
        MutableComponent result = Component.empty();

        text.visit((style, value) -> {
            if (value == null || value.isEmpty()) return java.util.Optional.empty();

            java.util.regex.Matcher matcher = pattern.matcher(value);
            int last = 0;
            while (matcher.find()) {
                if (matcher.start() > last) {
                    result.append(Component.literal(value.substring(last, matcher.start())).setStyle(style));
                }

                String url = matcher.group();
                while (url.length() > 1 && ")]>.".indexOf(url.charAt(url.length() - 1)) >= 0) {
                    url = url.substring(0, url.length() - 1);
                }

                try {
                    result.append(Component.literal(url).setStyle(style.withUnderlined(true)
                        .withColor(0x55AAFF)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal(url)))));
                } catch (Exception ignored) {
                    result.append(Component.literal(url).setStyle(style));
                }
                last = matcher.end();
            }

            if (last < value.length()) {
                result.append(Component.literal(value.substring(last)).setStyle(style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        return result;
    }

    /**
     * "[SB] Steve Has Been Granted [VIP] By Console" or "[SB] Steve's [VIP] Prefix Has Been Removed By Console",
     * with the prefix in its exact hex colour.
     */
    private static Component rankAnnouncement(JsonObject packet) {
        String name = packetString(packet, "username").replace("§", "").replaceAll("[^A-Za-z0-9_]", "");
        if (name.isBlank()) name = "Unknown";
        String prefix = packetString(packet, "prefix").replace("§", "");
        String by = packetString(packet, "by").replace("§", "");
        boolean removed = "removed".equals(packetString(packet, "action"));
        int colour = 0xFFAA00;
        String hex = packetString(packet, "color");
        if (hex.matches("#[0-9a-fA-F]{6}")) colour = Integer.parseInt(hex.substring(1), 16);
        boolean bold = packet.has("bold") && packet.get("bold").getAsBoolean();

        Style gray = Style.EMPTY.withColor(0xAAAAAA);
        // Hovering the name shows the real Minecraft name, like normal SBC messages.
        Component user = Component.literal(name).withStyle(Style.EMPTY.withColor(0xFFFFFF)
            .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                Component.literal("Real name: ").withStyle(gray)
                    .append(Component.literal(name).withStyle(Style.EMPTY.withColor(0xFFFFFF))))));
        Component rank = Component.literal("[" + prefix + "]").withStyle(Style.EMPTY.withColor(colour).withBold(bold));
        MutableComponent line = Component.empty()
            .append(Component.literal("[SB]").withStyle(net.minecraft.ChatFormatting.DARK_GREEN))
            .append(Component.literal(" "))
            .append(user);
        if (removed) {
            line.append(Component.literal("'s ").withStyle(gray))
                .append(rank)
                .append(Component.literal(" Prefix Has Been Removed By " + by).withStyle(gray));
        } else {
            line.append(Component.literal(" Has Been Granted ").withStyle(gray))
                .append(rank)
                .append(Component.literal(" By " + by).withStyle(gray));
        }
        return line;
    }

    private static String packetString(JsonObject packet, String key) {
        return packet.has(key) && !packet.get(key).isJsonNull() ? packet.get(key).getAsString() : "";
    }

    private static void mcMessage(Component message) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(message);
            }
        });
    }

    private static final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String raw = buffer.toString();
                buffer.setLength(0);
                handle(raw);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (socket == webSocket) socket = null;
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (socket == webSocket) socket = null;
            scheduleReconnect();
        }

        private void handle(String raw) {
            try {
                JsonObject packet = JsonParser.parseString(raw).getAsJsonObject();
                String type = packet.has("type") ? packet.get("type").getAsString() : "";

                if ("accountStatus".equals(type) || "nickResult".equals(type)) {
                    Minecraft.getInstance().execute(() -> SkyBallsNickCommand.handle(type, packet));
                    return;
                }

                if (type.startsWith("casino")) {
                    Minecraft.getInstance().execute(() -> SkyBallsCasino.handle(type, packet));
                    return;
                }

                // The website changed someone's rank: reload them now instead of waiting for the next check.
                if ("ranksUpdated".equals(type) || "ranks".equals(type)) {
                    SkyBallsStaff.refreshNow();
                    return;
                }

                if ("online".equals(type) || "whoResult".equals(type)) {
                    showWho(packet);
                    return;
                }

                if ("nicknameUpdate".equals(type)) {
                    try {
                        UUID uuid = UUID.fromString(packet.get("minecraftUuid").getAsString());
                        boolean enabled = packet.has("enabled") && packet.get("enabled").getAsBoolean();
                        String username = packet.has("username") ? packet.get("username").getAsString() : "";
                        String name = packet.has("name") ? packet.get("name").getAsString() : "";
                        String mode = packet.has("mode") ? packet.get("mode").getAsString() : "Plain";
                        String hex = packet.has("customHex") ? packet.get("customHex").getAsString() : "";
                        String font = packet.has("font") ? packet.get("font").getAsString() : "Default";
                        SkyBallsNick.updateRemote(uuid, username, enabled, name, mode, hex, font);
                    } catch (Exception ignored) {}
                    return;
                }

                // Nicknames are persistent. A socket disconnect is not a
                // reason to erase another player's nickname from this client.
                // The authoritative nicknameUpdate packet handles enable/disable.

                if ("discordResult".equals(type)) {
                    String requestId = packet.has("requestId")
                        ? packet.get("requestId").getAsString() : "";
                    boolean ok = packet.has("ok") && packet.get("ok").getAsBoolean();
                    JsonObject result = packet.has("result") && packet.get("result").isJsonObject()
                        ? packet.getAsJsonObject("result") : new JsonObject();
                    String detail = packet.has("message") ? packet.get("message").getAsString() : "";

                    Minecraft.getInstance().execute(() ->
                        SkyBallsDiscordScreen.handleResult(requestId, ok, result, detail));
                    return;
                }

                if ("dmResult".equals(type)) {
                    boolean ok = packet.has("ok") && packet.get("ok").getAsBoolean();
                    String target = packet.has("target") ? packet.get("target").getAsString() : "Discord user";
                    String detail = packet.has("message") ? packet.get("message").getAsString() : "";

                    if (ok) {
                        mcMessage(Component.literal("[SB] Discord DM sent to " + target + ".")
                            .withStyle(Style.EMPTY.withColor(0x55FF55)));
                    } else {
                        mcMessage(Component.literal("[SB] Discord DM failed: " + detail)
                            .withStyle(Style.EMPTY.withColor(0xFF5555)));
                    }
                    return;
                }

                if ("rankAnnounce".equals(type)) {
                    SkyBallsConfig announceConfig = SkyBallsConfig.current();
                    if (announceConfig != null && !announceConfig.chat.customChat.showSjChat) return;
                    mcMessage(rankAnnouncement(packet));
                    return;
                }

                if (!"message".equals(type)) return;

                String name = packet.has("username") ? packet.get("username").getAsString() : "Unknown";
                String displayName = packet.has("nickname") ? packet.get("nickname").getAsString() : name;
                String message = packet.has("message") ? packet.get("message").getAsString() : "";
                // The chat server's bot still calls itself SkyJew and writes [SJ]; show the new name.
                if (name.equalsIgnoreCase("SkyJew") || displayName.equalsIgnoreCase("SkyJew")) {
                    name = "SkyBalls";
                    displayName = "SkyBalls";
                    message = message.replace("[SJ]", "[SB]").replace("SkyJew", "SkyBalls");
                }
                if (message.isBlank()) return;

                UUID messageUuid = null;
                try {
                    if (packet.has("minecraftUuid")) {
                        messageUuid = UUID.fromString(packet.get("minecraftUuid").getAsString());
                        String messageUsername = packet.has("username") ? packet.get("username").getAsString() : "";
                        boolean nickEnabled = packet.has("nicknameEnabled")
                            && packet.get("nicknameEnabled").getAsBoolean();
                        String nickMode = packet.has("nicknameMode")
                            ? packet.get("nicknameMode").getAsString() : "Plain";
                        String nickHex = packet.has("nicknameHex")
                            ? packet.get("nicknameHex").getAsString() : "";

                        // nicknameUpdate is the authoritative state packet.
                        // A chat message from an older/stale connection may not
                        // contain nickname styling, so it must never erase a
                        // nickname that was already synced from the relay.
                        if (nickEnabled) {
                            String nickFont = packet.has("nicknameFont") ? packet.get("nicknameFont").getAsString() : null;
                            SkyBallsNick.updateRemote(messageUuid, messageUsername, true, displayName, nickMode, nickHex, nickFont);
                        }
                    }
                } catch (Exception ignored) {}

                name = name.replaceAll("[^A-Za-z0-9_]", "");
                if (name.isBlank()) name = "Unknown";

                String source = packet.has("source") ? packet.get("source").getAsString() : "mod";
                String prefix = "discord".equalsIgnoreCase(source) ? "[Discord]" : "[SB]";

                if (SkyBallsNickFilter.isBlocked(displayName, messageUuid)) displayName = name;
                Component shownName;
                try {
                    shownName = SkyBallsNick.displayName(messageUuid, displayName);
                } catch (Exception ignored) {
                    shownName = SkyBallsNick.displayName(displayName);
                }
                Component messageComponent = com.epic60869.skyballs.features.misc.ItemEmojis.replace(SkyBallsNopoFeatures.replaceChatEmojis(Component.literal(message)));
                // [SB] in dark green, like Hypixel's "Guild >"; messages from Discord add a blue [Discord] after it.
                MutableComponent line = Component.empty()
                    .append(Component.literal("[SB]").withStyle(net.minecraft.ChatFormatting.DARK_GREEN))
                    .append(Component.literal(" "));
                if (!"[SB]".equals(prefix)) {
                    line.append(Component.literal(prefix).withStyle(net.minecraft.ChatFormatting.BLUE)).append(Component.literal(" "));
                }
                // Rank prefix, by account UUID, only for messages sent from the mod (not Discord), if turned on.
                SkyBallsConfig rankConfig = SkyBallsConfig.current();
                boolean showRanks = rankConfig == null || rankConfig.chat.customChat.showRanks;
                MutableComponent staff = "discord".equalsIgnoreCase(source) || !showRanks ? null : SkyBallsStaff.prefix(messageUuid);
                if (staff != null) line.append(staff);
                int level = packet.has("level") ? packet.get("level").getAsInt() : 0;
                if (level > 0) {
                    int levelColor = 0xAAAAAA;
                    try {
                        String hex = packet.has("levelColor") ? packet.get("levelColor").getAsString() : "";
                        if (hex.matches("#[0-9a-fA-F]{6}")) levelColor = Integer.parseInt(hex.substring(1), 16);
                    } catch (Exception ignored) {}
                    line.append(Component.literal("[" + level + "] ").withStyle(Style.EMPTY.withColor(levelColor)));
                }
                // Hovering the sender shows their real Minecraft name.
                MutableComponent sender = Component.empty().withStyle(Style.EMPTY
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                        Component.literal("Real name: ").withStyle(Style.EMPTY.withColor(0xAAAAAA))
                            .append(Component.literal(name).withStyle(Style.EMPTY.withColor(0xFFFFFF)))))
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent.SuggestCommand("/msg " + name + " ")));
                sender.append(shownName);
                line.append(Component.literal("["))
                    .append(sender)
                    .append(Component.literal("]: "))
                    .append(linkify(messageComponent));
                SkyBallsConfig chatConfig = SkyBallsConfig.current();
                if (chatConfig != null && !chatConfig.chat.customChat.showSjChat) return;
                mcMessage(line);
                // Optional ping for other players' messages.
                java.util.UUID self = Minecraft.getInstance().getUser().getProfileId();
                if (chatConfig != null && chatConfig.chat.customChat.pingSound && (messageUuid == null || !messageUuid.equals(self))) {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        if (mc.player != null) mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 1.8f);
                    });
                }
            } catch (Exception ignored) {
            }
        }
    }
}
