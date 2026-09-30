package com.epic60869.skyballs.features.garden;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsCraftHelper;
import com.epic60869.skyballs.SkyBallsPriceTooltip;
import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.combat.CombatFeatures;
import com.epic60869.skyballs.features.core.SkyBallsAlerts;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.epic60869.skyballs.features.slayer.SlayerBossProfit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Farming profit tracker, ported from Skysoft's Profit Tracker with its Farming preset
 * (https://github.com/Akinsoft/Skysoft, features/profit/ and data/profit_tracker_presets.json, LGPL-3.0): in the
 * Garden, what each crop and Garden drop you got is worth (bazaar insta-sell), the coins Bountiful gave you, Kernels
 * from the Feast Chef, pests vacuumed, the total profit, profit per hour and your farming time (it pauses when you
 * stop farming), for this session, today or in total. Items count when they come into your inventory (not from a
 * chest or menu) or your sacks; seeds and crops Replenish takes back, and crops your compactor turns into enchanted
 * ones, are taken off again so nothing counts twice.
 */
public final class FarmingProfitTracker {
    /** Skysoft's Farming preset items (Hypixel ids, so NEU's INK_SACK-3 is INK_SACK:3). */
    private static final List<String> ITEMS = List.of("WHEAT", "SEEDS", "CARROT_ITEM", "POTATO_ITEM", "NETHER_STALK",
        "PUMPKIN", "MELON", "INK_SACK:3", "SUGAR_CANE", "CACTUS", "RED_MUSHROOM", "BROWN_MUSHROOM", "HUGE_MUSHROOM_1",
        "HUGE_MUSHROOM_2", "DOUBLE_PLANT", "MOONFLOWER", "WILD_ROSE", "ENCHANTED_WHEAT", "ENCHANTED_HAY_BALE",
        "ENCHANTED_GOLDEN_CARROT", "ENCHANTED_BAKED_POTATO", "MUTANT_NETHER_STALK", "POLISHED_PUMPKIN",
        "ENCHANTED_MELON_BLOCK", "ENCHANTED_COOKIE", "ENCHANTED_SUGAR_CANE", "ENCHANTED_CACTUS",
        "ENCHANTED_HUGE_MUSHROOM_1", "ENCHANTED_HUGE_MUSHROOM_2", "COMPACTED_SUNFLOWER", "COMPACTED_MOONFLOWER",
        "COMPACTED_WILD_ROSE", "CROPIE", "SQUASH", "FERMENTO", "HELIANTHUS", "CORNUCOPIA", "CARROT_ZEST", "DEEPFRIES",
        "AGGOURDIAN", "CANE_KNOT", "MELON_JUICE", "CACTUS_FLOWER", "DESIGNER_COFFEE_BEANS", "FEASTFUNGUS", "BOTROOT",
        "SALTED_SUNFLOWER_SEEDS", "CRYSTALIZED_MOONLIGHT", "FLORAL_GELATIN", "RAREFINDER_GARDEN_CHIP",
        "BURROWING_SPORES", "WARTY", "DYE_WILD_STRAWBERRY", "DYE_DUNG", "DYE_COPPER", "TOOL_EXP_CAPSULE",
        "SQUEAKY_TOY", "SQUEAKY_MOUSEMAT", "BEADY_EYES", "CHIRPING_STEREO", "BOOKWORM_BOOK", "LOCUST_LARVA",
        "ATMOSPHERIC_FILTER", "CLIPPED_WINGS", "WRIGGLING_LARVA", "OVERCLOCKER_3000", "MANTID_CLAW",
        "FIRE_IN_A_BOTTLE", "VERMIN_VAPORIZER_GARDEN_CHIP", "DUNG", "COMPOST", "HONEY_JAR", "PLANT_MATTER",
        "CHEESE_FUEL", "JELLY", "VINYL_BEETLE", "VINYL_CRICKET_CHOIR", "VINYL_EARTHWORM_ENSEMBLE", "VINYL_PRETTY_FLY",
        "VINYL_CICADA_SYMPHONY", "VINYL_DYNAMITES", "VINYL_BUZZIN_BEATS", "VINYL_WINGS_OF_HARMONY",
        "VINYL_RODENT_REVOLUTION", "VINYL_SLOW_AND_GROOVY", "VINYL_PRAY_FOR_ME", "VINYL_FIREFLY",
        "VINYL_IMAGINE_DRAGONFLIES", "ATTRIBUTE_SHARD_PEST_LUCK", "ENCHANTMENT_PESTERMINATOR_1",
        "ENCHANTMENT_ULTIMATE_SUNSET_1");
    private static final Set<String> TRACKED = new HashSet<>(ITEMS);

    private static final String KERNEL_DONATION = "[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.";
    private static final Pattern PEST_KILL = Pattern.compile("^You received (\\d+)x (.+) for killing an? (.+)!$");
    private static final Pattern NUMBER = Pattern.compile("[\\d,]+(?:\\.\\d+)?");
    /** Kernels are valued as Feast I books, 25 Kernels each (Skysoft's default). */
    private static final String KERNEL_ITEM = "ENCHANTMENT_FEAST_1";
    private static final int KERNELS_PER_ITEM = 25;
    /** Coins count as Bountiful when you broke a crop this recently with a Bountiful tool. */
    private static final long BOUNTIFUL_WINDOW_MS = 2_000L;
    /** A compactor's input taken from your inventory this recently is matched against its output. */
    private static final long COMPACTION_WINDOW_MS = 60_000L;
    /** Sack drops still count this long after you leave the Garden (they arrive every so often). */
    private static final long SACK_WINDOW_MS = 60_000L;

    public enum Period {
        SESSION("Session"), TODAY("Today"), TOTAL("Total");

        private final String label;

        Period(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    static final class Data {
        @Expose Map<String, Long> items = new LinkedHashMap<>();
        @Expose double coins;
        @Expose long kernels;
        @Expose long pests;
        @Expose Map<String, Long> pestKills = new LinkedHashMap<>();
        @Expose long activeMs;
    }

    static final class Saved {
        @Expose Data total = new Data();
        @Expose Data today = new Data();
        /** The day {@link #today} is for (epoch day), so it starts again at midnight. */
        @Expose long day;
    }

    /** Crafting an item from one kind of item, e.g. 160 Wheat -> 1 Enchanted Wheat. */
    private record Conversion(String input, int inputCount, int outputCount) {}

    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    private static Path file;
    private static Saved saved = new Saved();
    private static Data session = new Data();
    private static Map<String, Integer> lastInventory;
    private static long lastActivity;
    private static long lastFarmed;
    private static long lastInGarden;
    private static long lastTick;
    private static double lastPurse = -1;
    private static boolean dirty;
    private static int ticks;
    /** Replenish replants from your drops: crop item -> how many to take back when that crop comes in. */
    private static final Map<String, Integer> replenishCosts = new HashMap<>();
    /** Tracked items that left your inventory recently (a compactor's input): id -> {amount, until}. */
    private static final Map<String, long[]> removals = new HashMap<>();
    private static final Map<String, Conversion> conversions = new ConcurrentHashMap<>();
    private static boolean conversionsRequested;

    private FarmingProfitTracker() {}

    private static FeatureConfigs.FarmingProfitTracker config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.farming.profitTracker;
    }

    private static boolean enabled() {
        FeatureConfigs.FarmingProfitTracker c = config();
        return c != null && c.enabled;
    }

    private static boolean inGarden() {
        return SkyBallsLocation.onSkyblock() && SkyBallsLocation.inGarden();
    }

    public static void init(Path configDir) {
        file = configDir.resolve("skyballs").resolve("farming-profit.json");
        load();
        ClientTickEvents.END_CLIENT_TICK.register(FarmingProfitTracker::tick);
        SkyBallsChat.onChat(message -> onChat(message.component(), message.text()));
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> onBreak(state.getBlock(), level.getGameTime()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("farmingtracker")
                    .then(ClientCommands.argument("what", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("session", "today", "total", "reset"), b))
                        .executes(c -> command(StringArgumentType.getString(c, "what"))))));
            }
        });
        SkyBallsHuds.register("farming_profit", "Farming Profit Tracker",
            () -> enabled() && inGarden(),
            FarmingProfitTracker::lines,
            List.of(Component.literal("Farming Profit (Session)").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                Component.literal("Ench Wheat ").withStyle(ChatFormatting.WHITE).append(Component.literal("x42 ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("98.3k").withStyle(ChatFormatting.GOLD)),
                Component.literal("Total Profit: ").withStyle(ChatFormatting.GRAY).append(Component.literal("+1.24M").withStyle(ChatFormatting.GREEN)),
                Component.literal("Profit/h: ").withStyle(ChatFormatting.GRAY).append(Component.literal("+8.10M").withStyle(ChatFormatting.GREEN)),
                Component.literal("Uptime: ").withStyle(ChatFormatting.GRAY).append(Component.literal("9m 12s").withStyle(ChatFormatting.AQUA))),
            8, 150);
    }

    private static int command(String what) {
        Minecraft.getInstance().execute(() -> {
            FeatureConfigs.FarmingProfitTracker c = config();
            if (c == null) return;
            switch (what.toLowerCase(Locale.ROOT)) {
                case "session" -> c.period = Period.SESSION;
                case "today" -> c.period = Period.TODAY;
                case "total", "alltime" -> c.period = Period.TOTAL;
                case "reset" -> {
                    Period period = period();
                    switch (period) {
                        case SESSION -> session = new Data();
                        case TODAY -> saved.today = new Data();
                        case TOTAL -> saved.total = new Data();
                    }
                    dirty = true;
                    SkyBallsAlerts.chat(Component.literal("Farming profit tracker (" + period + ") reset.").withStyle(ChatFormatting.YELLOW));
                    return;
                }
                default -> {
                    SkyBallsAlerts.chat(Component.literal("Use /sb farmingtracker session, today, total or reset.").withStyle(ChatFormatting.RED));
                    return;
                }
            }
            SkyBallsAlerts.chat(Component.literal("Farming profit tracker shows: " + c.period).withStyle(ChatFormatting.YELLOW));
        });
        return 1;
    }

    private static Period period() {
        FeatureConfigs.FarmingProfitTracker c = config();
        return c == null || c.period == null ? Period.SESSION : c.period;
    }

    // ------------------------------------------------------------------------------------------------ counting

    private static List<Data> targets() {
        long day = LocalDate.now().toEpochDay();
        if (saved.day != day) {
            saved.day = day;
            saved.today = new Data();
        }
        return List.of(session, saved.today, saved.total);
    }

    private static void add(String id, long amount) {
        if (amount == 0) return;
        for (Data d : targets()) {
            long updated = Math.max(0, d.items.getOrDefault(id, 0L) + amount);
            if (updated == 0) d.items.remove(id);
            else d.items.put(id, updated);
        }
        lastActivity = System.currentTimeMillis();
        dirty = true;
    }

    private static String normalise(String id) {
        return id == null ? null : id.replace('-', ':');
    }

    private static boolean tracked(String id) {
        return id != null && TRACKED.contains(id);
    }

    private static void onBreak(Block block, long dayTime) {
        if (!enabled() || !inGarden() || !isCrop(block)) return;
        long now = System.currentTimeMillis();
        lastActivity = now;
        lastFarmed = now;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        CompoundTag data = Compat.getCustomData(mc.player.getMainHandItem());
        if (!data.getCompoundOrEmpty("enchantments").contains("replenish")) return;
        String crop = replenishCrop(block, dayTime);
        if (crop != null) replenishCosts.merge(crop, 1, Integer::sum);
    }

    private static void onChat(Component component, String text) {
        if (!enabled()) return;
        String t = text.trim();
        if (inGarden() && t.equals(KERNEL_DONATION)) {
            for (Data d : targets()) d.kernels++;
            lastActivity = System.currentTimeMillis();
            dirty = true;
            return;
        }
        Matcher pest = PEST_KILL.matcher(t);
        if (inGarden() && pest.matches()) {
            String item = pest.group(2), name = pest.group(3);
            // One message per drop: count the kill once (Skysoft's ParsedGardenPestKill).
            boolean kill = switch (name) {
                case "Field Mouse" -> item.equals("Dung");
                case "Lunar Moth" -> item.equals("Enchanted Sunflower");
                default -> !item.equals("Overclocker 3000");
            };
            if (kill) {
                for (Data d : targets()) {
                    d.pests++;
                    d.pestKills.merge(name, 1L, Long::sum);
                }
                lastActivity = System.currentTimeMillis();
                dirty = true;
            }
            return;
        }
        if (t.startsWith("[Sacks]") && System.currentTimeMillis() - lastInGarden < SACK_WINDOW_MS) {
            for (Map.Entry<String, Long> e : SlayerBossProfit.sackGains(component).entrySet()) {
                if (e.getValue() <= 0) continue;
                String id = normalise(RepoItems.idByName(e.getKey()));
                if (tracked(id)) add(id, e.getValue());
            }
        }
    }

    private static void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        long delta = lastTick == 0 ? 0 : Math.min(1_000L, now - lastTick);
        lastTick = now;
        if (!enabled() || mc.player == null) {
            lastInventory = null;
            lastPurse = -1;
            return;
        }
        if (!conversionsRequested && RepoItems.itemsLoaded()) {
            conversionsRequested = true;
            RepoItems.runAsync(FarmingProfitTracker::loadConversions);
        }
        boolean garden = inGarden();
        if (garden) lastInGarden = now;
        // Farming time, paused after a while without farming (Skysoft's Pause After).
        FeatureConfigs.FarmingProfitTracker c = config();
        if (garden && mc.isWindowActive() && !paused(c, now)) {
            for (Data d : targets()) d.activeMs += delta;
            if (++ticks % 200 == 0) dirty = true;
        }
        removals.values().removeIf(r -> r[1] < now);
        trackPurse(mc, garden, now);
        // Items that come into your inventory while no menu is open (moving them from chests doesn't count).
        if (mc.gui.screen() != null || !garden) {
            lastInventory = null;
        } else {
            Map<String, Integer> current = inventory(mc);
            if (lastInventory != null) countInventory(current, now);
            lastInventory = current;
        }
        if (dirty && ticks % 100 == 0) save();
    }

    private static boolean paused(FeatureConfigs.FarmingProfitTracker c, long now) {
        return c.pauseAfter && now - lastActivity > Math.max(15, c.pauseAfterSeconds) * 1000L;
    }

    private static void countInventory(Map<String, Integer> current, long now) {
        Map<String, Long> changes = new HashMap<>();
        Set<String> ids = new HashSet<>(current.keySet());
        ids.addAll(lastInventory.keySet());
        for (String id : ids) {
            if (!tracked(id)) continue;
            long change = current.getOrDefault(id, 0) - lastInventory.getOrDefault(id, 0);
            if (change > 0) changes.put(id, change);
            else if (change < 0) {
                // Maybe a compactor's input: remember it for a minute (Skysoft's ProfitCraftingReconciliation).
                long[] r = removals.computeIfAbsent(id, k -> new long[2]);
                r[0] += -change;
                r[1] = now + COMPACTION_WINDOW_MS;
            }
        }
        if (changes.isEmpty()) return;
        Map<String, Long> counted = new HashMap<>(changes);
        for (Map.Entry<String, Long> e : changes.entrySet()) {
            Conversion conversion = conversions.get(e.getKey());
            if (conversion == null || e.getValue() % conversion.outputCount() != 0) continue;
            long[] r = removals.get(conversion.input());
            if (r == null) continue;
            long required = (long) conversion.inputCount() * e.getValue() / conversion.outputCount();
            long consumed = Math.min(r[0], required);
            r[0] -= consumed;
            if (r[0] <= 0) removals.remove(conversion.input());
            counted.merge(conversion.input(), -consumed, Long::sum);
        }
        // Replenish took some of those crops back to replant (Skysoft's ProfitReplenishCosts).
        for (Map.Entry<String, Integer> cost : new ArrayList<>(replenishCosts.entrySet())) {
            String harvest = cost.getKey().equals("SEEDS") ? "WHEAT" : cost.getKey();
            if (changes.getOrDefault(harvest, 0L) <= 0) continue;
            counted.merge(cost.getKey(), (long) -cost.getValue(), Long::sum);
            replenishCosts.remove(cost.getKey());
        }
        for (Map.Entry<String, Long> e : counted.entrySet()) add(e.getKey(), e.getValue());
    }

    /** Coins while farming with a Bountiful tool (Skysoft's Bountiful Coins), from the scoreboard's purse. */
    private static void trackPurse(Minecraft mc, boolean garden, long now) {
        double purse = purse();
        double previous = lastPurse;
        lastPurse = purse;
        if (!garden || purse < 0 || previous < 0 || mc.gui.screen() != null) return;
        double gained = purse - previous;
        if (gained <= 1 || gained >= 100_000 || now - lastFarmed > BOUNTIFUL_WINDOW_MS) return;
        String modifier = Compat.getCustomData(mc.player.getMainHandItem()).getStringOr("modifier", "");
        if (!modifier.equals("bountiful")) return;
        for (Data d : targets()) d.coins += gained;
        dirty = true;
    }

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

    private static Map<String, Integer> inventory(Minecraft mc) {
        Map<String, Integer> counts = new HashMap<>();
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            String id = SkyBallsPriceTooltip.marketId(stack);
            if (!id.isEmpty()) counts.merge(id, stack.getCount(), Integer::sum);
        }
        return counts;
    }

    /** Crafting recipes with one kind of input between tracked items, from the NEU repo. Blocking. */
    private static void loadConversions() {
        for (String output : ITEMS) {
            try {
                SkyBallsCraftHelper.Recipe recipe = SkyBallsCraftHelper.recipeOf(output);
                if (recipe == null || !recipe.type().equals("crafting") || recipe.inputs().size() != 1) continue;
                SkyBallsCraftHelper.Input input = recipe.inputs().getFirst();
                String inputId = normalise(input.id());
                if (tracked(inputId)) conversions.put(output, new Conversion(inputId, input.amount(), recipe.outputCount()));
            } catch (Exception ignored) {}
        }
    }

    private static boolean isCrop(Block block) {
        return block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.NETHER_WART
            || block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN || block == Blocks.MELON || block == Blocks.COCOA
            || block == Blocks.SUGAR_CANE || block == Blocks.CACTUS || block == Blocks.RED_MUSHROOM
            || block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.BROWN_MUSHROOM_BLOCK
            || block == Blocks.SUNFLOWER || block == Blocks.ROSE_BUSH;
    }

    /** What Replenish takes back to replant a crop, or null if it doesn't. */
    private static String replenishCrop(Block block, long dayTime) {
        if (block == Blocks.WHEAT) return "SEEDS";
        if (block == Blocks.CARROTS) return "CARROT_ITEM";
        if (block == Blocks.POTATOES) return "POTATO_ITEM";
        if (block == Blocks.NETHER_WART) return "NETHER_STALK";
        if (block == Blocks.COCOA) return "INK_SACK:3";
        if (block == Blocks.ROSE_BUSH) return "WILD_ROSE";
        if (block == Blocks.SUNFLOWER) return dayTime % 24_000L >= 12_000L ? "MOONFLOWER" : "DOUBLE_PLANT";
        return null;
    }

    // ------------------------------------------------------------------------------------------------ HUD

    private static List<Component> lines() {
        FeatureConfigs.FarmingProfitTracker c = config();
        Period period = period();
        Data data = switch (period) {
            case SESSION -> session;
            case TODAY -> targets().get(1);
            case TOTAL -> saved.total;
        };
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Farming Profit (" + period + ")").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        List<Map.Entry<String, Long>> items = new ArrayList<>(data.items.entrySet());
        items.sort((a, b) -> Double.compare(value(b.getKey(), b.getValue()), value(a.getKey(), a.getValue())));
        double total = data.coins;
        for (Map.Entry<String, Long> e : items) total += value(e.getKey(), e.getValue());
        int max = c == null ? 8 : Math.max(1, Math.min(15, c.maximumItems));
        if (items.isEmpty()) lines.add(Component.literal("No tracked drops yet.").withStyle(ChatFormatting.GRAY));
        for (Map.Entry<String, Long> e : items.subList(0, Math.min(max, items.size()))) {
            double value = value(e.getKey(), e.getValue());
            lines.add(Component.literal(name(e.getKey()) + " ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("x" + String.format(Locale.US, "%,d", e.getValue()) + " ").withStyle(ChatFormatting.GRAY))
                .append(value > 0 ? Component.literal(CombatFeatures.formatCoins(value)).withStyle(ChatFormatting.GOLD)
                    : Component.literal("Unknown").withStyle(ChatFormatting.DARK_GRAY)));
        }
        if (items.size() > max) lines.add(Component.literal("... " + (items.size() - max) + " more").withStyle(ChatFormatting.DARK_GRAY));
        if (data.coins > 0) lines.add(row("Bountiful Coins", Component.literal(CombatFeatures.formatCoins(data.coins)).withStyle(ChatFormatting.GOLD)));
        if (data.kernels > 0) {
            double kernel = SkyBallsPriceTooltip.unitPrice(KERNEL_ITEM) * data.kernels / KERNELS_PER_ITEM;
            total += kernel;
            lines.add(row("Kernel Profit", kernel > 0 ? Component.literal(CombatFeatures.formatCoins(kernel)).withStyle(ChatFormatting.GOLD)
                : Component.literal("Unknown").withStyle(ChatFormatting.DARK_GRAY)));
        }
        lines.add(row("Total Profit", signed(total)));
        double hours = data.activeMs / 3_600_000d;
        lines.add(row("Profit/h", signed(hours > 0 ? total / hours : 0)));
        lines.add(row("Pests Vacuumed", Component.literal(String.format(Locale.US, "%,d", data.pests)).withStyle(ChatFormatting.YELLOW)));
        MutableComponent uptime = Component.literal(duration(data.activeMs)).withStyle(ChatFormatting.AQUA);
        if (c != null && paused(c, System.currentTimeMillis())) uptime.append(Component.literal(" (paused)").withStyle(ChatFormatting.RED));
        lines.add(row("Uptime", uptime));
        return lines;
    }

    private static double value(String id, long amount) {
        return SkyBallsPriceTooltip.unitPrice(id) * amount;
    }

    private static String name(String id) {
        String name = RepoItems.displayName(id);
        return (name == null ? id : ChatFormatting.stripFormatting(name)).replace("Enchanted ", "Ench ");
    }

    private static Component row(String label, Component value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY).append(value);
    }

    private static Component signed(double value) {
        String text = (value < 0 ? "-" : "+") + CombatFeatures.formatCoins(Math.abs(value));
        return Component.literal(text).withStyle(value < 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    private static String duration(long ms) {
        long seconds = ms / 1000;
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m " + seconds % 60 + "s";
    }

    // ------------------------------------------------------------------------------------------------ saving

    private static void load() {
        try {
            if (Files.exists(file)) {
                Saved loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Saved.class);
                if (loaded != null) {
                    if (loaded.total == null) loaded.total = new Data();
                    if (loaded.today == null) loaded.today = new Data();
                    saved = loaded;
                }
            }
        } catch (Exception e) {
            System.err.println("[SkyBalls] Couldn't read the farming profit tracker: " + e.getMessage());
        }
    }

    private static void save() {
        dirty = false;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(saved), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Couldn't save the farming profit tracker: " + e.getMessage());
        }
    }
}
