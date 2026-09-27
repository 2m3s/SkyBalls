package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsStorageSearch;
import com.epic60869.skyballs.custom.util.Compat;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Storage overlay, working like Firmament's: in the /storage menu and in any Ender Chest page or backpack, every saved
 * page is shown at once in a scrollable grid (from the Storage Search cache), with your inventory at the bottom.
 * Click a page's name to open it. The page you have open is live: its items (and your inventory) can be clicked,
 * shift-clicked, swapped with number keys and dropped with Q like in the normal menu. Written for SkyBalls, not
 * copied from Firmament.
 */
public final class StorageOverlay {
    private static final int CELL = 18;
    private static final int PANEL_W = 9 * CELL + 8;
    private static final int HEADER = 12;
    private static final int GAP = 6;

    private static double scroll;

    /** Something drawn on the overlay that you can click. */
    private record Cell(int x, int y, ItemStack stack, int menuSlot) {}

    private record Header(int x, int y, int w, SkyBallsStorageSearch.StoragePage page) {}

    private record Layout(List<Cell> cells, List<Header> headers, int contentHeight, int viewTop, int viewBottom) {}

    private StorageOverlay() {}

    /** Hypixel's top row of every page (glass panes, Go Back arrow, Close barrier, ...) isn't shown; the page's items start at slot 9. */
    private static int firstItem(List<ItemStack> items) {
        return items.size() > 9 ? 9 : 0;
    }

    private static int rows(List<ItemStack> items) {
        return Math.max(1, (items.size() - firstItem(items) + 8) / 9);
    }

    private static boolean enabled() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c != null && c.misc.storageOverlay && Compat.isOnSkyblock();
    }

    /** Whether the overlay replaces this menu: the Storage menu, or an Ender Chest page / backpack. */
    public static boolean applies(AbstractContainerScreen<?> screen) {
        if (!enabled()) return false;
        String title = ChatFormatting.stripFormatting(screen.getTitle().getString()).trim();
        if (!title.equals("Storage") && SkyBallsStorageSearch.pageKey(title) == null) return false;
        // Nothing to show yet (the menu's items haven't arrived): leave the normal menu usable.
        return !pages(screen).isEmpty();
    }

    private static final Pattern MENU_ENDER_CHEST = Pattern.compile("^Ender Chest Page ([0-9]+)$");
    private static final Pattern MENU_BACKPACK = Pattern.compile("^Backpack Slot ([0-9]+)$");
    /** Pages seen in the Storage menu ("ENDER_CHEST:3"), so they can be opened from the overlay even before they're saved. */
    private static final Set<String> KNOWN = new LinkedHashSet<>();

    /**
     * Every page to show: the saved ones, the open one (live, even if it isn't saved yet) and any page the Storage menu
     * lists that hasn't been saved yet (an empty box you can click to open it).
     */
    private static List<SkyBallsStorageSearch.StoragePage> pages(AbstractContainerScreen<?> screen) {
        String title = ChatFormatting.stripFormatting(screen.getTitle().getString()).trim();
        if (title.equals("Storage")) {
            for (Slot slot : screen.getMenu().slots) {
                if (slot.container instanceof Inventory || slot.getItem().isEmpty()) continue;
                String name = ChatFormatting.stripFormatting(slot.getItem().getHoverName().getString()).trim();
                Matcher m = MENU_ENDER_CHEST.matcher(name);
                if (m.matches()) KNOWN.add("ENDER_CHEST:" + m.group(1));
                m = MENU_BACKPACK.matcher(name);
                if (m.matches()) KNOWN.add("BACKPACK:" + m.group(1));
            }
        }
        List<SkyBallsStorageSearch.StoragePage> out = new ArrayList<>(SkyBallsStorageSearch.storagePages());
        Set<String> have = new HashSet<>();
        for (SkyBallsStorageSearch.StoragePage page : out) have.add(page.type() + ":" + page.number());
        Set<String> wanted = new LinkedHashSet<>(KNOWN);
        String openKey = SkyBallsStorageSearch.pageKey(title);
        if (openKey != null) wanted.add(openKey);
        for (String key : wanted) {
            if (have.contains(key)) continue;
            String[] parts = key.split(":");
            int number;
            try {
                number = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (number <= 0) continue;
            boolean ender = parts[0].equals("ENDER_CHEST");
            out.add(new SkyBallsStorageSearch.StoragePage(parts[0], number,
                (ender ? "Ender Chest #" : "Backpack #") + number + (key.equals(openKey) ? "" : " (click to load)"), List.of()));
        }
        out.sort((x, y) -> x.type().equals(y.type()) ? Integer.compare(x.number(), y.number())
            : x.type().equals("ENDER_CHEST") ? -1 : 1);
        return out;
    }

    /** Until when a page switch is under way (Hypixel may close the menu for a moment before the next page opens). */
    private static long switchingUntil;

    /** Whether the overlay just asked for another page: the game shouldn't grab (and centre) the mouse meanwhile. */
    public static boolean switchingPage() {
        return System.currentTimeMillis() < switchingUntil;
    }

    private static void openPage(SkyBallsStorageSearch.StoragePage page) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switchingUntil = System.currentTimeMillis() + 1500;
        mc.player.connection.sendCommand((page.type().equals("ENDER_CHEST") ? "ec " : "bp ") + page.number());
    }

    public static void init() {
        // If no page opened after all, give the game its mouse back.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (switchingUntil != 0 && !switchingPage()) {
                switchingUntil = 0;
                if (mc.gui.screen() == null && !mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.grabMouse();
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
                if (applies(container)) render(container, g, mouseX, mouseY);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
                !applies(container) || !click(container, event.x(), event.y(), event.button()));
            // The release and drag must not reach the hidden menu either: releasing with an item on the cursor
            // "outside" it threw the item.
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !applies(container));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> !applies(container));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> {
                if (!applies(container)) return true;
                scroll -= vertical * 24;
                return false;
            });
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) ->
                !applies(container) || !keyPressed(container, event.key()));
        });
    }

    // ---------------------------------------------------------------- layout

    private static Layout layout(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        int width = screen.width;
        int height = screen.height;
        String title = ChatFormatting.stripFormatting(screen.getTitle().getString()).trim();
        String openKey = SkyBallsStorageSearch.pageKey(title);

        List<Cell> cells = new ArrayList<>();
        List<Header> headers = new ArrayList<>();

        // Your inventory, live, at the bottom.
        int invW = PANEL_W;
        int invX = (width - invW) / 2;
        int invY = height - (4 * CELL + 10);
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof Inventory)) continue;
            int index = slot.getContainerSlot();
            if (index < 0 || index >= 36) continue;
            int row = index < 9 ? 3 : (index - 9) / 9;
            int col = index % 9;
            cells.add(new Cell(invX + 4 + col * CELL, invY + (index < 9 ? 4 : 0) + row * CELL, slot.getItem(), slot.index));
        }

        // Pages, in columns that fit the screen.
        int viewTop = 20;
        int viewBottom = invY - 8;
        int columns = Math.max(1, Math.min(4, (width - 20 + GAP) / (PANEL_W + GAP)));
        int totalW = columns * PANEL_W + (columns - 1) * GAP;
        int left = (width - totalW) / 2;
        int[] columnY = new int[columns];
        List<SkyBallsStorageSearch.StoragePage> pages = pages(screen);
        for (SkyBallsStorageSearch.StoragePage page : pages) {
            boolean open = (page.type() + ":" + page.number()).equals(openKey);
            List<ItemStack> items = open ? liveItems(screen) : page.items();
            int rows = rows(items);
            int col = 0;
            for (int i = 1; i < columns; i++) if (columnY[i] < columnY[col]) col = i;
            int x = left + col * (PANEL_W + GAP);
            int y = viewTop + columnY[col] - (int) scroll;
            headers.add(new Header(x, y, PANEL_W, page));
            int first = firstItem(items);
            for (int i = first; i < items.size(); i++) {
                int shown = i - first;
                cells.add(new Cell(x + 4 + (shown % 9) * CELL, y + HEADER + (shown / 9) * CELL, items.get(i), open ? i : -1));
            }
            columnY[col] += HEADER + rows * CELL + 6 + GAP;
        }
        int contentHeight = 0;
        for (int h : columnY) contentHeight = Math.max(contentHeight, h);
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - (viewBottom - viewTop)));
        return new Layout(cells, headers, contentHeight, viewTop, viewBottom);
    }

    /** The open page's items straight from the menu. */
    private static List<ItemStack> liveItems(AbstractContainerScreen<?> screen) {
        List<ItemStack> out = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory) break;
            out.add(slot.getItem());
        }
        return out;
    }

    private static boolean inView(Layout layout, int y, int h) {
        return y + h > layout.viewTop() && y < layout.viewBottom();
    }

    private static boolean isPageCell(Cell cell, Layout layout) {
        return cell.y() < layout.viewBottom() + 4;
    }

    // ---------------------------------------------------------------- drawing

    private static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Layout layout = layout(screen);
        String openKey = SkyBallsStorageSearch.pageKey(ChatFormatting.stripFormatting(screen.getTitle().getString()).trim());
        g.nextStratum();
        g.fill(0, 0, screen.width, screen.height, 0xFF101216);
        g.centeredText(mc.font, Component.literal("Storage").withStyle(ChatFormatting.BOLD), screen.width / 2, 6, 0xFFFFFFFF);

        if (layout.headers().isEmpty()) {
            g.centeredText(mc.font, Component.literal("Open your Ender Chest pages and backpacks once so SkyBalls can show them here.").withStyle(ChatFormatting.GRAY),
                screen.width / 2, screen.height / 3, 0xFFAAAAAA);
        }

        g.enableScissor(0, layout.viewTop(), screen.width, layout.viewBottom());
        for (Header header : layout.headers()) {
            int rows = rows(header.page().items());
            int h = HEADER + rows * CELL + 6;
            if (!inView(layout, header.y(), h)) continue;
            boolean open = (header.page().type() + ":" + header.page().number()).equals(openKey);
            boolean hovered = mouseX >= header.x() && mouseX < header.x() + header.w() && mouseY >= header.y() && mouseY < header.y() + HEADER;
            g.fill(header.x(), header.y(), header.x() + header.w(), header.y() + h, open ? 0xFF2A3A2A : 0xFF1C1F26);
            g.outline(header.x(), header.y(), header.w(), h, open ? 0xFF55FF55 : hovered ? 0xFFAAAAAA : 0xFF3A3F48);
            g.text(mc.font, Component.literal(header.page().label()).withStyle(open ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                header.x() + 4, header.y() + 2, 0xFFFFFFFF, false);
        }
        drawCells(g, layout, mouseX, mouseY, true);
        g.disableScissor();

        // Inventory panel.
        int invX = (screen.width - PANEL_W) / 2;
        int invY = screen.height - (4 * CELL + 10);
        g.fill(invX, invY - 4, invX + PANEL_W, invY + 4 * CELL + 8, 0xFF1C1F26);
        g.outline(invX, invY - 4, PANEL_W, 4 * CELL + 12, 0xFF3A3F48);
        drawCells(g, layout, mouseX, mouseY, false);

        // Tooltip, and the item on the cursor.
        for (Cell cell : layout.cells()) {
            if (!cell.stack().isEmpty() && over(cell, mouseX, mouseY) && (!isPageCell(cell, layout) || (mouseY >= layout.viewTop() && mouseY < layout.viewBottom()))) {
                g.setTooltipForNextFrame(mc.font, cell.stack(), mouseX, mouseY);
                break;
            }
        }
        ItemStack carried = screen.getMenu().getCarried();
        if (!carried.isEmpty()) {
            g.item(carried, mouseX - 8, mouseY - 8);
            g.itemDecorations(mc.font, carried, mouseX - 8, mouseY - 8);
        }
    }

    private static void drawCells(GuiGraphicsExtractor g, Layout layout, int mouseX, int mouseY, boolean pages) {
        Minecraft mc = Minecraft.getInstance();
        for (Cell cell : layout.cells()) {
            if (isPageCell(cell, layout) != pages) continue;
            if (pages && !inView(layout, cell.y(), CELL)) continue;
            g.fill(cell.x(), cell.y(), cell.x() + 16, cell.y() + 16, 0x40FFFFFF);
            if (cell.menuSlot() >= 0 && over(cell, mouseX, mouseY)) g.fill(cell.x(), cell.y(), cell.x() + 16, cell.y() + 16, 0x80FFFFFF);
            if (!cell.stack().isEmpty()) {
                g.item(cell.stack(), cell.x(), cell.y());
                g.itemDecorations(mc.font, cell.stack(), cell.x(), cell.y());
            }
        }
    }

    private static boolean over(Cell cell, double mouseX, double mouseY) {
        return mouseX >= cell.x() && mouseX < cell.x() + 16 && mouseY >= cell.y() && mouseY < cell.y() + 16;
    }

    // ---------------------------------------------------------------- input

    private static Cell hoveredCell(Layout layout, double mouseX, double mouseY) {
        for (Cell cell : layout.cells()) {
            if (!over(cell, mouseX, mouseY)) continue;
            if (isPageCell(cell, layout) && (mouseY < layout.viewTop() || mouseY >= layout.viewBottom())) continue;
            return cell;
        }
        return null;
    }

    private static boolean click(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        Layout layout = layout(screen);
        Minecraft mc = Minecraft.getInstance();
        // A page's name opens that page.
        for (Header header : layout.headers()) {
            if (mouseX >= header.x() && mouseX < header.x() + header.w() && mouseY >= header.y() && mouseY < header.y() + HEADER
                && mouseY >= layout.viewTop() && mouseY < layout.viewBottom()) {
                openPage(header.page());
                return true;
            }
        }
        Cell cell = hoveredCell(layout, mouseX, mouseY);
        if (cell == null) return true; // swallow clicks on the overlay background
        if (cell.menuSlot() < 0) {
            // An item on a page that isn't open: open that page.
            for (Header header : layout.headers()) {
                int rows = rows(header.page().items());
                if (mouseX >= header.x() && mouseX < header.x() + header.w() && mouseY >= header.y() && mouseY < header.y() + HEADER + rows * CELL + 6) {
                    openPage(header.page());
                    break;
                }
            }
            return true;
        }
        if (mc.gameMode == null || mc.player == null) return true;
        ContainerInput input = mc.hasShiftDown() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP;
        mc.gameMode.handleContainerInput(screen.getMenu().containerId, cell.menuSlot(), button == 1 ? 1 : 0, input, mc.player);
        return true;
    }

    private static boolean keyPressed(AbstractContainerScreen<?> screen, int key) {
        Minecraft mc = Minecraft.getInstance();
        if (key == GLFW.GLFW_KEY_ESCAPE || key == mc.options.keyInventory.getDefaultKey().getValue()) return false; // close as normal
        double mouseX = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double mouseY = mc.mouseHandler.getScaledYPos(mc.getWindow());
        Cell cell = hoveredCell(layout(screen), mouseX, mouseY);
        if (cell == null || cell.menuSlot() < 0 || mc.gameMode == null || mc.player == null) return false;
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            mc.gameMode.handleContainerInput(screen.getMenu().containerId, cell.menuSlot(), key - GLFW.GLFW_KEY_1, ContainerInput.SWAP, mc.player);
            return true;
        }
        if (key == GLFW.GLFW_KEY_Q) {
            mc.gameMode.handleContainerInput(screen.getMenu().containerId, cell.menuSlot(), mc.hasControlDown() ? 1 : 0, ContainerInput.THROW, mc.player);
            return true;
        }
        return false;
    }
}
