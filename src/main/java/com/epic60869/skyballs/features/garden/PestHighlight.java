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
import net.minecraft.world.entity.decoration.ArmorStand;
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
 * Pest highlight, in the Garden only: pests (an armor stand wearing the pest's head, under a "ൠ Beetle" name line)
 * get an outline in your colour, optionally with a line from your crosshair and a beam on the nearest one, and a HUD
 * shows how many are alive and on which plots (from the tab list). Written for SkyBalls; everywhere else it's off.
 */
public final class PestHighlight {
    private static final String PEST_SYMBOL = "ൠ";
    private static final Pattern ALIVE = Pattern.compile("Alive: (\\d+)");
    private static final Pattern PLOTS = Pattern.compile("(?:Infested )?Plots?: (.+)");
    private static final Pattern SCOREBOARD_PESTS = Pattern.compile("ൠ x(\\d+)");

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

    private static void scan(Minecraft mc) {
        Vec3 me = mc.player.position();
        List<ArmorStand> labels = new ArrayList<>();
        List<ArmorStand> heads = new ArrayList<>();
        AABB area = new AABB(me, me).inflate(96);
        for (ArmorStand stand : mc.level.getEntitiesOfClass(ArmorStand.class, area, e -> true)) {
            Component name = stand.getCustomName();
            if (name != null && name.getString().contains(PEST_SYMBOL)) labels.add(stand);
            else if (stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.PLAYER_HEAD)) heads.add(stand);
        }
        Set<Integer> found = new HashSet<>();
        List<Vec3> where = new ArrayList<>();
        for (ArmorStand label : labels) {
            // The pest's body is the head-wearing stand right below its name line.
            ArmorStand body = null;
            double best = Double.MAX_VALUE;
            for (ArmorStand head : heads) {
                double dx = head.getX() - label.getX(), dz = head.getZ() - label.getZ(), dy = label.getY() - head.getY();
                if (Math.abs(dx) > 1.5 || Math.abs(dz) > 1.5 || dy < -0.5 || dy > 3.5) continue;
                double d = dx * dx + dz * dz + dy * dy;
                if (d < best) {
                    best = d;
                    body = head;
                }
            }
            Entity target = body == null ? label : body;
            found.add(target.getId());
            where.add(body == null ? label.position().add(0, -1.5, 0) : body.position().add(0, body.getBbHeight() * 0.8, 0));
        }
        pests = Set.copyOf(found);
        positions = List.copyOf(where);
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
