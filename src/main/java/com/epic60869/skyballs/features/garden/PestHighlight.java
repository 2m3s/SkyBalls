package com.epic60869.skyballs.features.garden;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsTabWidgetManager;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.epic60869.skyballs.features.core.SkyBallsWorldRender;
import com.epic60869.skyballs.features.sbc.Flags;
import com.epic60869.skyballs.features.sbc.Sbc;
import com.epic60869.skyballs.features.sbc.SbcConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pest highlight, in the Garden only: pests (an invisible Silverfish or Bat under a "Beetle" name line)
 * get an outline in your colour, optionally with a line from your crosshair and a beam on the nearest one, and a HUD
 * shows how many are alive and on which plots (from the tab list). Written for SkyBalls; everywhere else it's off.
 */
public final class PestHighlight {
    private static final Pattern ALIVE = Pattern.compile("Alive: (\\d+)");
    private static final Pattern PLOTS = Pattern.compile("(?:Infested )?Plots?: (.+)");
    private static final Pattern SCOREBOARD_PESTS = Pattern.compile("[ൠ\\uE07F\\uE018] x(\\d+)");

    /** Entity ids of pest bodies this tick. */
    private static volatile Set<Integer> pests = Set.of();
    private static volatile List<Vec3> positions = List.of();
    private static String alive = "";
    private static String plots = "";
    private static int ticks;

    private PestHighlight() {}

    private static SbcConfig.PestHighlight config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? new SbcConfig.PestHighlight() : c.farming.pestHighlight;
    }

    private static boolean active() {
        return config().enabled && Flags.isEnabled("pestHighlight") && SkyBallsLocation.inGarden();
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 5 != 0) return;
            if (mc.level == null || !active()) {
                if (!pests.isEmpty()) {
                    pests = Set.of();
                    positions = List.of();
                }
                return;
            }
            scan(mc);
            readTab();
        });
        SkyBallsWorldRender.register(collector -> {
            if (!active() || positions.isEmpty()) return;
            SbcConfig.PestHighlight c = config();
            if (!c.tracer && !c.beacon) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            Vec3 eye = mc.player.getEyePosition();
            Vec3 nearest = null;
            for (Vec3 p : positions) if (nearest == null || p.distanceToSqr(eye) < nearest.distanceToSqr(eye)) nearest = p;
            int colour = Sbc.colour(c.colour, 0xFFFF55FF);
            float[] rgb = {(colour >> 16 & 255) / 255f, (colour >> 8 & 255) / 255f, (colour & 255) / 255f};
            if (c.tracer) collector.submitLineFromCursor(nearest.add(0, 0.3, 0), rgb, 1f, 2f);
            if (c.beacon) collector.submitFilledBoxWithBeaconBeam(new AABB(nearest.subtract(0.3, 0, 0.3), nearest.add(0.3, 0.6, 0.3)), rgb, 0.4f, false);
        });
        com.epic60869.skyballs.features.core.SkyBallsHuds.setting("pestHighlight", () -> config().enabled && config().hud);
        SkyBallsHuds.register("pestHighlight", "Pests", () -> active() && config().hud, PestHighlight::hudLines,
            List.of(Component.literal("Pests: ").withStyle(ChatFormatting.GOLD).append(Component.literal("3 alive · Plots 2, 7").withStyle(ChatFormatting.WHITE))), 8, 60);
    }

    /** SkyHanni's PestType names. */
    private static final Pattern PEST_NAME = Pattern.compile("\\b(Beetle|Cricket|Earthworm|Field Mouse|Fly|Locust|Lunar Moth|Mite|Mosquito|Moth|Rat|Slug|Praying Mantis|Firefly|Dragonfly)\\b");

    /**
     * As SkyHanni's MobFinder.tryAddGarden: a pest is an invisible Silverfish or Bat with the pest's name above it.
     * The name line is an armor stand or (as Hypixel now draws nametags) a text display, and its icon is a private-use
     * glyph now rather than ൠ, so the name itself is matched. The head the pest wears (an armor stand) is outlined too.
     */
    private static void scan(Minecraft mc) {
        Vec3 me = mc.player.position();
        AABB area = new AABB(me, me).inflate(96);
        List<Entity> tags = new ArrayList<>();
        List<ArmorStand> heads = new ArrayList<>();
        List<Entity> bodies = new ArrayList<>();
        for (Entity entity : mc.level.getEntities((Entity) null, area, e -> true)) {
            if (entity instanceof Silverfish || entity instanceof Bat) {
                bodies.add(entity);
                continue;
            }
            Component tag = nametag(entity);
            if (tag != null && PEST_NAME.matcher(SkyBallsLocation.strip(tag.getString())).find()) tags.add(entity);
            else if (entity instanceof ArmorStand stand && stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD)) heads.add(stand);
        }
        Set<Integer> found = new HashSet<>();
        List<Vec3> where = new ArrayList<>();
        for (Entity body : bodies) {
            boolean named = false;
            for (Entity tag : tags) {
                double dx = tag.getX() - body.getX(), dz = tag.getZ() - body.getZ(), dy = tag.getY() - body.getY();
                if (Math.abs(dx) <= 1.5 && Math.abs(dz) <= 1.5 && dy >= -0.5 && dy <= 3.5) {
                    named = true;
                    break;
                }
            }
            if (!named) continue;
            found.add(body.getId());
            for (ArmorStand head : heads) {
                if (head.distanceToSqr(body) <= 2.0 * 2.0) found.add(head.getId());
            }
            where.add(body.position().add(0, body.getBbHeight() * 0.5, 0));
        }
        pests = Set.copyOf(found);
        positions = List.copyOf(where);
    }

    /** A nametag's text: an armor stand's custom name or a text display's text. */
    private static Component nametag(Entity entity) {
        if (entity instanceof ArmorStand stand) return stand.getCustomName();
        if (entity instanceof Display.TextDisplay display) return display.getEntityData().get(Display.TextDisplay.DATA_TEXT_ID);
        return null;
    }

    private static void readTab() {
        String newAlive = "";
        String newPlots = "";
        boolean inPests = false;
        for (PlayerInfo info : SkyBallsTabWidgetManager.players()) {
            Component raw = Compat.rawTabName(info);
            if (raw == null) continue;
            String text = SkyBallsLocation.strip(raw.getString()).trim();
            if (text.startsWith("Pests")) inPests = true;
            if (!inPests) continue;
            Matcher a = ALIVE.matcher(text);
            if (a.find()) newAlive = a.group(1);
            Matcher p = PLOTS.matcher(text);
            if (p.find()) newPlots = p.group(1).trim();
            if (text.isEmpty() && !newAlive.isEmpty()) break;
        }
        if (newAlive.isEmpty()) {
            for (String line : SkyBallsLocation.scoreboard()) {
                Matcher m = SCOREBOARD_PESTS.matcher(line);
                if (m.find() && line.contains("Garden")) newAlive = m.group(1);
            }
        }
        alive = newAlive;
        plots = newPlots;
    }

    private static List<Component> hudLines() {
        String count = !alive.isEmpty() ? alive : String.valueOf(positions.size());
        if ("0".equals(count) && plots.isEmpty()) return List.of(Component.literal("Pests: ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("none").withStyle(ChatFormatting.GREEN)));
        return List.of(Component.literal("Pests: ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(count + " alive" + (plots.isEmpty() || plots.equalsIgnoreCase("none") ? "" : " · Plots " + plots)
                + (positions.isEmpty() ? "" : " · " + positions.size() + " nearby")).withStyle(ChatFormatting.WHITE)));
    }

    /** Whether the entity should be outlined (SkyBallsPestGlowMixin). */
    public static boolean isPest(Entity entity) {
        return !pests.isEmpty() && pests.contains(entity.getId()) && active();
    }

    public static int colour() {
        return Sbc.colour(config().colour, 0xFFFF55FF) & 0xFFFFFF;
    }
}
