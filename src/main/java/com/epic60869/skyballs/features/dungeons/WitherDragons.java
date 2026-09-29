// SPDX-License-Identifier: GPL-3.0-or-later
// Ported from NoFrills (https://github.com/WhatYouThing/NoFrills, GPL-3.0): features/dungeons/WitherDragons.java and
// RelicHighlight.java. Dragon boxes and priorities there are taken from Odin's WitherDragonEnum.
package com.epic60869.skyballs.features.dungeons;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.epic60869.skyballs.features.core.SkyBallsWorldRender;
import com.epic60869.skyballs.mixin.SkyBallsPlayerTabOverlayAccessor;
import com.epic60869.skyballs.sb.events.ServerTickCallback;
import com.epic60869.skyballs.sb.skyblock.dungeon.DungeonClass;
import com.epic60869.skyballs.sb.utils.render.primitive.PrimitiveCollector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dungeons > M7 Dragons and Relics: the Master Mode floor 7 dragon phase.
 * <ul>
 *     <li>Relic Highlight: the cauldron for the relic you're holding.</li>
 *     <li>Spawn alerts (with split priority on the first double spawn), spawn timers, kill areas, hitboxes, a tracer,
 *     stack waypoints, dragon health and an Ice Spray tracker.</li>
 * </ul>
 * A dragon starts spawning when the server sends its flame particles (20 flames at y 19 in its kill area); the entity
 * is matched to a dragon by the relic "collar" armour stand next to it.
 */
public final class WitherDragons {
    private static final Relic[] RELICS = {
        new Relic("Corrupted Green Relic", new BlockPos(49, 7, 44), 0x00FF00),
        new Relic("Corrupted Red Relic", new BlockPos(51, 7, 42), 0xFF0000),
        new Relic("Corrupted Purple Relic", new BlockPos(54, 7, 41), 0xAA00AA),
        new Relic("Corrupted Orange Relic", new BlockPos(57, 7, 42), 0xFFAA00),
        new Relic("Corrupted Blue Relic", new BlockPos(59, 7, 44), 0x55FFFF),
    };

    private static final Dragon RED = new Dragon("Red", 3, 3, "RED_KING_RELIC", 0xFF0000,
        AABB.ofSize(new Vec3(27.0, 14.0, 59.0), 1, 1, 1),
        List.of(
            new AABB(25.5, 14.0, 52.0, 28.5, 17.0, 55.0),
            new AABB(24.5, 14.0, 56.0, 29.5, 17.0, 61.0),
            new AABB(26.0, 15.5, 61.5, 28.0, 17.5, 67.5),
            new AABB(29.5, 16.0, 57.0, 33.5, 18.0, 61.0),
            new AABB(20.5, 16.0, 57.0, 24.5, 18.0, 61.0)),
        new AABB(14.5, 5, 45.5, 39.5, 28, 70.5));
    private static final Dragon ORANGE = new Dragon("Orange", 1, 5, "ORANGE_KING_RELIC", 0xFFAA00,
        AABB.ofSize(new Vec3(85.0, 14.0, 56.0), 1, 1, 1),
        List.of(
            new AABB(83.5, 14.0, 49.0, 86.5, 17.0, 52.0),
            new AABB(82.5, 14.0, 53.0, 87.5, 17.0, 58.0),
            new AABB(84.0, 15.5, 58.5, 86.0, 17.5, 64.5),
            new AABB(87.5, 16.0, 54.0, 91.5, 18.0, 58.0),
            new AABB(78.5, 16.0, 54.0, 82.5, 18.0, 58.0)),
        new AABB(72, 5, 47, 102, 28, 77));
    private static final Dragon BLUE = new Dragon("Blue", 4, 2, "BLUE_KING_RELIC", 0x55FFFF,
        AABB.ofSize(new Vec3(84.0, 14.0, 94.0), 1, 1, 1),
        List.of(
            new AABB(82.5, 14.0, 87.0, 85.5, 17.0, 90.0),
            new AABB(81.5, 14.0, 91.0, 86.5, 17.0, 96.0),
            new AABB(83.0, 15.5, 96.5, 85.0, 17.5, 102.5),
            new AABB(86.5, 16.0, 92.0, 90.5, 18.0, 96.0),
            new AABB(77.5, 16.0, 92.0, 81.5, 18.0, 96.0)),
        new AABB(71.5, 5, 82.5, 96.5, 26, 107.5));
    private static final Dragon PURPLE = new Dragon("Purple", 5, 1, "PURPLE_KING_RELIC", 0xAA00AA,
        AABB.ofSize(new Vec3(56.0, 14.0, 125.0), 1, 1, 1),
        List.of(
            new AABB(54.5, 14.0, 118.0, 57.5, 17.0, 121.0),
            new AABB(53.5, 14.0, 122.0, 58.5, 17.0, 127.0),
            new AABB(55.0, 15.5, 127.5, 57.0, 17.5, 133.5),
            new AABB(58.5, 16.0, 123.0, 62.5, 18.0, 127.0),
            new AABB(49.5, 16.0, 123.0, 53.5, 18.0, 127.0)),
        new AABB(45.5, 6, 113.5, 68.5, 23, 136.5));
    private static final Dragon GREEN = new Dragon("Green", 2, 4, "GREEN_KING_RELIC", 0x00FF00,
        AABB.ofSize(new Vec3(27.0, 14.0, 94.0), 1, 1, 1),
        List.of(
            new AABB(25.5, 14.0, 87.0, 28.5, 17.0, 90.0),
            new AABB(24.5, 14.0, 91.0, 29.5, 17.0, 96.0),
            new AABB(26.0, 15.5, 96.5, 28.0, 17.5, 102.5),
            new AABB(29.5, 16.0, 92.0, 33.5, 18.0, 96.0),
            new AABB(20.5, 16.0, 92.0, 24.5, 18.0, 96.0)),
        new AABB(7, 5, 80, 37, 28, 110));
    private static final List<Dragon> DRAGONS = List.of(RED, ORANGE, BLUE, PURPLE, GREEN);
    /** The M7 boss room; the dragon phase is the part of it below y 50. */
    private static final AABB BOSS_ROOM = new AABB(-8, 0, -8, 134, 254, 147);

    private static boolean splitDone;
    private static int tickCounter;

    private WitherDragons() {}

    private static FeatureConfigs.WitherDragons config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.dungeons.f7.witherDragons;
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reset());
        SkyBallsLocation.onAreaChange(area -> reset());
        ServerTickCallback.EVENT.register(WitherDragons::onServerTick);
        ClientTickEvents.END_CLIENT_TICK.register(WitherDragons::onClientTick);
        SkyBallsWorldRender.register(WitherDragons::render);
    }

    private static void reset() {
        splitDone = false;
        tickCounter = 0;
        for (Dragon d : DRAGONS) d.reset();
    }

    static boolean inDragonPhase() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getY() < 50 && PositionalMessages.onFloor7() && BOSS_ROOM.contains(mc.player.position());
    }

    /** Power blessing level from the tab footer, with Time counting half. */
    private static double power() {
        Component footer = ((SkyBallsPlayerTabOverlayAccessor) Minecraft.getInstance().gui.hud.getTabList()).skyballs$getFooter();
        if (footer == null) return 0;
        double power = 0;
        for (String line : SkyBallsLocation.strip(footer.getString()).split("\n")) {
            line = line.trim();
            if (line.startsWith("Blessing of Power")) power += roman(line.substring("Blessing of Power".length()).trim());
            else if (line.startsWith("Blessing of Time")) power += 0.5 * roman(line.substring("Blessing of Time".length()).trim());
        }
        return power;
    }

    private static int roman(String text) {
        int total = 0, previous = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            int value = switch (text.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                default -> 0;
            };
            if (value == 0) return 0;
            total += value < previous ? -value : value;
            previous = Math.max(previous, value);
        }
        return total;
    }

    private static boolean archerTeam() {
        DungeonClass c = SelfClass.get();
        return c == DungeonClass.ARCHER || c == DungeonClass.TANK;
    }

    private static Dragon higherPriority(Dragon first, Dragon second, boolean archerTeam) {
        if (archerTeam) return first.archPriority > second.archPriority ? first : second;
        return first.bersPriority > second.bersPriority ? first : second;
    }

    private static void announce(Dragon dragon, boolean priority) {
        SkyBallsAlerts.title(Component.literal(dragon.name.toUpperCase(Locale.ROOT) + " IS SPAWNING").setStyle(Style.EMPTY.withBold(true).withColor(dragon.colour)), null);
        SkyBallsAlerts.chat(Component.literal(dragon.name).withColor(dragon.colour)
            .append(Component.literal(priority ? " is your priority dragon." : " is spawning.").withStyle(ChatFormatting.GRAY)));
    }

    /** Called from SkyBallsWitherDragonsMixin for every particle packet, on the render thread. */
    public static void onParticle(ClientboundLevelParticlesPacket packet) {
        FeatureConfigs.WitherDragons config = config();
        if (config == null || !isDragonParticle(packet) || !inDragonPhase()) return;
        Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        for (Dragon dragon : DRAGONS) {
            if (dragon.spawnTicks != 0 || !dragon.area.contains(pos)) continue;
            dragon.spawnTicks = 100;
            List<Dragon> spawning = DRAGONS.stream().filter(Dragon::isSpawning).toList();
            if (!splitDone && spawning.size() == 2) {
                if (config.alert) {
                    Dragon first = spawning.getFirst(), second = spawning.getLast();
                    double power = power();
                    boolean purple = first == PURPLE || second == PURPLE;
                    boolean split = (purple && power >= config.powerEasy) || power >= config.power;
                    // No split: everyone goes to the Archer team's dragon.
                    announce(higherPriority(first, second, !split || archerTeam()), true);
                }
                splitDone = true;
            } else if (splitDone && config.alert) {
                announce(dragon, false);
            }
        }
    }

    private static boolean isDragonParticle(ClientboundLevelParticlesPacket p) {
        return p.getParticle().getType() == ParticleTypes.FLAME && p.getCount() == 20 && p.getY() == 19
            && p.getXDist() == 2f && p.getYDist() == 3f && p.getZDist() == 2f && p.getMaxSpeed() == 0f
            && p.getX() % 1 == 0 && p.getZ() % 1 == 0;
    }

    /** Called from SkyBallsWitherDragonsMixin after an entity's data (health included) is updated. */
    public static void onEntityData(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || config() == null || !inDragonPhase()) return;
        if (!(mc.level.getEntity(id) instanceof EnderDragon entity)) return;
        // Read the health now: the client resets it on the next tick.
        for (Dragon dragon : DRAGONS) {
            if (dragon.entity == entity) dragon.health = entity.getHealth();
        }
    }

    private static void onServerTick() {
        if (!inDragonPhase()) return;
        for (Dragon dragon : DRAGONS) dragon.tick();
        tickCounter++;
    }

    private static void onClientTick(Minecraft mc) {
        FeatureConfigs.WitherDragons config = config();
        if (config == null || mc.level == null || !inDragonPhase()) return;
        List<EnderDragon> dragons = new ArrayList<>();
        List<ArmorStand> stands = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EnderDragon d && d.isAlive()) dragons.add(d);
            else if (entity instanceof ArmorStand s) stands.add(s);
        }
        for (Dragon dragon : DRAGONS) {
            if (dragon.hasEntity()) continue;
            find:
            for (ArmorStand stand : stands) {
                if (!dragon.relicId.equals(Compat.neuName(stand.getItemBySlot(EquipmentSlot.HEAD)))) continue;
                for (EnderDragon entity : dragons) {
                    if (horizontalDistance(entity.position(), stand.position()) <= 10) {
                        dragon.setEntity(entity);
                        break find;
                    }
                }
            }
        }
        if (!config.trackIceSpray) return;
        for (ArmorStand stand : stands) {
            if (!isIceSpray(stand)) continue;
            for (Dragon dragon : DRAGONS) {
                if (!dragon.hasEntity() || dragon.iceSprayed) continue;
                EnderDragon entity = dragon.entity;
                if (horizontalDistance(entity.position(), stand.position()) < 2 && stand.getY() > entity.getY()) {
                    SkyBallsAlerts.chat(Component.literal(dragon.name).withColor(dragon.colour)
                        .append(Component.literal(" Ice Sprayed in " + (tickCounter - dragon.spawnedAt) + " tick(s).").withStyle(ChatFormatting.GRAY)));
                    dragon.iceSprayed = true;
                    break;
                }
            }
        }
    }

    private static boolean isIceSpray(ArmorStand stand) {
        if (!stand.isMarker()) return false;
        ItemStack item = stand.getItemBySlot(EquipmentSlot.MAINHAND);
        return item.is(Items.PACKED_ICE) && item.getCount() == 1 && Compat.neuName(item).isEmpty();
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void render(PrimitiveCollector collector) {
        FeatureConfigs.WitherDragons config = config();
        if (config == null || !inDragonPhase()) return;
        Minecraft mc = Minecraft.getInstance();
        if (config.relicHighlight && mc.player != null) {
            ItemStack stack = mc.player.getInventory().getItem(8);
            if (stack.is(Items.PLAYER_HEAD)) {
                String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
                for (Relic relic : RELICS) {
                    if (relic.name.equals(name)) collector.submitFilledBox(relic.pos, rgb(relic.colour), 0.5f, false);
                }
            }
        }
        for (Dragon dragon : DRAGONS) {
            float[] colour = rgb(dragon.colour);
            if (config.timer && dragon.isSpawning()) {
                float seconds = dragon.spawnTicks / 20f;
                collector.submitText(Component.literal(String.format(Locale.ROOT, "%.2fs", seconds)).withColor(percentColour(seconds / 5f)),
                    dragon.pos.getCenter().add(0, 4, 0), 3f, true);
            }
            if (config.boxes && (dragon.isSpawning() || dragon.hasEntity())) {
                collector.submitOutlinedBox(dragon.area, colour, 1f, 3f, true);
            }
            if (config.hitboxes && dragon.hasEntity()) {
                for (EnderDragonPart part : dragon.entity.getSubEntities()) {
                    collector.submitOutlinedBox(part.getBoundingBox(), colour, 1f, 2f, false);
                }
            }
            if (dragon.isSpawning()) {
                switch (config.waypoints) {
                    case SIMPLE -> collector.submitFilledBox(dragon.pos, colour, 0.5f, false);
                    case ADVANCED -> {
                        for (AABB part : dragon.parts) collector.submitFilledBox(part, colour, 0.33f, false);
                    }
                    default -> {}
                }
            }
            if (config.health && dragon.hasEntity()) {
                double max = dragon.maxHealth > 0 ? dragon.maxHealth : 200;
                collector.submitText(Component.literal(String.format(Locale.ROOT, "%.2fM", dragon.health / 1_000_000.0)).withColor(percentColour((float) (dragon.health / max))),
                    dragon.entity.position(), 3f, true);
            }
        }
        if (config.tracers) {
            List<Dragon> spawning = DRAGONS.stream().filter(Dragon::isSpawning).toList();
            if (!spawning.isEmpty()) {
                Dragon dragon = spawning.size() == 2 ? higherPriority(spawning.getFirst(), spawning.get(1), archerTeam()) : spawning.getFirst();
                collector.submitLineFromCursor(dragon.pos.getCenter(), rgb(dragon.colour), 1f, 2f);
            }
        }
    }

    /** Green when full, through yellow, to red when empty. */
    private static int percentColour(float fraction) {
        return Mth.hsvToRgb(Mth.clamp(fraction, 0f, 1f) / 3f, 1f, 1f);
    }

    private static float[] rgb(int colour) {
        return new float[]{((colour >> 16) & 255) / 255f, ((colour >> 8) & 255) / 255f, (colour & 255) / 255f};
    }

    private record Relic(String name, BlockPos pos, int colour) {}

    private static final class Dragon {
        final String name;
        final int archPriority;
        final int bersPriority;
        final String relicId;
        final int colour;
        final AABB pos;
        final List<AABB> parts;
        final AABB area;
        EnderDragon entity;
        float health;
        double maxHealth = 200;
        int spawnTicks;
        int spawnedAt;
        boolean iceSprayed;

        Dragon(String name, int archPriority, int bersPriority, String relicId, int colour, AABB pos, List<AABB> parts, AABB area) {
            this.name = name;
            this.archPriority = archPriority;
            this.bersPriority = bersPriority;
            this.relicId = relicId;
            this.colour = colour;
            this.pos = pos;
            this.parts = parts;
            this.area = area;
        }

        boolean isSpawning() {
            return spawnTicks > 0;
        }

        boolean hasEntity() {
            return entity != null && entity.isAlive() && !entity.isRemoved();
        }

        void setEntity(EnderDragon dragon) {
            entity = dragon;
            health = dragon.getHealth();
            maxHealth = dragon.getAttributeBaseValue(Attributes.MAX_HEALTH);
            if (spawnedAt == 0) spawnedAt = tickCounter;
        }

        void tick() {
            if (spawnTicks > 0) spawnTicks--;
            if (!hasEntity()) {
                entity = null;
                iceSprayed = false;
                spawnedAt = 0;
            }
        }

        void reset() {
            entity = null;
            iceSprayed = false;
            health = 0;
            maxHealth = 200;
            spawnTicks = 0;
            spawnedAt = 0;
        }
    }
}
