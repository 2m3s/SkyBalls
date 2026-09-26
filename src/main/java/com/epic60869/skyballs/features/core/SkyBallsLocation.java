package com.epic60869.skyballs.features.core;

import com.epic60869.skyballs.SkyBallsTabWidgetManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where the player is in SkyBlock, read from the sidebar scoreboard and the tab list.
 * Updated twice a second; listeners are told when the tab-list area changes.
 */
public final class SkyBallsLocation {
    private static final Pattern FLOOR = Pattern.compile("Catacombs \\((?<floor>[FM]\\d|E)\\)");
    /** Hypixel's "[player] entered (MM) The Catacombs, Floor VII!" / "... The Catacombs, Entrance!" when a run starts. */
    private static final Pattern ENTERED = Pattern.compile("entered (MM )?The Catacombs, (?:Floor (?<roman>[IV]+)|(?<entrance>Entrance))!");
    private static final Pattern GLACITE = Pattern.compile("Glacite Tunnels|Dwarven Base Camp|Great Glacite Lake|Fossil Research Center");
    private static final List<Consumer<String>> AREA_LISTENERS = new CopyOnWriteArrayList<>();

    private static List<String> scoreboard = List.of();
    private static List<String> teamLines = List.of();
    /** Floor from the dungeon's "entered" chat message, only while still in that world. */
    private static String chatFloor = "";
    private static java.lang.ref.WeakReference<Object> chatFloorWorld = new java.lang.ref.WeakReference<>(null);
    private static String scoreboardTitle = "";
    private static String area = "";
    private static String location = "";
    private static String floor = "";
    private static boolean onSkyblock;
    private static int ticks;

    private SkyBallsLocation() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 10 == 0) update(mc);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> reset());
        SkyBallsChat.onChat(message -> {
            Matcher m = ENTERED.matcher(message.text());
            if (!m.find()) return;
            chatFloor = m.group("entrance") != null ? "E" : (m.group(1) != null ? "M" : "F") + roman(m.group("roman"));
            chatFloorWorld = new java.lang.ref.WeakReference<>(Minecraft.getInstance().level);
        });
    }

    private static int roman(String numeral) {
        return switch (numeral) {
            case "I" -> 1; case "II" -> 2; case "III" -> 3; case "IV" -> 4; case "V" -> 5; case "VI" -> 6; case "VII" -> 7;
            default -> 0;
        };
    }

    /** Called when the tab-list area changes (e.g. "Garden", "Catacombs", "Crystal Hollows"). */
    public static void onAreaChange(Consumer<String> listener) {
        AREA_LISTENERS.add(listener);
    }

    private static void reset() {
        scoreboard = List.of();
        teamLines = List.of();
        chatFloor = "";
        scoreboardTitle = "";
        location = "";
        floor = "";
        onSkyblock = false;
        setArea("");
    }

    private static void update(Minecraft mc) {
        if (mc.level == null) {
            if (onSkyblock || !area.isEmpty()) reset();
            return;
        }

        Scoreboard board = mc.level.getScoreboard();
        Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        // Scoreboard mods (CustomScoreboard, ...) can take the sidebar out of its display slot to draw their own;
        // the SkyBlock objective is still there, so find it directly.
        if (objective == null) objective = findSidebarObjective(board);
        List<String> lines = new ArrayList<>();
        if (objective != null) {
            scoreboardTitle = strip(objective.getDisplayName().getString());
            List<PlayerScoreEntry> entries = new ArrayList<>(board.listPlayerScores(objective));
            entries.removeIf(PlayerScoreEntry::isHidden);
            entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner));
            for (PlayerScoreEntry entry : entries) {
                PlayerTeam team = board.getPlayersTeam(entry.owner());
                // Hypixel pads lines with emoji (outside the basic plane) between the team prefix and suffix.
                lines.add(strip(PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString()).replaceAll("[\\x{10000}-\\x{10FFFF}]", "").trim());
            }
        } else {
            scoreboardTitle = "";
        }
        scoreboard = List.copyOf(lines);
        // Every sidebar line is a team's prefix + suffix. Reading the teams directly works even when a scoreboard
        // mod hides or replaces the sidebar objective, like NoammAddons does.
        List<String> teams = new ArrayList<>();
        for (PlayerTeam team : board.getPlayerTeams()) {
            String text = strip(team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()).replaceAll("[\\x{10000}-\\x{10FFFF}]", "").trim();
            if (!text.isEmpty()) teams.add(text);
        }
        teamLines = List.copyOf(teams);
        onSkyblock = scoreboardTitle.contains("SKYBLOCK") || scoreboardTitle.contains("SKIBLOCK");

        String newLocation = "";
        String newFloor = "";
        for (String line : teams) {
            Matcher m = FLOOR.matcher(line);
            if (m.find() && !line.contains("Queue")) newFloor = m.group("floor");
        }
        for (String line : lines) {
            int symbol = Math.max(line.indexOf('⏣'), line.indexOf('ф'));
            if (symbol >= 0) newLocation = line.substring(symbol + 1).trim();
            Matcher m = FLOOR.matcher(line);
            if (m.find() && !line.contains("Queue")) newFloor = m.group("floor");
        }
        location = newLocation;
        if (newFloor.isEmpty() && !chatFloor.isEmpty() && mc.level == chatFloorWorld.get()) newFloor = chatFloor;
        floor = newFloor;

        String newArea = "";
        for (PlayerInfo info : SkyBallsTabWidgetManager.players()) {
            Component name = com.epic60869.skyballs.custom.util.Compat.rawTabName(info);
            if (name == null) continue;
            String text = strip(name.getString()).trim();
            if (text.startsWith("Area: ")) { newArea = text.substring(6).trim(); break; }
            if (text.startsWith("Dungeon: ")) { newArea = text.substring(9).trim(); break; }
        }
        setArea(newArea);
    }

    /** The SkyBlock sidebar objective when nothing is in the sidebar slot: the one titled SKYBLOCK, else the biggest. */
    private static Objective findSidebarObjective(Scoreboard board) {
        Objective best = null;
        int bestSize = 0;
        for (Objective candidate : board.getObjectives()) {
            String title = strip(candidate.getDisplayName().getString());
            if (title.contains("SKYBLOCK") || title.contains("SKIBLOCK")) return candidate;
            int size = board.listPlayerScores(candidate).size();
            if (size > bestSize) {
                best = candidate;
                bestSize = size;
            }
        }
        return best;
    }

    private static void setArea(String newArea) {
        if (newArea.equals(area)) return;
        area = newArea;
        for (Consumer<String> listener : AREA_LISTENERS) {
            try {
                listener.accept(newArea);
            } catch (Exception ignored) {}
        }
    }

    public static String strip(String text) {
        String stripped = ChatFormatting.stripFormatting(text);
        return stripped == null ? "" : stripped;
    }

    public static boolean onSkyblock() { return onSkyblock; }
    public static String area() { return area; }
    public static String location() { return location; }
    public static List<String> scoreboard() { return scoreboard; }
    /** The sidebar lines read from the scoreboard teams (prefix + suffix), found even when the sidebar is hidden. */
    public static List<String> teamLines() { return teamLines; }
    /** Floor such as "F7" or "M7", or "" outside dungeons. */
    public static String dungeonFloor() { return floor; }

    public static boolean inDungeon() {
        return area.equals("Catacombs") || !floor.isEmpty();
    }

    public static boolean inGarden() { return area.equals("Garden"); }
    public static boolean inCrystalHollows() { return area.equals("Crystal Hollows"); }
    public static boolean inDwarvenMines() { return area.equals("Dwarven Mines"); }
    public static boolean inMineshaft() { return area.equals("Mineshaft"); }
    public static boolean inGlaciteTunnels() { return inDwarvenMines() && GLACITE.matcher(location).find(); }
    public static boolean inMiningIsland() { return inDwarvenMines() || inCrystalHollows() || inMineshaft(); }

    public static boolean areaIs(String name) {
        return area.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT));
    }

    /** First scoreboard line that starts with {@code prefix}, without the prefix, or null. */
    public static String scoreboardValue(String prefix) {
        for (String line : scoreboard) {
            if (line.startsWith(prefix)) return line.substring(prefix.length()).trim();
        }
        return null;
    }
}
