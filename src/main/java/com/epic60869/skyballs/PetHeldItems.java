package com.epic60869.skyballs;

import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What each of your pets is holding, for the pet HUD. Hypixel's tab list doesn't say, so like SkyHanni it is
 * gathered from the Pets menu (every pet's data has its held item), "Your pet is now holding X." and "You removed X
 * from your pet!", and Autopet's "Held Item: X" hover. Saved per pet name, so switching pets shows the right item.
 */
public final class PetHeldItems {
    private static final Pattern NOW_HOLDING = Pattern.compile("^Your pet is now holding (?<item>.+?)\\.$");
    private static final Pattern REMOVED = Pattern.compile("^You removed (?<item>.+?) from your pet!$");
    private static final Pattern AUTOPET = Pattern.compile("^Autopet equipped your (?<pet>.+?)! VIEW RULE$");
    private static final Pattern HOVER_HELD = Pattern.compile(".*Held Item: (?<item>.+)$");
    private static final Gson GSON = new Gson();

    /** Pet key (see {@link #key}) -> held item id, or "" for none. */
    private static final Map<String, String> HELD = new ConcurrentHashMap<>();
    private static Path file;
    private static Supplier<String> currentPet = () -> "";

    private PetHeldItems() {}

    /** {@code current}: the pet the HUD shows now, for "Your pet is now holding X." (which doesn't name it). */
    public static void init(Path configDir, Supplier<String> current) {
        file = configDir.resolve("skyballs").resolve("pet-held-items.json");
        currentPet = current;
        load();
        SkyBallsChat.onGameMessage((component, overlay) -> {
            if (!overlay) onMessage(component);
        });
    }

    /** "[Lvl 200] [122✦] Golden Dragon ✦" -> "golden dragon". */
    private static String key(String petName) {
        String name = SkyBallsLocation.strip(petName).replaceAll("\\[Lvl \\d+]", "").replaceAll("\\[[^]]*✦]", "")
            .replace("✦", "").trim();
        return name.toLowerCase(Locale.ROOT);
    }

    /** From the Pets menu: {@code heldItem} is the item id from the pet's data, or null/"" when it holds nothing. */
    public static void fromMenu(String petName, String heldItem) {
        set(petName, heldItem == null ? "" : heldItem);
    }

    private static void set(String petName, String id) {
        String key = key(petName);
        if (key.isEmpty()) return;
        String old = HELD.put(key, id);
        if (!id.equals(old)) save();
    }

    /** The held item's name in its rarity colour, or null if unknown or none. */
    public static Component heldItem(String petName) {
        String id = HELD.get(key(petName));
        if (id == null || id.isEmpty()) return null;
        // The repo item's name carries its rarity colour ("Lucky Clover" in the colour of its rarity).
        net.minecraft.world.item.ItemStack stack = RepoItems.itemStack(id);
        if (!stack.isEmpty() && !stack.is(net.minecraft.world.item.Items.BARRIER)) return stack.getHoverName().copy();
        String name = RepoItems.displayName(id);
        return Component.literal(name != null ? name : id.replace('_', ' ')).withStyle(net.minecraft.ChatFormatting.WHITE);
    }

    private static void onMessage(Component component) {
        String text = SkyBallsLocation.strip(component.getString()).trim();
        Matcher m;
        if ((m = NOW_HOLDING.matcher(text)).matches()) {
            String pet = currentPet.get();
            if (!pet.isEmpty()) set(pet, idFor(m.group("item")));
        } else if (REMOVED.matcher(text).matches()) {
            String pet = currentPet.get();
            if (!pet.isEmpty()) set(pet, "");
        } else if ((m = AUTOPET.matcher(text)).matches()) {
            String pet = m.group("pet");
            String held = hoverHeldItem(component);
            set(pet, held == null ? "" : idFor(held));
        }
    }

    /** The "Held Item: X" line in the Autopet message's hover, or null when the pet holds nothing. */
    private static String hoverHeldItem(Component component) {
        if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowText(Component hover)) {
            for (String line : SkyBallsLocation.strip(hover.getString()).split("\n")) {
                Matcher m = HOVER_HELD.matcher(line.trim());
                if (m.matches()) return m.group("item").trim();
            }
        }
        for (Component sibling : component.getSiblings()) {
            String found = hoverHeldItem(sibling);
            if (found != null) return found;
        }
        return null;
    }

    /** An item's id from its name ("Lucky Clover" -> "PET_ITEM_LUCKY_CLOVER"), or the name itself if unknown. */
    private static String idFor(String name) {
        String id = RepoItems.idByName(name.trim());
        return id != null ? id : name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            root.entrySet().forEach(e -> HELD.put(e.getKey(), e.getValue().getAsString()));
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not read pet-held-items.json: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(Map.copyOf(HELD)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not save pet-held-items.json: " + e.getMessage());
        }
    }
}
