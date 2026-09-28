package com.epic60869.skyballs.features.slayer;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsPriceTooltip;
import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.features.combat.CombatFeatures;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
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
 * Profit per slayer boss: "Profit: -10k" in chat after each kill, with a hover listing each drop and its value, the
 * drops' total, the quest's cost and what's left, and the same in the Boss Profit HUD. Drops are what came into your
 * inventory since your boss spawned, plus what went straight into your sacks (Hypixel's "[Sacks]" messages). The
 * chat line goes out as soon as the "[Sacks]" message after the kill comes, or 2 seconds after the kill without one;
 * the HUD shows from the moment the boss dies and fills in as later drops arrive, for up to 30 seconds. The cost is
 * what your purse went down by when the quest started, or Hypixel's price for that boss and tier.
 */
public final class SlayerBossProfit {
    /** Without a "[Sacks]" message, the chat line waits this long for drops to reach your inventory. */
    private static final long CHAT_WAIT_MS = 2_000L;
    /** How long after the kill late drops (sacks, pickups) still count. */
    private static final long TRACK_MS = 30_000L;
    /** "+64 Revenant Flesh (Combat Sack)" in a "[Sacks]" message's hover. */
    private static final Pattern SACK_CHANGE = Pattern.compile("^ *([+-])([0-9,]+) (.+?) [(].*[)] *$");
    private static final Pattern NUMBER = Pattern.compile("[\\d,.]+");

    /** Inventory when your boss spawned: item price id -> count. */
    private static Map<String, Integer> baseline;
    /** The kill still being totted up (drops can still arrive), or null. */
    private static Kill kill;
    /** The latest result, for the HUD, and when its boss died. */
    private static Result last;

    /** What the purse went down by around the last quest start, or -1 (checked against the price at the kill). */
    private static long questPaid = -1;
    private static long questStartedAt;
    /** The purse over the last 15 seconds: {time, coins}. Hypixel takes the fee before "SLAYER QUEST STARTED!". */
    private static final java.util.ArrayDeque<double[]> PURSE = new java.util.ArrayDeque<>();
    private static int ticks;

    private static final class Kill {
        final String boss;
        final long at;
        final Map<String, Integer> baseline;
        final Map<String, Long> sacks = new LinkedHashMap<>();
        final long cost;
        boolean posted;

        Kill(String boss, long at, Map<String, Integer> baseline, long cost) {
            this.boss = boss;
            this.at = at;
            this.baseline = baseline;
            this.cost = cost;
        }
    }

    private record Line(String name, long amount, double value) {}

    /** One boss's drops (most valuable first), their total, the quest's cost and the profit. */
    private record Result(String boss, long at, List<Line> lines, double drops, long cost, boolean counting) {
        double total() {
            return drops - cost;
        }
    }

    private SlayerBossProfit() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++ticks % 5 == 0) tick();
        });
        SkyBallsChat.onGameMessage((component, overlay) -> {
            if (!overlay) onMessage(component);
        });
        SkyBallsHuds.register("slayer_boss_profit", "Slayer Boss Profit", SlayerBossProfit::hudVisible, SlayerBossProfit::hudLines,
            List.of(
                Component.literal("Revenant Horror V").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Profit: ").withStyle(ChatFormatting.YELLOW).append(Component.literal("+84.2k").withStyle(ChatFormatting.GREEN)),
                Component.literal("1x Scythe Blade ").withStyle(ChatFormatting.WHITE).append(Component.literal("120.0k").withStyle(ChatFormatting.GOLD)),
                Component.literal("64x Revenant Flesh ").withStyle(ChatFormatting.WHITE).append(Component.literal("4.2k").withStyle(ChatFormatting.GOLD)),
                Component.literal("Slayer cost: ").withStyle(ChatFormatting.GRAY).append(Component.literal("-50.0k").withStyle(ChatFormatting.RED))),
            8, 320);
    }

    private static SkyBallsConfig.BossProfit config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.slayers.bossProfit;
    }

    static boolean enabled() {
        SkyBallsConfig.BossProfit c = config();
        return c != null && (c.chat || c.hud);
    }

    // ---------------------------------------------------------------- the quest's cost

    private static void onMessage(Component component) {
        String text = SkyBallsLocation.strip(component.getString()).trim();
        if (text.equals("SLAYER QUEST STARTED!")) {
            // The fee is taken when you click Maddox's menu, just before this message; the scoreboard can lag behind.
            questStartedAt = System.currentTimeMillis();
            questPaid = -1;
        } else if (text.startsWith("[Sacks]") && kill != null) {
            for (Map.Entry<String, Long> e : sackGains(component).entrySet()) {
                if (e.getValue() > 0) kill.sacks.merge(e.getKey(), e.getValue(), Long::sum);
            }
            // The boss's sack drops are in: the chat line can go.
            refresh();
            postChat();
        }
    }

    /** The purse (or Piggy) from the scoreboard, or -1. */
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

    /**
     * The quest's cost for {@code boss}: what the purse went down by if that's Hypixel's price or half of it
     * (Aatrox's Slashed Pricing), else Hypixel's price. Other coin changes around the quest start can't be mistaken
     * for the fee that way.
     */
    private static long cost(String boss) {
        long standard = standardCost(boss);
        if (questPaid > 0 && standard > 0) {
            for (long price : new long[]{standard, standard / 2}) {
                if (Math.abs(questPaid - price) <= price * 0.05) return price;
            }
        }
        return standard;
    }

    /** Hypixel's price for a quest (without Aatrox's discount). */
    private static long standardCost(String boss) {
        // Rift quests cost motes, not coins.
        if (boss.startsWith("Riftstalker Bloodfiend")) return 0;
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

    /** Hypixel confirmed the kill ({@link SlayerTimes}): the HUD shows it straight away. */
    static void onKill(String boss) {
        if (!enabled() || baseline == null) return;
        if (kill != null) close();
        long cost = cost(boss);
        kill = new Kill(boss, System.currentTimeMillis(), baseline, cost);
        baseline = null;
        refresh();
    }

    private static void tick() {
        long now = System.currentTimeMillis();
        double purse = purse();
        if (purse >= 0) {
            PURSE.addLast(new double[]{now, purse});
            while (!PURSE.isEmpty() && now - PURSE.peekFirst()[0] > 15_000L) PURSE.removeFirst();
            // Three seconds after the quest started the scoreboard shows the fee paid: the drop from the highest
            // purse in the 10 seconds before the message.
            if (questStartedAt > 0 && now - questStartedAt > 3_000L) {
                double highest = -1;
                for (double[] sample : PURSE) {
                    if (sample[0] >= questStartedAt - 10_000L && sample[0] <= questStartedAt) highest = Math.max(highest, sample[1]);
                }
                questPaid = highest > purse ? Math.round(highest - purse) : -1;
                questStartedAt = 0;
            }
        }
        if (kill == null) return;
        long since = System.currentTimeMillis() - kill.at;
        refresh();
        if (since >= CHAT_WAIT_MS) postChat();
        if (since >= TRACK_MS) close();
    }

    /** Stops counting drops for the kill; its last result stays on the HUD. */
    private static void close() {
        refresh();
        postChat();
        if (last != null) last = new Result(last.boss(), last.at(), last.lines(), last.drops(), last.cost(), false);
        kill = null;
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

    /** Works out the kill's result from the inventory now and the sack drops so far. */
    private static void refresh() {
        Kill k = kill;
        if (k == null) return;
        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : inventory().entrySet()) {
            int gained = e.getValue() - k.baseline.getOrDefault(e.getKey(), 0);
            if (gained <= 0) continue;
            String id = e.getKey();
            String name = RepoItems.displayName(id);
            String plain = name == null ? null : ChatFormatting.stripFormatting(name);
            lines.add(new Line(plain == null ? id : plain, gained, SkyBallsPriceTooltip.unitPrice(id) * gained));
        }
        for (Map.Entry<String, Long> e : k.sacks.entrySet()) {
            String id = RepoItems.idByName(e.getKey());
            double each = id == null ? 0 : SkyBallsPriceTooltip.unitPrice(id);
            lines.add(new Line(e.getKey(), e.getValue(), each * e.getValue()));
        }
        lines.sort((a, b) -> Double.compare(b.value(), a.value()));
        double drops = lines.stream().mapToDouble(Line::value).sum();
        last = new Result(k.boss, k.at, List.copyOf(lines), drops, k.cost, true);
    }

    // ---------------------------------------------------------------- chat

    private static void postChat() {
        Kill k = kill;
        SkyBallsConfig.BossProfit c = config();
        if (k == null || k.posted || last == null) return;
        k.posted = true;
        if (c == null || !c.chat) return;
        Result r = last;
        MutableComponent hover = Component.literal(r.boss() + " drops").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (r.lines().isEmpty()) hover.append(Component.literal("\nNo drops seen").withStyle(ChatFormatting.GRAY));
        for (Line line : r.lines()) {
            hover.append(Component.literal("\n" + line.amount() + "x " + line.name() + "  ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(line.value() > 0 ? CombatFeatures.formatCoins(line.value()) : "no price").withStyle(ChatFormatting.GOLD));
        }
        hover.append(Component.literal("\n\nDrops: ").withStyle(ChatFormatting.GRAY)).append(Component.literal(CombatFeatures.formatCoins(r.drops())).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("\nSlayer cost: ").withStyle(ChatFormatting.GRAY)).append(Component.literal("-" + CombatFeatures.formatCoins(r.cost())).withStyle(ChatFormatting.RED))
            .append(Component.literal("\nTotal: ").withStyle(ChatFormatting.GRAY)).append(coins(r.total()));
        SkyBallsAlerts.chat(Component.literal("Profit: ").withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                .withHoverEvent(new HoverEvent.ShowText(hover)))
            .append(coins(r.total()).copy().withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover))))
            .append(Component.literal(" (hover for the drops)").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)
                .withHoverEvent(new HoverEvent.ShowText(hover)))));
    }

    // ---------------------------------------------------------------- HUD

    private static boolean hudVisible() {
        SkyBallsConfig.BossProfit c = config();
        Minecraft mc = Minecraft.getInstance();
        if (c == null || !c.hud || last == null || mc.level == null || mc.gui.hud.isHidden()) return false;
        return last.counting() || System.currentTimeMillis() - last.at() < c.hudSeconds * 1000L;
    }

    private static List<Component> hudLines() {
        Result r = last;
        if (r == null) return List.of();
        List<Component> lines = new ArrayList<>();
        MutableComponent title = Component.literal(r.boss()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (r.counting()) title.append(Component.literal(" (counting)").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withBold(false)));
        lines.add(title);
        lines.add(Component.literal("Profit: ").withStyle(ChatFormatting.YELLOW).append(coins(r.total())));
        SkyBallsConfig.BossProfit c = config();
        int rows = c == null ? 5 : c.hudRows;
        for (int i = 0; i < Math.min(rows, r.lines().size()); i++) {
            Line line = r.lines().get(i);
            lines.add(Component.literal(line.amount() + "x " + line.name() + " ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(line.value() > 0 ? CombatFeatures.formatCoins(line.value()) : "no price").withStyle(ChatFormatting.GOLD)));
        }
        if (r.lines().size() > rows) lines.add(Component.literal("+" + (r.lines().size() - rows) + " more").withStyle(ChatFormatting.DARK_GRAY));
        if (r.lines().isEmpty()) lines.add(Component.literal("No drops yet").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Slayer cost: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("-" + CombatFeatures.formatCoins(r.cost())).withStyle(ChatFormatting.RED)));
        return lines;
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
