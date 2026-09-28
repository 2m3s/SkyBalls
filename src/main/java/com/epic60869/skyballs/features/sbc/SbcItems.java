package com.epic60869.skyballs.features.sbc;

import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.mixin.SkyBallsContainerScreenAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Items as the mod server sends them ({@code {id, name, lore[], count, rarity, skullTexture?}} with § codes): turning
 * your item into one to share in SkyBalls chat, and turning a received one back into an ItemStack, a tooltip or a
 * hoverable "[Item Name]" in chat.
 */
public final class SbcItems {
    private static final String[] RARITIES = {"VERY SPECIAL", "SPECIAL", "DIVINE", "MYTHIC", "LEGENDARY", "EPIC", "RARE",
        "UNCOMMON", "COMMON", "ULTIMATE", "ADMIN"};
    private static final int MAX_LORE = 40;
    private static final int MAX_LINE = 200;
    private static final int MAX_NAME = 100;

    /** Items shared in chat recently, so clicking one can show it. */
    private static final Map<Integer, JsonObject> SHARED = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, JsonObject> eldest) {
            return size() > 100;
        }
    };
    private static int nextShared = 1;

    private SbcItems() {}

    // ------------------------------------------------------------------------------------------------ outgoing

    /** The item in the slot under the mouse in an open container, or the one in your hand; empty if neither. */
    public static ItemStack heldOrHovered() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = ((SkyBallsContainerScreenAccessor) screen).skyballs$getHoveredSlot();
            if (slot != null && slot.hasItem()) return slot.getItem();
        }
        return mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
    }

    /** The item as the server wants it, or null for an empty stack. */
    public static JsonObject toJson(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        JsonObject item = new JsonObject();
        String id = Compat.neuName(stack);
        item.addProperty("id", id.isEmpty() ? stack.getItem().toString().toUpperCase(Locale.ROOT).replace("MINECRAFT:", "") : id);
        item.addProperty("name", limit(legacy(Compat.realName(stack)), MAX_NAME));
        JsonArray lore = new JsonArray();
        ItemLore itemLore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        for (Component line : itemLore.lines()) {
            if (lore.size() >= MAX_LORE) break;
            lore.add(limit(legacy(line), MAX_LINE));
        }
        item.add("lore", lore);
        item.addProperty("count", stack.getCount());
        String rarity = rarity(itemLore.lines());
        if (!rarity.isEmpty()) item.addProperty("rarity", rarity);
        String texture = Compat.getHeadTexture(stack);
        if (!texture.isEmpty() && texture.length() < 2000) item.addProperty("skullTexture", texture);
        return item;
    }

    /** "LEGENDARY" from the last lore line that names a rarity ("§6§lLEGENDARY SWORD"). */
    private static String rarity(List<Component> lore) {
        for (int i = lore.size() - 1; i >= 0; i--) {
            String text = ChatFormatting.stripFormatting(lore.get(i).getString());
            if (text == null) continue;
            text = text.trim().replaceFirst("^a ", "").toUpperCase(Locale.ROOT);
            for (String rarity : RARITIES) {
                if (text.startsWith(rarity)) return rarity.replace(' ', '_');
            }
        }
        return "";
    }

    private static String limit(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }

    /** A component as a string with § codes, like Hypixel item names and lore. */
    public static String legacy(Component component) {
        StringBuilder out = new StringBuilder();
        Style[] last = {null};
        component.visit((style, text) -> {
            if (text.isEmpty()) return Optional.empty();
            if (!style.equals(last[0])) {
                if (last[0] != null) out.append("§r");
                ChatFormatting colour = nearest(style.getColor());
                if (colour != null) out.append(colour);
                if (style.isObfuscated()) out.append("§k");
                if (style.isBold()) out.append("§l");
                if (style.isStrikethrough()) out.append("§m");
                if (style.isUnderlined()) out.append("§n");
                if (style.isItalic()) out.append("§o");
                last[0] = style;
            }
            out.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    private static ChatFormatting nearest(TextColor colour) {
        if (colour == null) return null;
        int rgb = colour.getValue();
        ChatFormatting best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (ChatFormatting f : ChatFormatting.values()) {
            TextColor legacy = TextColor.fromLegacyFormat(f);
            if (legacy == null) continue;
            int c = legacy.getValue();
            int dr = (c >> 16 & 255) - (rgb >> 16 & 255), dg = (c >> 8 & 255) - (rgb >> 8 & 255), db = (c & 255) - (rgb & 255);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                best = f;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------------------------------------ incoming

    /** A string with § codes as a styled component (so it works in tooltips and hovers). */
    public static MutableComponent parseLegacy(String text) {
        MutableComponent out = Component.empty();
        if (text == null || text.isEmpty()) return out;
        Style style = Style.EMPTY.withItalic(false);
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(text.charAt(i + 1)));
                if (f != null) {
                    if (!run.isEmpty()) {
                        out.append(Component.literal(run.toString()).setStyle(style));
                        run.setLength(0);
                    }
                    style = f == ChatFormatting.RESET ? Style.EMPTY.withItalic(false)
                        : TextColor.fromLegacyFormat(f) != null ? Style.EMPTY.withItalic(false).withColor(f) : style.applyFormat(f);
                    i++;
                    continue;
                }
            }
            run.append(c);
        }
        if (!run.isEmpty()) out.append(Component.literal(run.toString()).setStyle(style));
        return out;
    }

    public static String name(JsonObject item) {
        String name = Sbc.str(item, "name");
        return name.isEmpty() ? Sbc.str(item, "id") : name;
    }

    /** The item's name and lore, as a tooltip. */
    public static List<Component> tooltip(JsonObject item) {
        List<Component> lines = new ArrayList<>();
        lines.add(parseLegacy(name(item)));
        for (JsonElement line : Sbc.arr(item, "lore")) lines.add(parseLegacy(line.isJsonPrimitive() ? line.getAsString() : ""));
        return lines;
    }

    /** An ItemStack that looks like the item (its SkyBlock icon when known), with its name and lore. */
    public static ItemStack stack(JsonObject item) {
        String id = Sbc.str(item, "id");
        String texture = Sbc.str(item, "skullTexture");
        ItemStack stack;
        if (!texture.isEmpty()) {
            stack = Compat.createSkull(texture);
        } else {
            ItemStack repo = id.isEmpty() ? ItemStack.EMPTY : RepoItems.itemStack(id);
            stack = repo == null || repo.isEmpty() ? new ItemStack(Items.PAPER) : repo.copy();
        }
        stack.setCount((int) Math.max(1, Math.min(99, Sbc.lng(item, "count", 1))));
        stack.set(DataComponents.CUSTOM_NAME, parseLegacy(name(item)));
        List<Component> lore = tooltip(item);
        lore.removeFirst();
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    /** "[Item Name]" in the item's colour; hover shows the tooltip, click shows the item. */
    public static MutableComponent chatComponent(JsonObject item) {
        int key;
        synchronized (SHARED) {
            key = nextShared++;
            SHARED.put(key, item);
        }
        MutableComponent name = parseLegacy(name(item));
        TextColor colour = firstColour(name);
        long count = Sbc.lng(item, "count", 1);
        MutableComponent hover = Component.empty();
        List<Component> lines = tooltip(item);
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) hover.append("\n");
            hover.append(lines.get(i));
        }
        String plain = ChatFormatting.stripFormatting(name.getString());
        return Component.literal("[" + (count > 1 ? count + "x " : "") + plain + "]").withStyle(Style.EMPTY
            .withColor(colour == null ? TextColor.fromLegacyFormat(ChatFormatting.AQUA) : colour)
            .withHoverEvent(new HoverEvent.ShowText(hover))
            .withClickEvent(new ClickEvent.RunCommand("/sb viewitem " + key)));
    }

    private static TextColor firstColour(Component component) {
        TextColor[] found = {null};
        component.visit((style, text) -> {
            if (found[0] == null && !text.isBlank() && style.getColor() != null) found[0] = style.getColor();
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    static JsonObject shared(int key) {
        synchronized (SHARED) {
            return SHARED.get(key);
        }
    }
}
