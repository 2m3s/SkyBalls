package com.epic60869.skyballs.features.slayer;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Slayer miniboss alert: a title, a ding and a chat line when a slayer miniboss (Revenant Champion, Tarantula
 * Beast, Voidcrazed Maniac, ...) appears near you during a slayer quest. Minibosses are found by their nametags,
 * like the boss in {@link SlayerFeatures}; each one alerts once.
 */
public final class SlayerMinibossAlert {
    /** Minibosses further away than this are someone else's. */
    private static final double RANGE = 20;
    private static final List<String> MINIBOSSES = List.of(
        "Revenant Sycophant", "Revenant Champion", "Deformed Revenant", "Atoned Champion", "Atoned Revenant",
        "Tarantula Vermin", "Tarantula Beast", "Mutant Tarantula", "Primordial Jockey", "Primordial Viscount",
        "Pack Enforcer", "Sven Follower", "Sven Alpha",
        "Voidling Devotee", "Voidling Radical", "Voidcrazed Maniac",
        "Flare Demon", "Kindleheart Demon", "Burningsoul Demon");

    /** Nametag entities already alerted for; ones that are gone are dropped each scan. */
    private static final Set<Integer> ALERTED = new HashSet<>();
    private static int ticks;

    private SlayerMinibossAlert() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 5 == 0) scan(mc);
        });
    }

    private static boolean enabled() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c != null && c.slayers.minibossAlert;
    }

    /** The scoreboard shows "Slayer Quest" from starting a quest until it's done. */
    private static boolean onSlayerQuest() {
        for (String line : SkyBallsLocation.scoreboard()) if (line.contains("Slayer Quest")) return true;
        return false;
    }

    private static void scan(Minecraft mc) {
        if (mc.player == null || mc.level == null || !enabled() || !SkyBallsLocation.onSkyblock() || !onSlayerQuest()) {
            ALERTED.clear();
            return;
        }
        Set<Integer> present = new HashSet<>();
        List<Entity> tags = mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(RANGE),
            e -> SlayerFeatures.nametag(e) != null);
        for (Entity entity : tags) {
            String miniboss = miniboss(SlayerFeatures.nametag(entity));
            if (miniboss == null) continue;
            present.add(entity.getId());
            if (ALERTED.contains(entity.getId())) continue;
            SkyBallsAlerts.title(Component.literal("MINIBOSS!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.literal(miniboss).withStyle(ChatFormatting.GOLD));
            SkyBallsAlerts.chat(Component.literal("Miniboss spawned: ").withStyle(ChatFormatting.RED)
                .append(Component.literal(miniboss).withStyle(ChatFormatting.GOLD)));
        }
        ALERTED.retainAll(present);
        ALERTED.addAll(present);
    }

    /** The miniboss a nametag belongs to ("☠ Revenant Champion 180k❤" -> "Revenant Champion"), or null. */
    private static String miniboss(Component tag) {
        for (Component line : SlayerFeatures.splitLines(tag)) {
            String text = SkyBallsLocation.strip(line.getString());
            if (!text.contains("❤")) continue;
            for (String name : MINIBOSSES) if (text.contains(name)) return name;
        }
        return null;
    }
}
