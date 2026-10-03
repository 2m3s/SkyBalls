package com.epic60869.skyballs.features.sbc;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * An inventory shared with [inv] (click its [Inventory] in chat): the armour, the inventory and the hotbar laid out
 * like your own inventory, with each item's tooltip on hover.
 */
public final class SbcInventoryScreen extends Screen {
    private static final int SLOT = 18;
    private static final int PANEL_WIDTH = 9 * SLOT + 14 + SLOT + 6;
    private static final int PANEL_HEIGHT = 4 * SLOT + 4 + 14 + 10;

    private final List<ItemStack> slots;

    /** {@code slots}: 0-8 the hotbar, 9-35 the inventory, 36-39 the armour from boots to helmet. */
    public SbcInventoryScreen(String owner, List<ItemStack> slots) {
        super(Component.literal(owner == null || owner.isEmpty() ? "Shared Inventory" : owner + "'s Inventory"));
        this.slots = slots;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        g.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0C6C6C6);
        g.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);
        g.text(font, title, left + 7, top + 6, 0xFF404040, false);

        int gridLeft = left + 7 + SLOT + 6;
        int gridTop = top + 18;
        ItemStack hovered = ItemStack.EMPTY;
        // Armour down the left, helmet at the top.
        for (int i = 0; i < 4; i++) {
            ItemStack stack = slot(39 - i);
            if (drawSlot(g, stack, left + 7, gridTop + i * SLOT, mouseX, mouseY)) hovered = stack;
        }
        // Inventory rows (9-35), then a gap, then the hotbar (0-8).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ItemStack stack = slot(9 + row * 9 + col);
                if (drawSlot(g, stack, gridLeft + col * SLOT, gridTop + row * SLOT, mouseX, mouseY)) hovered = stack;
            }
        }
        for (int col = 0; col < 9; col++) {
            ItemStack stack = slot(col);
            if (drawSlot(g, stack, gridLeft + col * SLOT, gridTop + 3 * SLOT + 4, mouseX, mouseY)) hovered = stack;
        }
        if (!hovered.isEmpty()) g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        g.centeredText(font, "Press Esc to close", width / 2, top + PANEL_HEIGHT + 6, 0xFFAAAAAA);
    }

    private ItemStack slot(int index) {
        return index < slots.size() ? slots.get(index) : ItemStack.EMPTY;
    }

    /** Draws a slot and its item; true if the mouse is over it. */
    private boolean drawSlot(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF8B8B8B);
        boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
        if (!stack.isEmpty()) {
            g.item(stack, x + 1, y + 1);
            g.itemDecorations(font, stack, x + 1, y + 1);
        }
        if (over) g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x80FFFFFF);
        return over && !stack.isEmpty();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
