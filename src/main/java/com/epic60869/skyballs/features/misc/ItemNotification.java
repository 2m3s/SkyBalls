package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsPriceTooltip;
import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.epic60869.skyballs.features.core.SkyBallsHuds;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item Notification, SkyOcean's Sack Notification as a HUD: when an item on your list goes into your sacks ("[Sacks]"
 * messages) or your inventory, it shows up like the farming RNG HUD, "5x Enchanted Diamond   1.2m" (the price is for
 * all of them). Repeats of the same item add up while it is showing. List items by name in Misc > Item Notification
 * or with /sj itemnotify add|remove|list.
 */
public final class ItemNotification {
    private static final Pattern SACK_LINE = Pattern.compile("^\\s*\\+([\\d,]+) (.+?) \\(.+\\)$");
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 14;

    /** One item on the HUD: how many, its name (with colour codes), its id for the price, and when it goes away. */
    private static final class Shown {
        long amount;
        String name;
        String id;
        long until;
    }

    private static final Map<String, Shown> SHOWN = new LinkedHashMap<>();
    /** Everything in your inventory last tick, by item id; null when there's nothing to compare against. */
    private static Map<String, Integer> lastInventory;
    /**
     * The most of each item you had just before its count dropped, and when. Hypixel sometimes swaps a stack out and
     * back (lore or cooldown updates, moving it), which looked like picking it up again; a gain only counts past this.
     */
    private static final Map<String, Integer> recentMax = new HashMap<>();
    private static final Map<String, Long> recentMaxAt = new HashMap<>();
    private static final long RECENT_MS = 10_000L;
    /** Inventory gains in the last few seconds, so the same drop also reported by a [Sacks] message isn't shown twice. */
    private static final Map<String, Long> inventoryGainAt = new HashMap<>();
    /**
     * Ticks to wait after a menu closes (or you change area) before counting again: Hypixel resends your inventory a
     * moment later, which looked like new items when you just opened and closed your sacks.
     */
    private static int settleTicks;
    private static final int SETTLE_AFTER_MENU = 20;
    /** "Moved 64 Gold Ingot from your Sacks to your inventory." (/gfs): items taken out of your sacks aren't gains. */
    private static final Pattern FROM_SACKS = Pattern.compile("^Moved ([0-9,]+) (.+?) from your Sacks to your inventory\\.?$");
    /** Lower-case item name -> until when gains of it are ignored. */
    private static final Map<String, Long> fromSacksUntil = new HashMap<>();

    private static void onFromSacks(String text) {
        Matcher m = FROM_SACKS.matcher(ChatFormatting.stripFormatting(text).trim());
        if (m.matches()) fromSacksUntil.put(m.group(2).trim().toLowerCase(Locale.ROOT), System.currentTimeMillis() + 3_000L);
    }

    private static boolean takenFromSacks(String name) {
        Long until = fromSacksUntil.get(ChatFormatting.stripFormatting(name == null ? "" : name).trim().toLowerCase(Locale.ROOT));
        return until != null && System.currentTimeMillis() < until;
    }

    private static final Map<String, String> NAMES = new HashMap<>(); // item id -> name with colour codes

    private ItemNotification() {}

    private static FeatureConfigs.ItemNotification config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.misc.itemNotification;
    }

    private static boolean enabled() {
        FeatureConfigs.ItemNotification c = config();
        return c != null && c.enabled && Compat.isOnSkyblock();
    }

    public static void init() {
        // Shown in the RNG HUD (SkyBallsRngHud) together with the farming RNG drops, not a HUD of its own.
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
        SkyBallsChat.onGameMessage((component, overlay) -> {
            if (!overlay) {
                onSacksMessage(component);
                onFromSacks(component.getString());
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("itemnotify")
                    .executes(c -> {
                        com.epic60869.skyballs.custom.util.Compat.queueOpenScreen(new ItemNotificationScreen(null));
                        return 1;
                    })
                    .then(ClientCommands.literal("list").executes(c -> list()))
                    .then(ClientCommands.literal("add").then(ClientCommands.argument("item", StringArgumentType.greedyString())
                        .executes(c -> add(StringArgumentType.getString(c, "item")))))
                    .then(ClientCommands.literal("remove").then(ClientCommands.argument("item", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(items(), b))
                        .executes(c -> remove(StringArgumentType.getString(c, "item")))))));
            }
        });
    }

    // ---------------------------------------------------------------- the list

    private static List<String> items() {
        FeatureConfigs.ItemNotification c = config();
        return c == null ? new ArrayList<>() : split(c.items);
    }

    /** The saved list: one item per line (commas also work), blanks and duplicates dropped. */
    static List<String> split(String saved) {
        List<String> out = new ArrayList<>();
        if (saved == null) return out;
        for (String part : saved.split("[\n,]")) {
            String item = part.trim();
            if (!item.isEmpty() && out.stream().noneMatch(item::equalsIgnoreCase)) out.add(item);
        }
        return out;
    }

    public static void openEditor() {
        Minecraft mc = Minecraft.getInstance();
        com.epic60869.skyballs.custom.util.Compat.queueOpenScreen(new ItemNotificationScreen(mc.gui.screen()));
    }

    /** Whether an item (by its plain name or its id) is on the list. */
    private static boolean listed(String name, String id) {
        String plain = ChatFormatting.stripFormatting(name == null ? "" : name).trim();
        for (String item : items()) {
            if (item.equalsIgnoreCase(plain) || item.equalsIgnoreCase(id) || item.replace(' ', '_').equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- gains

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        FeatureConfigs.ItemNotification c = config();
        long now = System.currentTimeMillis();
        SHOWN.values().removeIf(s -> now > s.until);
        if (!enabled() || mc.player == null || !c.checkInventory || items().isEmpty()) {
            lastInventory = null;
            return;
        }
        // Only count pickups while no menu is open, so moving items out of chests doesn't count.
        if (mc.gui.screen() != null) {
            lastInventory = null;
            settleTicks = SETTLE_AFTER_MENU;
            return;
        }
        if (settleTicks > 0) {
            settleTicks--;
            lastInventory = null;
            return;
        }
        Map<String, Integer> nowCounts = new HashMap<>();
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            String id = Compat.neuName(stack);
            if (id.isEmpty()) continue;
            nowCounts.merge(id, stack.getCount(), Integer::sum);
            NAMES.putIfAbsent(id, legacyName(stack));
        }
        if (lastInventory != null) {
            for (Map.Entry<String, Integer> e : lastInventory.entrySet()) {
                int had = e.getValue();
                if (nowCounts.getOrDefault(e.getKey(), 0) >= had) continue;
                Long at = recentMaxAt.get(e.getKey());
                int max = at != null && now - at < RECENT_MS ? Math.max(had, recentMax.getOrDefault(e.getKey(), 0)) : had;
                recentMax.put(e.getKey(), max);
                recentMaxAt.put(e.getKey(), now);
            }
            for (Map.Entry<String, Integer> e : nowCounts.entrySet()) {
                int before = lastInventory.getOrDefault(e.getKey(), 0);
                Long at = recentMaxAt.get(e.getKey());
                if (at != null && now - at < RECENT_MS) before = Math.max(before, recentMax.getOrDefault(e.getKey(), 0));
                int gained = e.getValue() - before;
                String name = NAMES.getOrDefault(e.getKey(), e.getKey());
                if (gained > 0 && listed(name, e.getKey()) && !takenFromSacks(name)) {
                    show(e.getKey(), name, gained);
                    inventoryGainAt.put(e.getKey().toLowerCase(Locale.ROOT), now);
                }
            }
        }
        lastInventory = nowCounts;
    }

    /** "[Sacks] +1,234 items." — the hover lists each item that went into your sacks. */
    private static void onSacksMessage(Component component) {
        FeatureConfigs.ItemNotification c = config();
        if (!enabled() || !c.checkSacks || !component.getString().contains("[Sacks]")) return;
        for (Component part : flatten(component)) {
            if (!(part.getStyle().getHoverEvent() instanceof HoverEvent.ShowText(Component hover))) continue;
            for (String line : ChatFormatting.stripFormatting(hover.getString()).split("\n")) {
                Matcher m = SACK_LINE.matcher(line);
                if (!m.matches()) continue;
                String name = m.group(2).trim();
                String id = RepoItems.idByName(name);
                if (!listed(name, id == null ? "" : id)) continue;
                String shownName = id != null && RepoItems.displayName(id) != null ? RepoItems.displayName(id) : name;
                // Already shown from your inventory a moment ago (the same drop, then put into your sacks).
                Long gainedAt = inventoryGainAt.get((id == null ? name : id).toLowerCase(Locale.ROOT));
                if (gainedAt != null && System.currentTimeMillis() - gainedAt < 3_000L) continue;
                show(id == null ? name : id, shownName, Long.parseLong(m.group(1).replace(",", "")));
            }
        }
    }

    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        out.add(component);
        for (Component sibling : component.getSiblings()) out.addAll(flatten(sibling));
        return out;
    }

    private static void show(String id, String name, long amount) {
        FeatureConfigs.ItemNotification c = config();
        long duration = (c == null ? 5 : c.seconds) * 1000L;
        Shown s = SHOWN.computeIfAbsent(id.toLowerCase(Locale.ROOT), k -> new Shown());
        s.amount += amount;
        s.name = name;
        s.id = id;
        s.until = System.currentTimeMillis() + duration;
        if (c != null && c.sound) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
        }
    }

    /** The stack's name with its colour, as legacy colour codes. */
    private static String legacyName(ItemStack stack) {
        Component name = Compat.realName(stack);
        net.minecraft.network.chat.TextColor colour = name.getStyle().getColor();
        if (colour == null) {
            for (Component sibling : name.getSiblings()) {
                if (sibling.getStyle().getColor() != null) {
                    colour = sibling.getStyle().getColor();
                    break;
                }
            }
        }
        String code = "";
        if (colour != null) {
            for (ChatFormatting f : ChatFormatting.values()) {
                net.minecraft.network.chat.TextColor legacy = net.minecraft.network.chat.TextColor.fromLegacyFormat(f);
                if (legacy != null && legacy.getValue() == colour.getValue()) {
                    code = f.toString();
                    break;
                }
            }
        }
        return code + name.getString();
    }

    // ---------------------------------------------------------------- commands

    private static int add(String item) {
        String name = item.trim();
        Set<String> list = new LinkedHashSet<>(items());
        for (String existing : list) {
            if (existing.equalsIgnoreCase(name)) return say(Component.literal(name + " is already on your Item Notification list.").withStyle(ChatFormatting.YELLOW));
        }
        list.add(name);
        save(list);
        String hint = RepoItems.idByName(name) == null && !name.contains("_")
            ? " (couldn't find an item called that; check the spelling)" : "";
        return say(Component.literal("Added " + name + " to your Item Notification list." + hint).withStyle(ChatFormatting.GREEN));
    }

    private static int remove(String item) {
        List<String> list = items();
        if (!list.removeIf(n -> n.equalsIgnoreCase(item.trim()))) {
            return say(Component.literal(item + " isn't on your Item Notification list.").withStyle(ChatFormatting.YELLOW));
        }
        save(new LinkedHashSet<>(list));
        return say(Component.literal("Removed " + item + " from your Item Notification list.").withStyle(ChatFormatting.GREEN));
    }

    private static int list() {
        List<String> list = items();
        if (list.isEmpty()) return say(Component.literal("Your Item Notification list is empty. Add one with /sb itemnotify add <item name>.").withStyle(ChatFormatting.YELLOW));
        return say(Component.literal("Item Notification (" + list.size() + "): ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(String.join(", ", list)).withStyle(ChatFormatting.WHITE)));
    }

    private static void save(Set<String> list) {
        SkyBallsConfig c = SkyBallsConfig.current();
        if (c == null) return;
        c.misc.itemNotification.items = String.join("\n", list);
        SkyBallsConfig.saveCurrent(c);
    }

    private static int say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[SB] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(message));
        });
        return 1;
    }

    // ---------------------------------------------------------------- HUD (laid out like the farming RNG HUD)

    private static String coins(double value) {
        if (value >= 1_000_000_000) return compact(value / 1_000_000_000, "b");
        if (value >= 1_000_000) return compact(value / 1_000_000, "m");
        if (value >= 1_000) return compact(value / 1_000, "k");
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String compact(double value, String suffix) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "") + suffix;
    }

    /** One line on the RNG HUD: "5x Enchanted Diamond" (with colour codes), its total price, and the plain name. */
    public record Row(String item, String price, String plain) {}

    /** What Item Notification is showing right now, for the RNG HUD. */
    public static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (!enabled()) return rows;
        for (Shown s : SHOWN.values()) {
            double unit = SkyBallsPriceTooltip.unitPrice(s.id);
            rows.add(new Row(s.amount + "x " + s.name, unit > 0 ? coins(unit * s.amount) : "—",
                ChatFormatting.stripFormatting(s.name).trim()));
        }
        return rows;
    }
}
