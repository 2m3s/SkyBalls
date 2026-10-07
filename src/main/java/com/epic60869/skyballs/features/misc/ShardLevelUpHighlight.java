package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Highlights attribute shards you own enough of to level up (or unlock). In the Hunting Box each shard says both how
 * many it needs ("Syphon 3 shards to level up!") and how many you have ("Owned: 5 Shards"). The Attribute Menu only
 * says how many it needs, so it uses the amounts seen in the Hunting Box this session (SkyHanni's AttributeShardsData
 * reads the same lines).
 */
public final class ShardLevelUpHighlight {
    private static final Pattern HUNTING_BOX = Pattern.compile("^(?:\\(\\d+/\\d+\\) )?Hunting Box");
    private static final Pattern ATTRIBUTE_MENU = Pattern.compile("^(?:\\(\\d+/\\d+\\) )?Attribute Menu");
    private static final Pattern SYPHON = Pattern.compile("Syphon (\\d+) shards? to (?:level up|unlock)!");
    private static final Pattern OWNED = Pattern.compile("Owned: ([\\d,]+) Shards?");
    /** The Hunting Box's lore line naming the attribute: "Veil (Combat)", "Yummy X (Foraging)". */
    private static final Pattern BOX_NAME = Pattern.compile("^(.+?)(?: [IVXL]+)? \\(\\w+\\)$");
    /** The Attribute Menu's item name: "Berry Eater IX", "Nature Elemental". */
    private static final Pattern MENU_NAME = Pattern.compile("^(.+?)(?: [IVXL]+)?$");
    private static final int COLOUR = 0x8055FF55;

    /** Shards owned in the Hunting Box, by attribute name. */
    private static final Map<String, Integer> OWNED_BY_NAME = new ConcurrentHashMap<>();
    /** Whether each item can level up, so lore isn't read every frame. */
    private static final Map<ItemStack, Boolean> CACHE = new WeakHashMap<>();

    private ShardLevelUpHighlight() {}

    /** Drawn behind the slot's item, from SkyBallsSlotBackgroundMixin. */
    public static void renderSlot(GuiGraphicsExtractor graphics, Slot slot) {
        SkyBallsConfig config = SkyBallsConfig.current();
        if (config == null || !config.misc.shardLevelUpHighlight || !slot.hasItem()) return;
        if (slot.container instanceof Inventory || !Compat.isOnSkyblock()) return;
        if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        String title = SkyBallsLocation.strip(screen.getTitle().getString());
        boolean box = HUNTING_BOX.matcher(title).find();
        if (!box && !ATTRIBUTE_MENU.matcher(title).find()) return;
        ItemStack stack = slot.getItem();
        Boolean ready = CACHE.get(stack);
        if (ready == null) {
            ready = box ? readBoxShard(stack) : readMenuShard(stack);
            CACHE.put(stack, ready);
        }
        if (ready) graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, COLOUR);
    }

    private static boolean readBoxShard(ItemStack stack) {
        String name = null;
        int needed = -1;
        int owned = -1;
        for (Component line : lore(stack)) {
            String text = SkyBallsLocation.strip(line.getString()).trim();
            Matcher m;
            if (name == null && (m = BOX_NAME.matcher(text)).matches()) name = m.group(1);
            if ((m = SYPHON.matcher(text)).find()) needed = Integer.parseInt(m.group(1));
            if ((m = OWNED.matcher(text)).find()) owned = Integer.parseInt(m.group(1).replace(",", ""));
        }
        if (name != null && owned >= 0) OWNED_BY_NAME.put(name, owned);
        return needed > 0 && owned >= needed;
    }

    private static boolean readMenuShard(ItemStack stack) {
        int needed = -1;
        for (Component line : lore(stack)) {
            Matcher m = SYPHON.matcher(SkyBallsLocation.strip(line.getString()));
            if (m.find()) needed = Integer.parseInt(m.group(1));
        }
        if (needed <= 0) return false;
        Matcher m = MENU_NAME.matcher(SkyBallsLocation.strip(stack.getHoverName().getString()).trim());
        if (!m.matches()) return false;
        Integer owned = OWNED_BY_NAME.get(m.group(1));
        return owned != null && owned >= needed;
    }

    private static java.util.List<Component> lore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? java.util.List.of() : lore.lines();
    }
}
