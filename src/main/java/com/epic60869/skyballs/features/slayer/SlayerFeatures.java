package com.epic60869.skyballs.features.slayer;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Slayer boss phase display: your boss's health (or hits while Voidgloom's shield is up) from its nametag, in a HUD,
 * with your Tarantula's egg sacs (time left and hits) under it. The same scan times your kills for {@link SlayerTimes}.
 */
public final class SlayerFeatures {
    /** How far from you your own boss is looked for. */
    private static final double SEARCH_RANGE = 48;
    /** How far from you a Tarantula's egg sacs are looked for. */
    private static final double SACK_RANGE = 20;

    private static final Pattern EGG_SAC = Pattern.compile("\\d+s \\d+/\\d+");

    private static List<Component> bossLines = List.of();

    private static int ticks;

    private SlayerFeatures() {}

    private static FeatureConfigs.Slayer config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.slayers.huds;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 2 == 0) tick(mc);
        });

        SkyBallsHuds.register("slayer_phase", "Slayer Boss Phase",
            () -> config() != null && config().phaseDisplay,
            () -> bossLines,
            List.of(Component.literal("☠ Voidgloom Seraph 45M❤").withStyle(ChatFormatting.RED)),
            8, 220);
    }

    private static void tick(Minecraft mc) {
        FeatureConfigs.Slayer config = config();
        boolean needed = (config != null && config.phaseDisplay) || SlayerTimes.enabled();
        if (mc.player == null || mc.level == null || !needed || !SkyBallsLocation.onSkyblock()) {
            bossLines = List.of();
            SlayerTimes.onBoss(null);
            return;
        }
        updateBossLines(mc);
    }

    /**
     * A nametag's text as the server sent it, from an armor stand's custom name or (as Hypixel now uses) a text
     * display. The text display's text is read from its synced data rather than getText(), which the nickname
     * mixin rewrites: with a nickname set, "Spawned by: you" would otherwise never match your username.
     */
    static Component nametag(Entity entity) {
        if (entity instanceof ArmorStand stand && stand.hasCustomName()) return stand.getCustomName();
        if (entity instanceof Display.TextDisplay display) return display.getEntityData().get(Display.TextDisplay.DATA_TEXT_ID);
        return null;
    }

    private static boolean isOwnerTag(String text, String name) {
        for (String line : SkyBallsLocation.strip(text).split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Spawned by:")
                && trimmed.substring("Spawned by:".length()).trim().toLowerCase(Locale.ROOT).equals(name)) return true;
        }
        return false;
    }

    /**
     * Nametag lines of your own boss: the nametags stacked around the "Spawned by: you" line. Hypixel draws them
     * with armor stands or text displays, and a text display can hold several lines.
     */
    private static void updateBossLines(Minecraft mc) {
        String name = mc.player.getGameProfile().name().toLowerCase(Locale.ROOT);
        double rangeSq = SEARCH_RANGE * SEARCH_RANGE;
        Entity owner = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.distanceToSqr(mc.player) > rangeSq) continue;
            Component tag = nametag(entity);
            if (tag != null && isOwnerTag(tag.getString(), name)) {
                owner = entity;
                break;
            }
        }
        if (owner == null) {
            bossLines = List.of();
            SlayerTimes.onBoss(null);
            return;
        }
        AABB area = owner.getBoundingBox().inflate(1.5, 3, 1.5);
        List<Entity> tags = mc.level.getEntities((Entity) null, area, e -> nametag(e) != null);
        tags.sort(Comparator.comparingDouble((Entity e) -> e.getY()).reversed());
        // Only the boss's own health line: the other tags around it are pets and floating damage numbers.
        List<Component> lines = new ArrayList<>();
        for (Entity tagEntity : tags) {
            for (Component line : splitLines(nametag(tagEntity))) {
                if (isBossHealth(SkyBallsLocation.strip(line.getString()))) {
                    lines.add(line);
                    break;
                }
            }
            if (!lines.isEmpty()) break;
        }
        String health = lines.isEmpty() ? "" : SkyBallsLocation.strip(lines.get(0).getString());
        SlayerTimes.onBoss(health);
        if (isTarantula(health)) addEggSacLines(mc, lines);
        bossLines = lines;
    }

    private static boolean isTarantula(String text) {
        return text.contains("Tarantula") || text.contains("Brood");
    }

    /**
     * Your Tarantula's egg sacs, nearest first, under its health line. As in SkyHanni's SpiderEggSacHighlighter, an
     * egg sac is a "5s 1/3" nametag (time left, hits taken/needed) next to a "SHOOT ME!" one.
     */
    private static void addEggSacLines(Minecraft mc, List<Component> lines) {
        List<Entity> tags = mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(SACK_RANGE), e -> nametag(e) != null);
        List<Entity> shootMe = tags.stream()
            .filter(e -> SkyBallsLocation.strip(nametag(e).getString()).trim().equalsIgnoreCase("SHOOT ME!")).toList();
        tags.sort(Comparator.comparingDouble(mc.player::distanceToSqr));
        MutableComponent row = null;
        for (Entity tagEntity : tags) {
            Component tag = nametag(tagEntity);
            if (!EGG_SAC.matcher(SkyBallsLocation.strip(tag.getString()).trim()).matches()) continue;
            if (shootMe.stream().noneMatch(e -> e.distanceToSqr(tagEntity) < 2.5 * 2.5)) continue;
            // Two egg sacs per line.
            if (row == null) {
                row = Component.literal("Egg Sac ").withStyle(ChatFormatting.YELLOW).append(tag);
            } else {
                lines.add(row.append(Component.literal("   Egg Sac ").withStyle(ChatFormatting.YELLOW)).append(tag));
                row = null;
            }
        }
        if (row != null) lines.add(row);
    }

    private static final List<String> BOSSES = List.of("Revenant Horror", "Atoned Horror", "Tarantula Broodfather",
        "Conjoined Brood", "Sven Packmaster", "Voidgloom Seraph", "Inferno Demonlord", "Riftstalker Bloodfiend");

    /** "☠ Voidgloom Seraph IV 45M❤" (or "... 15 Hits" while its shield is up). */
    private static boolean isBossHealth(String text) {
        if (!text.contains("❤") && !text.contains(" Hits")) return false;
        if (text.contains("☠")) return true;
        for (String boss : BOSSES) if (text.contains(boss)) return true;
        return false;
    }

    /** Splits a nametag on its line breaks, keeping each part's colours. */
    static List<Component> splitLines(Component tag) {
        List<Component> lines = new ArrayList<>();
        MutableComponent[] current = {Component.empty()};
        tag.visit((style, value) -> {
            String[] parts = value.split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    lines.add(current[0]);
                    current[0] = Component.empty();
                }
                if (!parts[i].isEmpty()) current[0].append(Component.literal(parts[i]).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        lines.add(current[0]);
        return lines;
    }
}
