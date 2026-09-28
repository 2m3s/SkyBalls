package com.epic60869.skyballs.features.slayer;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsPriceTooltip;
import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.features.combat.CombatFeatures;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Profit per slayer boss: after each kill, "Profit: -10k" in chat, with a hover listing each drop and its value, the
 * drops' total, the quest's cost and what's left. Drops are what came into your inventory from your boss spawning to
 * 5 seconds after the kill, plus what went straight into your sacks (the next "[Sacks]" message, up to 30 seconds
 * later). The cost is what your purse went down by when the quest started, or Hypixel's price for that boss and tier.
 */
public final class SlayerBossProfit {
    private static final long INVENTORY_WINDOW_MS = 5_000L;
    private static final long SACKS_WINDOW_MS = 30_000L;
    /** "+64 Revenant Flesh (Combat Sack)" in a "[Sacks]" message's hover. */
    private static final Pattern SACK_CHANGE = Pattern.compile("^ *([+-])([0-9,]+) (.+?) [(].*[)] *$");
    private static final Pattern NUMBER = Pattern.compile("[\\d,.]+");

    /** Inventory when your boss spawned: item price id -> count. */
    private static Map<String, Integer> baseline;
    /** The kill being totted up, or null. */
    private static Kill kill;
    /** The "[Sacks]" message after the kill has come. */
    private static boolean sacksIn;

    private static long questCost = -1;
    private static double purseBeforeQuest = -1;
    private static long questStartedAt;
    private static double lastPurse = -1;

    private record Kill(String boss, long at, Map<String, Integer> baseline, Map<String, Long> sacks, long cost) {}

    private SlayerBossProfit() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++inventoryTicks % 5 != 0) return;
            tick();
            checkInventory();
        });
        SkyBallsChat.onGameMessage((component, overlay) -> {
            if (!overlay) onMessage(component);
        });
    }

    static boolean enabled() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c != null && c.slayers.bossProfit;
    }

    // ---------------------------------------------------------------- the quest's cost

    private static void onMessage(Component component) {
        String text = SkyBallsLocation.strip(component.getString()).trim();
        if (text.equals("SLAYER QUEST STARTED!")) {
            // The purse on the scoreboard goes down a moment after this message.
            purseBeforeQuest = lastPurse;
            questStartedAt = System.currentTimeMillis();
            questCost = -1;
        } else if (text.startsWith("[Sacks]") && kill != null) {
            for (Map.Entry<String, Long> e : sackGains(component).entrySet()) {
                if (e.getValue() > 0) kill.sacks().merge(e.getKey(), e.getValue(), Long::sum);
            }
            // Sack drops are in: finish once the inventory has been checked too.
            if (System.currentTimeMillis() - kill.at() > 1_000L) {
                sacksIn = true;
                if (kill.baseline() == null) finish();
            }
        }
    }

    /** The purse (or Piggy, in the Rift it's motes) from the scoreboard, or -1. */
    private static double purse() {
        String value = SkyBallsLocation.scoreboardValue("Purse:");
        if (value == null) value = SkyBallsLocation.scoreboardValue("Piggy:");
        if (value == null) return -1;
        Matcher m = NUMBER.matcher(value);
        if (!m.find()) return -1;
        try {
            return Double.parseDouble(m.group().replace(",", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Hypixel's price for a quest when the purse didn't show it (Aatrox's discount isn't known then). */
    private static long standardCost(String boss) {
        String tier = boss.substring(boss.lastIndexOf(' ') + 1);
        boolean blaze = boss.startsWith("Inferno Demonlord");
        if (boss.startsWith("Atoned Horror")) tier = "V";
        return switch (tier) {
            case "I" -> blaze ? 10_000 : 2_000;
            case "II" -> blaze ? 25_000 : 7_500;
            case "III" -> blaze ? 60_000 : 20_000;
            case "IV" -> blaze ? 150_000 : 50_000;
            case "V" -> 100_000;
            default -> 0;
        };
    }

    // ---------------------------------------------------------------- the fight

    /** Your boss appeared ({@link SlayerTimes}): remember the inventory, so the drops are what's new after. */
    static void onSpawn() {
        if (enabled()) baseline = inventory();
    }

    /** Hypixel confirmed the kill ({@link SlayerTimes}). */
    static void onKill(String boss) {
        if (!enabled() || baseline == null) return;
        if (kill != null) finish();
        long cost = questCost >= 0 ? questCost : standardCost(boss);
        kill = new Kill(boss, System.currentTimeMillis(), baseline, new LinkedHashMap<>(), cost);
        sacksIn = false;
        baseline = null;
    }

    private static void tick() {
        double purse = purse();
        if (purse >= 0) {
            // Two seconds after the quest started, the purse has gone down by its cost.
            if (questStartedAt > 0 && System.currentTimeMillis() - questStartedAt > 2_000L) {
                double paid = purseBeforeQuest - purse;
                // Unknown (no purse before, or it went up from coins picked up): use Hypixel's price instead.
                questCost = purseBeforeQuest >= 0 && paid > 0 && paid <= 1_000_000 ? Math.round(paid) : -1;
                questStartedAt = 0;
            }
            lastPurse = purse;
        }
        if (kill != null && System.currentTimeMillis() - kill.at() > SACKS_WINDOW_MS) finish();
    }

    private static Map<String, Integer> inventory() {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Integer> counts = new HashMap<>();
        if (mc.player == null) return counts;
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            String id = SkyBallsPriceTooltip.marketId(stack);
            if (!id.isEmpty()) counts.merge(id, stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static int inventoryTicks;

    /** The inventory part is read once, 5 seconds after the kill (drops on the ground take a moment to pick up). */
    private static void checkInventory() {
        if (kill == null || kill.baseline() == null) return;
        if (System.currentTimeMillis() - kill.at() < INVENTORY_WINDOW_MS) return;
        addInventoryGains(kill.baseline());
        kill = new Kill(kill.boss(), kill.at(), null, kill.sacks(), kill.cost());
        if (sacksIn) finish();
    }

    private static void addInventoryGains(Map<String, Integer> before) {
        for (Map.Entry<String, Integer> e : inventory().entrySet()) {
            int gained = e.getValue() - before.getOrDefault(e.getKey(), 0);
            if (gained > 0) GAINED.merge(e.getKey(), (long) gained, Long::sum);
        }
    }

    /** Inventory gains of the kill being totted up (id -> amount), filled 5 seconds after it. */
    private static final Map<String, Long> GAINED = new LinkedHashMap<>();

    // ---------------------------------------------------------------- the message

    private record Line(String name, long amount, double value) {}

    private static void finish() {
        Kill done = kill;
        kill = null;
        if (done == null) return;
        // Finished before the 5 second inventory check (the next boss died first): check it now.
        if (done.baseline() != null) addInventoryGains(done.baseline());
        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, Long> e : GAINED.entrySet()) {
            String id = e.getKey();
            String name = RepoItems.displayName(id);
            String plain = name == null ? null : ChatFormatting.stripFormatting(name);
            lines.add(new Line(plain == null ? id : plain, e.getValue(), SkyBallsPriceTooltip.unitPrice(id) * e.getValue()));
        }
        GAINED.clear();
        for (Map.Entry<String, Long> e : done.sacks().entrySet()) {
            String id = RepoItems.idByName(e.getKey());
            double each = id == null ? 0 : SkyBallsPriceTooltip.unitPrice(id);
            lines.add(new Line(e.getKey(), e.getValue(), each * e.getValue()));
        }
        lines.sort((a, b) -> Double.compare(b.value(), a.value()));
        double drops = lines.stream().mapToDouble(Line::value).sum();
        double total = drops - done.cost();

        MutableComponent hover = Component.literal(done.boss() + " drops").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (lines.isEmpty()) hover.append(Component.literal("\nNo drops seen").withStyle(ChatFormatting.GRAY));
        for (Line line : lines) {
            hover.append(Component.literal("\n" + line.amount() + "x " + line.name() + "  ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(line.value() > 0 ? CombatFeatures.formatCoins(line.value()) : "no price").withStyle(ChatFormatting.GOLD));
        }
        hover.append(Component.literal("\n\nDrops: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(CombatFeatures.formatCoins(drops)).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("\nSlayer cost: ").withStyle(ChatFormatting.GRAY)).append(Component.literal("-" + CombatFeatures.formatCoins(done.cost())).withStyle(ChatFormatting.RED))
            .append(Component.literal("\nTotal: ").withStyle(ChatFormatting.GRAY)).append(coins(total));
        SkyBallsAlerts.chat(Component.literal("Profit: ").withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                .withHoverEvent(new HoverEvent.ShowText(hover)))
            .append(coins(total).copy().withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover))))
            .append(Component.literal(" (hover for the drops)").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)
                .withHoverEvent(new HoverEvent.ShowText(hover)))));
    }

    private static MutableComponent coins(double value) {
        String text = (value < 0 ? "-" : "+") + CombatFeatures.formatCoins(Math.abs(value));
        return Component.literal(text).withStyle(value < 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    /** What a "[Sacks]" message added, per item name (each hover once; Hypixel repeats it on several parts). */
    private static Map<String, Long> sackGains(Component component) {
        Map<String, Long> net = new LinkedHashMap<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<Component> parts = new ArrayList<>();
        collect(component, parts);
        for (Component part : parts) {
            if (!(part.getStyle().getHoverEvent() instanceof HoverEvent.ShowText(Component hover))) continue;
            String text = ChatFormatting.stripFormatting(hover.getString());
            if (text == null || !seen.add(text)) continue;
            for (String line : text.split("\n")) {
                Matcher m = SACK_CHANGE.matcher(line);
                if (!m.matches()) continue;
                long amount = Long.parseLong(m.group(2).replace(",", ""));
                net.merge(m.group(3).trim(), m.group(1).equals("-") ? -amount : amount, Long::sum);
            }
        }
        return net;
    }

    private static void collect(Component component, List<Component> out) {
        out.add(component);
        for (Component sibling : component.getSiblings()) collect(sibling, out);
    }
}
