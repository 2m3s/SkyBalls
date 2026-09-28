package com.epic60869.skyballs.features.slayer;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slayer time messages and personal bests, following SkyHanni's SlayerTimeMessages: how long your boss took to kill
 * (from its "Spawned by: you" nametag appearing to "NICE! SLAYER BOSS SLAIN!"), your personal best for that boss and
 * tier (saved per Minecraft account), and how long the whole quest took.
 */
public final class SlayerTimes {
    /** "☠ Revenant Horror V 12M❤" -> "Revenant Horror", "V". */
    private static final Pattern BOSS = Pattern.compile("(Revenant Horror|Atoned Horror|Tarantula Broodfather|Conjoined Brood"
        + "|Sven Packmaster|Voidgloom Seraph|Inferno Demonlord|Riftstalker Bloodfiend)(?: ([IVX]+))?");
    private static final Gson GSON = new Gson();

    /** Minecraft username (lowercase) -> boss key ("Revenant Horror V") -> best kill time in ms. */
    private static final Map<String, Map<String, Long>> BESTS = new ConcurrentHashMap<>();
    private static Path file;

    private static long questStartedAt;
    private static long spawnedAt;
    private static String boss;
    /** Set after a kill until your boss's nametag is gone, so the lingering tag doesn't start a new timer. */
    private static boolean killed;

    private SlayerTimes() {}

    public static void init(Path configDir) {
        file = configDir.resolve("skyballs").resolve("slayer-personal-bests.json");
        load();
        SkyBallsChat.onChat(message -> onChat(message.text().trim()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> reset());
    }

    private static SkyBallsConfig.Slayers config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.slayers;
    }

    /** Whether the boss scan in {@link SlayerFeatures} has to run for these messages. */
    static boolean enabled() {
        SkyBallsConfig.Slayers config = config();
        return config != null && (config.timeToKill || config.personalBests);
    }

    /** Your boss's health line this tick, or null when your boss isn't around (from {@link SlayerFeatures}). */
    static void onBoss(String healthLine) {
        if (healthLine == null) {
            killed = false;
            return;
        }
        if (killed) return;
        if (spawnedAt == 0) spawnedAt = System.currentTimeMillis();
        Matcher m = BOSS.matcher(healthLine);
        if (m.find()) boss = m.group(2) == null ? m.group(1) : m.group(1) + " " + m.group(2);
    }

    private static void onChat(String text) {
        switch (text) {
            case "SLAYER QUEST STARTED!" -> {
                reset();
                questStartedAt = System.currentTimeMillis();
            }
            case "NICE! SLAYER BOSS SLAIN!" -> onKill();
            case "SLAYER QUEST COMPLETE!" -> {
                onKill();
                SkyBallsConfig.Slayers config = config();
                if (config != null && config.questComplete && questStartedAt > 0) {
                    String took = format(System.currentTimeMillis() - questStartedAt);
                    SkyBallsAlerts.chat(config.compactTimes
                        ? Component.literal("Quest took ").withStyle(ChatFormatting.YELLOW).append(aqua(took)).append(yellow(" in total."))
                        : Component.literal("Slayer quest took ").withStyle(ChatFormatting.YELLOW).append(aqua(took)).append(yellow(" to complete.")));
                }
                questStartedAt = 0;
            }
            case "SLAYER QUEST FAILED!" -> reset();
            default -> {}
        }
    }

    private static void reset() {
        questStartedAt = 0;
        spawnedAt = 0;
        boss = null;
    }

    private static void onKill() {
        if (spawnedAt == 0 || boss == null) return;
        long time = System.currentTimeMillis() - spawnedAt;
        String name = boss;
        spawnedAt = 0;
        boss = null;
        killed = true;
        SkyBallsConfig.Slayers config = config();
        if (config == null) return;

        Map<String, Long> bests = BESTS.computeIfAbsent(player(), p -> new ConcurrentHashMap<>());
        Long previous = bests.get(name);
        boolean newBest = previous == null || time < previous;
        if (newBest) {
            bests.put(name, time);
            save();
            SlayerLeaderboard.onPersonalBest(name, time);
        }

        boolean compact = config.compactTimes;
        String took = format(time);
        if (config.timeToKill) {
            SkyBallsAlerts.chat(compact
                ? red(name).append(yellow(" took ")).append(aqua(took))
                : yellow("It took ").append(aqua(took)).append(yellow(" to kill ")).append(red(name)));
        }
        if (!config.personalBests) return;
        MutableComponent bold = Component.literal(compact ? "NEW PB! " : "NEW PERSONAL BEST! ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        if (newBest && previous == null) {
            SkyBallsAlerts.chat(compact
                ? bold.append(red(name)).append(yellow(" in ")).append(green(took))
                : bold.append(green(took)).append(yellow(" for ")).append(red(name)));
        } else if (newBest) {
            String old = format(previous);
            SkyBallsAlerts.chat(compact
                ? bold.append(red(name)).append(yellow(" in ")).append(Component.literal(old).withStyle(ChatFormatting.RED))
                    .append(yellow(" -> ")).append(green(took))
                : bold.append(green(took)).append(Component.literal(" (Previous " + old + ")").withStyle(ChatFormatting.GRAY))
                    .append(yellow(" for ")).append(red(name)));
        } else {
            SkyBallsAlerts.chat(red(name).append(yellow(compact ? " PB: " : " Personal best: "))
                .append(Component.literal(format(previous)).withStyle(ChatFormatting.GOLD)));
        }
    }

    /** Your personal bests: boss key ("Revenant Horror V") -> best kill time in ms. */
    public static Map<String, Long> personalBests() {
        return Map.copyOf(BESTS.getOrDefault(player(), Map.of()));
    }

    private static String player() {
        return Minecraft.getInstance().getUser().getName().toLowerCase(Locale.ROOT);
    }

    /** "34.52s", "1m 3.45s". */
    public static String format(long millis) {
        long minutes = millis / 60_000;
        double seconds = (millis % 60_000) / 1000.0;
        return minutes > 0 ? String.format(Locale.US, "%dm %.2fs", minutes, seconds) : String.format(Locale.US, "%.2fs", seconds);
    }

    private static MutableComponent yellow(String text) { return Component.literal(text).withStyle(ChatFormatting.YELLOW); }
    private static MutableComponent aqua(String text) { return Component.literal(text).withStyle(ChatFormatting.AQUA); }
    private static MutableComponent green(String text) { return Component.literal(text).withStyle(ChatFormatting.GREEN); }
    private static MutableComponent red(String text) { return Component.literal(text).withStyle(ChatFormatting.RED); }

    // ---------------------------------------------------------------- saving

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var player : root.entrySet()) {
                Map<String, Long> bests = new ConcurrentHashMap<>();
                for (var best : player.getValue().getAsJsonObject().entrySet()) bests.put(best.getKey(), best.getValue().getAsLong());
                BESTS.put(player.getKey(), bests);
            }
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not read slayer-personal-bests.json: " + e.getMessage());
        }
    }

    private static void save() {
        Map<String, Map<String, Long>> copy = new HashMap<>(BESTS);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(copy), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not save slayer-personal-bests.json: " + e.getMessage());
        }
    }
}
