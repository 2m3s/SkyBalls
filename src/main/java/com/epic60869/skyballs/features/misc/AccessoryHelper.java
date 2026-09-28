package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.custom.RepoItems;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.sbc.Flags;
import com.epic60869.skyballs.features.sbc.Sbc;
import com.epic60869.skyballs.features.sbc.SbcConfig;
import com.epic60869.skyballs.features.sbc.SbcCrashReports;
import com.epic60869.skyballs.features.sbc.SbcInfo;
import com.epic60869.skyballs.mixin.SkyBallsContainerScreenAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Accessory helper, like Skyblocker's: next to the Accessory Bag, a scrollable list of the accessories (and upgrades)
 * you're missing, the most magical power per coin first, with price, rarity and the item's tooltip, and your magical
 * power. What you own is read from the bag's pages as you open them ({@link AccessoryTooltip}); the SkyBalls server
 * works out what's missing and the prices.
 */
public final class AccessoryHelper {
    private record Suggestion(String id, String name, String tier, int magicalPower, double price, String from, int gain, String skin) {
        int power() {
            return from.isEmpty() ? magicalPower : gain;
        }

        double perMillion() {
            return price <= 0 ? 0 : power() / (price / 1_000_000.0);
        }
    }

    private static final String BASE = "https://tastyfish.org/mod-api/api/v1/accessories";
    private static final Pattern TITLE = Pattern.compile("Accessory Bag(?: \\(\\d+/\\d+\\))?");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final int PANEL_W = 170;
    private static final int ROW = 18;
    private static final Map<String, Integer> TIER_COLOURS = Map.of("COMMON", 0xFFFFFF, "UNCOMMON", 0x55FF55, "RARE", 0x5555FF,
        "EPIC", 0xAA00AA, "LEGENDARY", 0xFFAA00, "MYTHIC", 0xFF55FF, "SPECIAL", 0xFF5555, "VERY_SPECIAL", 0xFF5555, "DIVINE", 0x55FFFF);
    private static final Map<String, Integer> TIER_POWER = Map.of("COMMON", 3, "UNCOMMON", 5, "RARE", 8, "EPIC", 12,
        "LEGENDARY", 16, "MYTHIC", 22, "SPECIAL", 3, "VERY_SPECIAL", 5, "DIVINE", 22);

    private static volatile List<Suggestion> suggestions = List.of();
    /** Every accessory (id -> tier, power, family), for your current magical power. */
    private static volatile Map<String, JsonObject> catalog = Map.of();
    private static Set<String> askedFor;
    private static long askedAt;
    private static volatile boolean loading;
    private static volatile String problem = "";
    private static int scroll;
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    private AccessoryHelper() {}

    private static SbcConfig.AccessoryHelper config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? new SbcConfig.AccessoryHelper() : c.misc.accessoryHelper;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            String title = ChatFormatting.stripFormatting(container.getTitle().getString());
            if (title == null || !TITLE.matcher(title.trim()).matches()) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
                try {
                    if (active()) render(container, graphics, mouseX, mouseY);
                } catch (Exception e) {
                    SbcCrashReports.report(e, "accessory helper");
                }
            });
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> {
                if (!active() || !overPanel(container, mouseX)) return true;
                scroll = Math.max(0, scroll - (int) Math.signum(vertical));
                return false;
            });
        });
    }

    private static boolean active() {
        return config().enabled && Flags.isEnabled("accessoryHelper");
    }

    private static int panelX(AbstractContainerScreen<?> screen) {
        return Math.max(2, ((SkyBallsContainerScreenAccessor) screen).skyballs$getLeftPos() - PANEL_W - 4);
    }

    private static boolean overPanel(AbstractContainerScreen<?> screen, double mouseX) {
        int x = panelX(screen);
        return mouseX >= x && mouseX < x + PANEL_W;
    }

    /** Asks the server again when what you own changed (at most every 3 seconds). */
    private static void update() {
        Set<String> owned = AccessoryTooltip.ownedAccessories();
        long now = System.currentTimeMillis();
        if (loading || (owned.equals(askedFor) && now - askedAt < 10 * 60_000L) || now - askedAt < 3_000L) return;
        askedFor = owned;
        askedAt = now;
        loading = true;
        if (catalog.isEmpty()) loadCatalog();
        JsonObject body = new JsonObject();
        JsonArray ids = new JsonArray();
        owned.forEach(ids::add);
        body.add("owned", ids);
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + "/missing")).timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json").header("User-Agent", "SkyBalls/" + SbcInfo.modVersion())
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() != 200) {
                problem = "The SkyBalls server couldn't work it out (HTTP " + response.statusCode() + ").";
                return;
            }
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            List<Suggestion> list = new ArrayList<>();
            for (JsonElement e : Sbc.arr(root, "missing")) if (e.isJsonObject()) list.add(parse(e.getAsJsonObject(), false));
            for (JsonElement e : Sbc.arr(root, "upgrades")) if (e.isJsonObject()) list.add(parse(e.getAsJsonObject(), true));
            // Most magical power per coin first; unknown prices last.
            list.sort((a, b) -> {
                if ((a.price() <= 0) != (b.price() <= 0)) return a.price() <= 0 ? 1 : -1;
                return Double.compare(b.perMillion(), a.perMillion());
            });
            suggestions = List.copyOf(list);
            problem = "";
        }).exceptionally(e -> {
            problem = "SBC offline: can't reach the SkyBalls server.";
            return null;
        }).whenComplete((v, e) -> loading = false);
    }

    private static Suggestion parse(JsonObject o, boolean upgrade) {
        String tier = Sbc.str(o, "tier").toUpperCase(Locale.ROOT);
        int power = (int) Sbc.lng(o, "magicalPower", TIER_POWER.getOrDefault(tier, 0));
        return new Suggestion(Sbc.str(o, "id"), Sbc.str(o, "name"), tier, power, Sbc.dbl(o, "price", 0),
            upgrade ? Sbc.str(o, "from") : "", (int) Sbc.lng(o, "gain", power), Sbc.str(o, "skin"));
    }

    private static void loadCatalog() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE)).timeout(Duration.ofSeconds(20))
            .header("User-Agent", "SkyBalls/" + SbcInfo.modVersion()).GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() != 200) return;
            Map<String, JsonObject> map = new HashMap<>();
            for (JsonElement e : Sbc.arr(JsonParser.parseString(response.body()).getAsJsonObject(), "accessories")) {
                if (e.isJsonObject()) map.put(Sbc.str(e.getAsJsonObject(), "id"), e.getAsJsonObject());
            }
            catalog = Map.copyOf(map);
        }).exceptionally(e -> null);
    }

    /** Your magical power from what you own: the best accessory of each family counts. */
    private static int magicalPower() {
        Map<String, Integer> best = new HashMap<>();
        for (String id : AccessoryTooltip.ownedAccessories()) {
            JsonObject o = catalog.get(id);
            if (o == null) continue;
            String tier = Sbc.str(o, "tier").toUpperCase(Locale.ROOT);
            int power = (int) Sbc.lng(o, "magicalPower", TIER_POWER.getOrDefault(tier, 0));
            String family = Sbc.str(o, "family");
            best.merge(family.isEmpty() ? id : family, power, Math::max);
        }
        return best.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static ItemStack stack(Suggestion s) {
        return STACKS.computeIfAbsent(s.id(), id -> {
            ItemStack repo = RepoItems.itemStack(id);
            if (repo != null && !repo.isEmpty()) return repo;
            if (s.skin().length() > 40) return Compat.createSkull(s.skin());
            return new ItemStack(Items.PAPER);
        });
    }

    private static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int mouseX, int mouseY) {
        update();
        Font font = Minecraft.getInstance().font;
        int x = panelX(screen);
        int top = Math.max(4, ((SkyBallsContainerScreenAccessor) screen).skyballs$getTopPos());
        int bottom = Math.min(screen.height - 4, top + 222);
        g.fill(x, top, x + PANEL_W, bottom, 0xE0121722);
        g.outline(x, top, PANEL_W, bottom - top, 0xFF3B465B);
        int power = catalog.isEmpty() ? -1 : magicalPower();
        g.text(font, "Missing Accessories", x + 4, top + 4, 0xFFFFD35A, true);
        g.text(font, "Magical Power: " + (power < 0 ? "..." : "≈" + power), x + 4, top + 15, 0xFFAA55FF, false);

        long maxPrice = config().maxPriceMillions * 1_000_000L;
        boolean upgrades = config().upgrades;
        List<Suggestion> list = new ArrayList<>();
        for (Suggestion s : suggestions) {
            if (!upgrades && !s.from().isEmpty()) continue;
            if (maxPrice > 0 && s.price() > maxPrice) continue;
            list.add(s);
        }
        int listTop = top + 28;
        if (list.isEmpty()) {
            String text = !problem.isEmpty() ? problem : loading || askedFor == null ? "Loading..."
                : AccessoryTooltip.ownedAccessories().isEmpty() ? "Open each page of your bag." : "You have them all!";
            g.textWithWordWrap(font, Component.literal(text), x + 4, listTop, PANEL_W - 8, 0xFFAAAAAA);
            return;
        }
        int rows = (bottom - listTop - 2) / ROW;
        scroll = Math.max(0, Math.min(scroll, list.size() - rows));
        Suggestion hovered = null;
        for (int i = scroll; i < Math.min(list.size(), scroll + rows); i++) {
            Suggestion s = list.get(i);
            int y = listTop + (i - scroll) * ROW;
            boolean hover = mouseX >= x && mouseX < x + PANEL_W && mouseY >= y && mouseY < y + ROW;
            if (hover) {
                g.fill(x + 1, y, x + PANEL_W - 1, y + ROW, 0x30FFFFFF);
                hovered = s;
            }
            g.item(stack(s), x + 3, y + 1);
            int colour = 0xFF000000 | TIER_COLOURS.getOrDefault(s.tier(), 0xFFFFFF);
            String name = (s.from().isEmpty() ? "" : "↑ ") + s.name();
            while (name.length() > 3 && font.width(name) > PANEL_W - 26) name = name.substring(0, name.length() - 2);
            g.text(font, name, x + 22, y + 1, colour, false);
            String detail = "+" + s.power() + " MP  " + (s.price() > 0 ? Sbc.shortNumber(s.price()) : "?");
            g.text(font, detail, x + 22, y + 10, 0xFF9AA5B8, false);
        }
        if (list.size() > rows) g.text(font, (scroll + 1) + "-" + Math.min(list.size(), scroll + rows) + "/" + list.size(), x + PANEL_W - 44, top + 4, 0xFF666666, false);
        if (hovered != null) {
            Suggestion s = hovered;
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(s.name()).withStyle(st -> st.withColor(TIER_COLOURS.getOrDefault(s.tier(), 0xFFFFFF))));
            tip.add(Component.literal(s.tier().replace('_', ' ')).withStyle(st -> st.withColor(TIER_COLOURS.getOrDefault(s.tier(), 0xFFFFFF)).withBold(true)));
            if (!s.from().isEmpty()) tip.add(Component.literal("Upgrade from " + s.from().replace('_', ' ')).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("+" + s.power() + " Magical Power").withStyle(ChatFormatting.LIGHT_PURPLE));
            tip.add(Component.literal("Price: " + (s.price() > 0 ? Sbc.number(Math.round(s.price())) + " coins" : "unknown")).withStyle(ChatFormatting.GOLD));
            if (s.price() > 0) tip.add(Component.literal(String.format(Locale.ENGLISH, "%.1f MP per million coins", s.perMillion())).withStyle(ChatFormatting.GRAY));
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }
}
