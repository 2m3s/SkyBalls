package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsNopoFeatures;
import com.epic60869.skyballs.custom.RepoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hypixel item emojis in SkyBalls chat, like the SkyHelper Discord: ":summoning_eye:" shows the Summoning Eye's icon.
 * Every SkyBlock item works by its id in lower case. Head items show their own skin; items Hypixel draws with its
 * resource pack (item_model, e.g. the Summoning Eye) show Hypixel's texture while that pack is loaded (on Hypixel), and
 * their plain Minecraft material otherwise. Client side only: the message itself stays ":summoning_eye:".
 *
 * Also the emoji autocomplete: a short, cached list of matches (typing ":" no longer lists thousands of emojis, which
 * lagged), with each emoji's picture next to its name.
 */
public final class ItemEmojis {
    private static final Pattern EMOJI = Pattern.compile(":([a-z0-9_]+):");
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");
    private static final int MAX_SUGGESTIONS = 50;
    /** Width of the picture drawn before each suggestion, plus its gap. */
    public static final int PREVIEW_WIDTH = 11;

    /** Emoji name ("summoning_eye") -> SkyBlock item id ("SUMMONING_EYE"). */
    private static final Map<String, String> ITEMS = new HashMap<>();
    private static final Map<String, ItemStack> STACKS = new HashMap<>();
    /** Every emoji shortcode (":name:"), sorted; built once, rebuilt when the item list loads. */
    private static List<String> allSuggestions;
    private static int nopoCountAtBuild = -1;

    private ItemEmojis() {}

    public static void init() {
        RepoItems.runAfterItemsLoaded(() -> Minecraft.getInstance().execute(ItemEmojis::loadItems));
    }

    private static void loadItems() {
        ITEMS.clear();
        for (String id : RepoItems.allIds()) {
            String name = id.toLowerCase(Locale.ROOT);
            if (NAME.matcher(name).matches()) ITEMS.put(name, id);
        }
        allSuggestions = null;
    }

    private static SkyBallsConfig.Chat chat() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.chat;
    }

    public static boolean itemEmojisEnabled() {
        SkyBallsConfig.Chat c = chat();
        return c != null && c.itemEmojis;
    }

    public static boolean autocompleteEnabled() {
        SkyBallsConfig.Chat c = chat();
        return c != null && c.emojiAutocomplete && (c.chatEmoji || c.itemEmojis);
    }

    // ---------------------------------------------------------------- chat

    /** SkyBalls chat: ":summoning_eye:" becomes the item's icon (hover shows its name). Nopo emojis are left to Nopo's renderer. */
    public static Component replace(Component message) {
        if (!itemEmojisEnabled() || ITEMS.isEmpty() || !message.getString().contains(":")) return message;
        MutableComponent result = Component.empty();
        message.visit((style, value) -> {
            if (value == null || value.isEmpty()) return Optional.empty();
            Matcher m = EMOJI.matcher(value);
            int cursor = 0;
            while (m.find()) {
                String name = m.group(1);
                Component icon = SkyBallsNopoFeatures.isChatEmoji(name) ? null : icon(name);
                if (icon == null) continue;
                if (m.start() > cursor) result.append(Component.literal(value.substring(cursor, m.start())).withStyle(style));
                result.append(icon);
                cursor = m.end();
            }
            if (cursor < value.length()) result.append(Component.literal(value.substring(cursor)).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    /** The item's icon as a chat picture, or null if it isn't an item (or has no picture). */
    private static Component icon(String name) {
        String id = ITEMS.get(name);
        if (id == null) return null;
        ItemStack stack = stack(name);
        Component icon = null;
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile != null) {
            icon = Component.object(new PlayerSprite(profile, true));
        } else {
            Identifier sprite = hypixelSprite(id);
            if (sprite == null) sprite = itemSprite(stack);
            if (sprite != null) icon = Component.object(new AtlasSprite(exists(AtlasIds.ITEMS, sprite) ? AtlasIds.ITEMS : AtlasIds.BLOCKS, sprite));
        }
        if (icon != null) {
            String display = RepoItems.displayName(id);
            icon = icon.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withHoverEvent(new HoverEvent.ShowText(
                Component.literal(display == null ? id : display).append(Component.literal("\n:" + name + ":").withStyle(ChatFormatting.DARK_GRAY)))));
        }
        return icon;
    }

    private static ItemStack stack(String name) {
        return STACKS.computeIfAbsent(name, n -> RepoItems.itemStack(ITEMS.get(n)));
    }

    /**
     * The item as drawn in the autocomplete: with Hypixel's item model when Hypixel's resource pack has it (on Hypixel),
     * otherwise the plain material. Checked each time, since the pack only loads once you join Hypixel.
     */
    private static ItemStack previewStack(String name) {
        ItemStack stack = stack(name);
        Identifier model = hypixelModel(ITEMS.get(name));
        if (model == null) return stack;
        ItemStack withModel = stack.copy();
        withModel.set(DataComponents.ITEM_MODEL, model);
        return withModel;
    }

    /** Hypixel's item model for the item, if Hypixel's resource pack is loaded and has it. */
    private static Identifier hypixelModel(String id) {
        String model = id == null ? null : RepoItems.itemModel(id);
        Identifier modelId = model == null ? null : Identifier.tryParse(model);
        if (modelId == null) return null;
        try {
            var models = Minecraft.getInstance().getModelManager();
            var missing = models.getItemModel(Identifier.fromNamespaceAndPath("skyballs", "no_such_item_model"));
            return models.getItemModel(modelId) != missing ? modelId : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Hypixel's texture for the item (same path as its item model), if Hypixel's resource pack is loaded. */
    private static Identifier hypixelSprite(String id) {
        String model = RepoItems.itemModel(id);
        Identifier sprite = model == null ? null : Identifier.tryParse(model);
        if (sprite == null) return null;
        if (exists(AtlasIds.ITEMS, sprite)) return sprite;
        if (exists(AtlasIds.BLOCKS, sprite)) return sprite;
        return null;
    }

    /** The Minecraft texture of a non-head item: item/&lt;id&gt; in the items atlas, else block/&lt;id&gt;; null if neither exists. */
    private static Identifier itemSprite(ItemStack stack) {
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier item = Identifier.fromNamespaceAndPath(key.getNamespace(), "item/" + key.getPath());
        if (exists(AtlasIds.ITEMS, item)) return item;
        Identifier block = Identifier.fromNamespaceAndPath(key.getNamespace(), "block/" + key.getPath());
        if (exists(AtlasIds.BLOCKS, block)) return block;
        return null;
    }

    private static boolean exists(Identifier atlasId, Identifier sprite) {
        try {
            TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(atlasId);
            return atlas.getSprite(sprite) != atlas.missingSprite();
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- autocomplete

    private static List<String> allSuggestions() {
        int nopo = SkyBallsNopoFeatures.chatEmojiNames().size();
        if (allSuggestions == null || nopo != nopoCountAtBuild) {
            TreeSet<String> names = new TreeSet<>();
            SkyBallsConfig.Chat c = chat();
            if (c == null || c.chatEmoji) names.addAll(SkyBallsNopoFeatures.chatEmojiNames());
            if (c == null || c.itemEmojis) names.addAll(ITEMS.keySet());
            List<String> list = new ArrayList<>(names.size());
            for (String n : names) list.add(":" + n + ":");
            allSuggestions = list;
            nopoCountAtBuild = nopo;
        }
        return allSuggestions;
    }

    /** Forget the cached list (a setting changed). */
    public static void invalidate() {
        allSuggestions = null;
    }

    /**
     * Up to 50 emojis for what's typed after ':' — names starting with it first, then names containing it — so the list
     * stays short and quick however many emojis there are.
     */
    public static List<String> suggestions(String typed) {
        String query = typed.startsWith(":") ? typed.substring(1).toLowerCase(Locale.ROOT) : typed.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        List<String> all = allSuggestions();
        for (String s : all) {
            if (s.startsWith(":" + query)) {
                out.add(s);
                if (out.size() >= MAX_SUGGESTIONS) return out;
            }
        }
        if (query.length() >= 2) {
            for (String s : all) {
                if (!s.startsWith(":" + query) && s.contains(query)) {
                    out.add(s);
                    if (out.size() >= MAX_SUGGESTIONS) return out;
                }
            }
        }
        return out;
    }

    /** Whether a suggestion is an emoji shortcode (gets a picture next to it). */
    public static boolean isEmojiSuggestion(String text) {
        if (text == null || text.length() < 3 || text.charAt(0) != ':' || text.charAt(text.length() - 1) != ':') return false;
        String name = text.substring(1, text.length() - 1);
        return SkyBallsNopoFeatures.isChatEmoji(name) || ITEMS.containsKey(name);
    }

    /** Draws the emoji's picture (9x9) at x, y for the autocomplete list. */
    public static void drawPreview(GuiGraphicsExtractor g, String text, int x, int y) {
        String name = text.substring(1, text.length() - 1);
        Identifier nopo = SkyBallsNopoFeatures.chatEmojiSprite(name);
        if (nopo != null) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, nopo, x, y, 9, 9);
            return;
        }
        if (!ITEMS.containsKey(name)) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(9f / 16f, 9f / 16f);
        g.item(previewStack(name), 0, 0);
        g.pose().popMatrix();
    }
}
