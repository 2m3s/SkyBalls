package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsStorageSearch;
import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.custom.util.Compat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accessory tooltip, ported from Skyblocker's AccessoriesHelper and AccessoryTooltip: whether you're missing an
 * accessory, already have it, or whether it's an upgrade or downgrade of the one you have from the same family
 * ("(2→3/4)": you have tier 2, this is tier 3, the family's best is tier 4). Families and tiers come from the
 * accessory list Skyblocker uses (hysky.de); what you own is read, per profile, from your Accessory Bag pages.
 */
public final class AccessoryTooltip {
    private static final String ACCESSORIES_URL = "https://hysky.de/api/accessories";
    private static final Pattern ACCESSORY_BAG_TITLE = Pattern.compile("Accessory Bag(?: \\((?<page>\\d+)/\\d+\\))?");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Gson GSON = new Gson();

    private static final int COLLECTED_COLOUR = 0x55FF55;
    private static final int UPGRADE_COLOUR = 0x218BFF;
    private static final int UPGRADABLE_COLOUR = 0xF8D048;
    private static final int DOWNGRADE_COLOUR = 0xAAAAAA;
    private static final int MISSING_COLOUR = 0xFF5555;
    private static final int DETAIL_COLOUR = 0xF8F8FF;

    /** Accessory id -> its family, tier and origin, from Aaron's Mod's data (via hysky.de). */
    private static volatile Map<String, Accessory> accessories = Map.of();
    /** Profile id -> Accessory Bag page -> accessory ids on that page. */
    private static final Map<String, Map<Integer, Set<String>>> PROFILES = new ConcurrentHashMap<>();
    private static Path file;

    private record Accessory(String id, Optional<String> family, int tier, Optional<String> origin) {
        boolean sameFamily(Accessory other) {
            return family.isPresent() && family.equals(other.family);
        }
    }

    private enum Report { HAS_HIGHEST_TIER, IS_GREATER_TIER, HAS_GREATER_TIER, OWNS_BETTER_TIER, MISSING, INELIGIBLE }

    private AccessoryTooltip() {}

    public static void init(Path configDir) {
        file = configDir.resolve("skyballs").resolve("accessory-bag.json");
        load();
        RepoItems.runAsync(AccessoryTooltip::loadAccessories);

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            String title = ChatFormatting.stripFormatting(container.getTitle().getString());
            Matcher matcher = ACCESSORY_BAG_TITLE.matcher(title == null ? "" : title.trim());
            if (!matcher.matches()) return;
            int page = matcher.group("page") != null ? Integer.parseInt(matcher.group("page")) : 1;
            int[] ticks = {0};
            ScreenEvents.afterTick(screen).register(s -> {
                if (++ticks[0] % 10 == 1) collect(container, page);
            });
        });

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            SkyBallsConfig config = SkyBallsConfig.current();
            if (config == null || !config.misc.collectionTooltips.accessories || !Compat.isOnSkyblock()) return;
            addTooltip(stack, lines);
        });
    }

    private static void loadAccessories() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ACCESSORIES_URL)).timeout(Duration.ofSeconds(15))
                .header("User-Agent", "SkyBalls/1.0").GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[SkyBalls] Accessory list download failed: HTTP " + response.statusCode());
                return;
            }
            Map<String, Accessory> loaded = new HashMap<>();
            for (var entry : JsonParser.parseString(response.body()).getAsJsonObject().entrySet()) {
                JsonObject o = entry.getValue().getAsJsonObject();
                loaded.put(entry.getKey(), new Accessory(entry.getKey(),
                    o.has("family") ? Optional.of(o.get("family").getAsString()) : Optional.empty(),
                    o.has("tier") ? o.get("tier").getAsInt() : 0,
                    o.has("origin") ? Optional.of(o.get("origin").getAsString()) : Optional.empty()));
            }
            accessories = loaded;
        } catch (Exception e) {
            System.err.println("[SkyBalls] Accessory list download failed: " + e.getMessage());
        }
    }

    /** Remembers the accessories on this Accessory Bag page (the bag's slots, not your inventory below it). */
    private static void collect(AbstractContainerScreen<?> container, int page) {
        List<Slot> slots = container.getMenu().slots;
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < slots.size() - 36; i++) {
            String id = Compat.neuName(slots.get(i).getItem());
            if (!id.isEmpty()) ids.add(id);
        }
        Map<Integer, Set<String>> pages = PROFILES.computeIfAbsent(SkyBallsStorageSearch.currentProfile(), p -> new ConcurrentHashMap<>());
        if (ids.equals(pages.get(page))) return;
        pages.put(page, ids);
        save();
    }

    // ---------------------------------------------------------------- report (Skyblocker's calculateReport4Accessory)

    private record Result(Report report, String detail) {}

    private static Result report(String accessoryId, Map<Integer, Set<String>> pages) {
        Accessory accessory = accessories.get(accessoryId);
        // Rift accessories don't go in the Accessory Bag.
        if (accessory == null || accessory.origin().orElse("").equals("RIFT")) return new Result(Report.INELIGIBLE, "");

        Set<Accessory> collected = new HashSet<>();
        for (Set<String> ids : pages.values()) {
            for (String id : ids) {
                Accessory a = accessories.get(id);
                if (a != null) collected.add(a);
            }
        }

        if (accessory.family().isEmpty()) {
            return new Result(collected.contains(accessory) ? Report.HAS_HIGHEST_TIER : Report.MISSING, "");
        }

        int highestInFamily = accessories.values().stream().filter(accessory::sameFamily)
            .mapToInt(Accessory::tier).max().orElse(accessory.tier());
        int highestCollected = collected.stream().filter(accessory::sameFamily)
            .mapToInt(Accessory::tier).max().orElse(-1);
        int tier = accessory.tier();

        if (highestCollected < 0) return new Result(Report.MISSING, "(%d/%d)".formatted(tier, highestInFamily));
        if (tier == highestInFamily && highestCollected == highestInFamily) return new Result(Report.HAS_HIGHEST_TIER, "");
        if (tier > highestCollected) return new Result(Report.IS_GREATER_TIER, "(%d→%d/%d)".formatted(highestCollected, tier, highestInFamily));
        if (tier < highestCollected) return new Result(Report.OWNS_BETTER_TIER, "(%d→%d/%d)".formatted(highestCollected, tier, highestInFamily));
        if (tier < highestInFamily) return new Result(Report.HAS_GREATER_TIER, "(%d/%d)".formatted(highestCollected, highestInFamily));
        return new Result(Report.MISSING, "(%d/%d)".formatted(tier, highestInFamily));
    }

    private static void addTooltip(ItemStack stack, List<Component> lines) {
        String id = Compat.neuName(stack);
        if (id.isEmpty() || !accessories.containsKey(id)) return;
        Map<Integer, Set<String>> pages = PROFILES.get(SkyBallsStorageSearch.currentProfile());
        Component title = Component.literal("Accessory: ").withColor(0xF57542);
        if (pages == null || pages.isEmpty()) {
            lines.add(title.copy().append(Component.literal("? Open your Accessory Bag to check").withStyle(ChatFormatting.GRAY)));
            return;
        }
        Result result = report(id, pages);
        Component state = switch (result.report()) {
            case HAS_HIGHEST_TIER -> Component.literal("✔ Collected").withColor(COLLECTED_COLOUR);
            case IS_GREATER_TIER -> Component.literal("✦ Upgrade ").withColor(UPGRADE_COLOUR).append(detail(result));
            case HAS_GREATER_TIER -> Component.literal("↑ Upgradable ").withColor(UPGRADABLE_COLOUR).append(detail(result));
            case OWNS_BETTER_TIER -> Component.literal("↓ Downgrade ").withColor(DOWNGRADE_COLOUR).append(detail(result));
            case MISSING -> Component.literal("✖ Missing ").withColor(MISSING_COLOUR).append(detail(result));
            case INELIGIBLE -> null;
        };
        if (state != null) lines.add(title.copy().append(state));
    }

    private static Component detail(Result result) {
        return Component.literal(result.detail()).withColor(DETAIL_COLOUR);
    }

    // ---------------------------------------------------------------- saving

    /** Every accessory id seen in the current profile's Accessory Bag pages (for the accessory helper). */
    public static Set<String> ownedAccessories() {
        Set<String> owned = new HashSet<>();
        Map<Integer, Set<String>> pages = PROFILES.get(SkyBallsStorageSearch.currentProfile());
        if (pages != null) pages.values().forEach(owned::addAll);
        return owned;
    }

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var profile : root.entrySet()) {
                Map<Integer, Set<String>> pages = new ConcurrentHashMap<>();
                for (var page : profile.getValue().getAsJsonObject().entrySet()) {
                    Set<String> ids = new HashSet<>();
                    for (JsonElement id : page.getValue().getAsJsonArray()) ids.add(id.getAsString());
                    pages.put(Integer.parseInt(page.getKey()), ids);
                }
                PROFILES.put(profile.getKey(), pages);
            }
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not read accessory-bag.json: " + e.getMessage());
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        PROFILES.forEach((profile, pages) -> {
            JsonObject o = new JsonObject();
            pages.forEach((page, ids) -> {
                JsonArray array = new JsonArray();
                ids.stream().sorted().forEach(array::add);
                o.add(String.valueOf(page), array);
            });
            root.add(profile, o);
        });
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Could not save accessory-bag.json: " + e.getMessage());
        }
    }
}
