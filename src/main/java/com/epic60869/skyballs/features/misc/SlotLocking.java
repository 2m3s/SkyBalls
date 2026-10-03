package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.mixin.SkyBallsContainerScreenAccessor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Item protection and slot binding. Protected items cannot be dropped; binding turns a shift-click into the same swap
 * you'd do with a number key.
 * <ul>
 *   <li>Protect: run /sb protect while holding an item. Protection follows the item as it moves between slots.</li>
 *   <li>Bind (Odin's Slot Binds): in your inventory, press the bind key (B) over a slot, then over another (one must be
 *   in the hotbar). Shift-clicking either then swaps them. Press B on a bound slot to remove the bind. A line joins
 *   bound slots while you hover one.</li>
 * </ul>
 */
public final class SlotLocking {
    private static Integer pendingBind;

    private SlotLocking() {}

    private static SkyBallsConfig.SlotLocking config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.misc.slotLocking;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !keyPressed(container, event.key()));
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> render(container, g, mouseX, mouseY));
            ScreenEvents.remove(screen).register(s -> pendingBind = null);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root)
                    .then(ClientCommands.literal("protect").executes(context -> toggleProtection())));
            }
        });
    }

    // ------------------------------------------------------------ protection

    private static String protectionKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String uuid = Compat.uuid(stack);
        if (!uuid.isBlank()) return "uuid:" + uuid;
        String id = Compat.neuName(stack);
        if (id.isBlank()) id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return "item:" + id;
    }

    public static boolean isProtected(ItemStack stack) {
        SkyBallsConfig.SlotLocking c = config();
        String key = protectionKey(stack);
        return c != null && !key.isEmpty() && c.protectedItems.contains(key);
    }

    private static int toggleProtection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        ItemStack stack = mc.player.getMainHandItem();
        String key = protectionKey(stack);
        if (key.isEmpty()) {
            say(Component.literal("Hold an item to protect or unprotect it.").withStyle(ChatFormatting.RED));
            return 0;
        }
        SkyBallsConfig.SlotLocking c = config();
        if (c == null) return 0;
        String name = Compat.realName(stack).getString();
        if (c.protectedItems.remove(key)) {
            say(Component.literal("Removed drop protection from ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(name).withStyle(ChatFormatting.WHITE)));
        } else {
            c.protectedItems.add(key);
            say(Component.literal("★ Protected ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" from dropping.").withStyle(ChatFormatting.GREEN)));
        }
        SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
        return 1;
    }

    /**
    * Called for every slot click in a menu (ContainerSolverScreenMixin); true cancels protected-item drops or handles
    * a shift-click on a bound slot.
     */
    public static boolean onSlotClicked(AbstractContainerScreen<?> screen, Slot slot, int button, ContainerInput input) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null) return false;
        if (input == ContainerInput.THROW || slot == null && input == ContainerInput.PICKUP) {
            ItemStack dropped = slot == null ? screen.getMenu().getCarried() : slot.getItem();
            if (isProtected(dropped)) {
                say(Component.literal("★ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("That item is protected. Use /sb protect to unprotect it.").withStyle(ChatFormatting.RED)));
                return true;
            }
        }
        if (!c.enabled) return false;

        // Slot binds: shift-click in your own inventory swaps with the bound hotbar slot.
        if (screen instanceof InventoryScreen && input == ContainerInput.QUICK_MOVE && slot != null) {
            int clicked = slot.index;
            Integer bound = boundTo(clicked);
            if (bound == null) return false;
            int from;
            int hotbar;
            if (clicked >= 36 && clicked <= 44) {
                from = bound;
                hotbar = clicked - 36;
            } else if (bound >= 36 && bound <= 44) {
                from = clicked;
                hotbar = bound - 36;
            } else {
                return false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode == null || mc.player == null) return false;
            mc.gameMode.handleContainerInput(screen.getMenu().containerId, from, hotbar, ContainerInput.SWAP, mc.player);
            return true;
        }
        return false;
    }

    /** Q in the world: nothing drops while the selected item is protected. */
    public static boolean blockDrop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        boolean blocked = isProtected(mc.player.getMainHandItem());
        if (blocked) say(Component.literal("★ ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("That item is protected. Use /sb protect to unprotect it.").withStyle(ChatFormatting.RED)));
        return blocked;
    }

    private static Integer boundTo(int menuSlot) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null) return null;
        Integer direct = c.binds.get(menuSlot);
        if (direct != null) return direct;
        for (var e : c.binds.entrySet()) if (e.getValue() == menuSlot) return e.getKey();
        return null;
    }

    private static boolean keyPressed(AbstractContainerScreen<?> screen, int key) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null || !c.enabled || key == GLFW.GLFW_KEY_UNKNOWN) return false;
        Slot hovered = ((SkyBallsContainerScreenAccessor) screen).skyballs$getHoveredSlot();
        if (key == c.bindKey && screen instanceof InventoryScreen) {
            if (hovered == null || hovered.index < 5 || hovered.index >= 45) return false;
            int clicked = hovered.index;
            if (pendingBind == null) {
                Integer existing = c.binds.containsKey(clicked) ? Integer.valueOf(clicked) : null;
                if (existing == null) {
                    for (var e : c.binds.entrySet()) if (e.getValue() == clicked) existing = e.getKey();
                }
                if (existing != null) {
                    int to = c.binds.remove(existing);
                    SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
                    say(Component.literal("Removed the bind between slots " + existing + " and " + to + ".").withStyle(ChatFormatting.YELLOW));
                    return true;
                }
                pendingBind = clicked;
                return true;
            }
            int first = pendingBind;
            pendingBind = null;
            if (first == clicked) {
                say(Component.literal("You can't bind a slot to itself.").withStyle(ChatFormatting.RED));
                return true;
            }
            if ((first < 36 || first > 44) && (clicked < 36 || clicked > 44)) {
                say(Component.literal("One of the two slots must be in your hotbar.").withStyle(ChatFormatting.RED));
                return true;
            }
            c.binds.put(first, clicked);
            SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
            say(Component.literal("Bound slot " + first + " to slot " + clicked + ". Shift-click either to swap them.").withStyle(ChatFormatting.GREEN));
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- drawing

    private static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int mouseX, int mouseY) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null) return;
        SkyBallsContainerScreenAccessor access = (SkyBallsContainerScreenAccessor) screen;
        int left = access.skyballs$getLeftPos();
        int top = access.skyballs$getTopPos();
        for (Slot slot : screen.getMenu().slots) {
            if (!isProtected(slot.getItem())) continue;
            int x = left + slot.x;
            int y = top + slot.y;
            g.outline(x, y, 16, 16, 0xFF55DD99);
            g.text(Minecraft.getInstance().font, "★", x + 8, y - 1, 0xFFFFD54F, true);
        }

        if (!c.enabled || !(screen instanceof InventoryScreen)) return;
        Slot hovered = access.skyballs$getHoveredSlot();
        Integer startIndex = pendingBind != null ? pendingBind : hovered == null ? null : Integer.valueOf(hovered.index);
        if (startIndex == null || startIndex < 5 || startIndex >= 45) return;
        Slot start = screen.getMenu().getSlot(startIndex);
        int sx = left + start.x + 8;
        int sy = top + start.y + 8;
        int ex;
        int ey;
        if (pendingBind != null) {
            ex = mouseX;
            ey = mouseY;
        } else {
            Integer bound = boundTo(startIndex);
            if (bound == null || (c.lineOnlyWithShift && !Minecraft.getInstance().hasShiftDown())) return;
            Slot end = screen.getMenu().getSlot(bound);
            ex = left + end.x + 8;
            ey = top + end.y + 8;
        }
        line(g, sx, sy, ex, ey, 0xFF55FF55);
    }

    /** A straight line made of small squares. */
    private static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int colour) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps);
            int y = y1 + (y2 - y1) * i / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    private static void say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(
            Component.literal("[SB] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(message));
    }
}
