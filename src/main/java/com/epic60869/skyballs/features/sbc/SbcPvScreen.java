package com.epic60869.skyballs.features.sbc;

import com.epic60869.skyballs.custom.RepoItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The profile viewer (/sb pv): overview, skills, slayers, dungeons, pets and every inventory (with the real item
 * tooltips) of a player's SkyBlock profile, with a profile switcher. Everything comes from the SkyBalls server's
 * pvResult; the items arrive already decoded.
 */
public final class SbcPvScreen extends Screen {
    private enum Tab { OVERVIEW("Overview"), SKILLS("Skills"), SLAYERS("Slayers"), DUNGEONS("Dungeons"), PETS("Pets"), ITEMS("Items");
        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private enum Inv { INVENTORY("Inventory", "inventory"), ARMOR("Armor", "armor"), WARDROBE("Wardrobe", "wardrobe"),
        ENDER("Ender Chest", "enderChest"), BACKPACKS("Backpacks", "backpacks"), ACCESSORIES("Accessories", "accessoryBag"),
        VAULT("Vault", "personalVault");
        final String label;
        final String key;

        Inv(String label, String key) {
            this.label = label;
            this.key = key;
        }
    }

    private static final int W = 420;
    private static final int H = 250;
    private static final int PANEL = 0xE0121722;
    private static final int BORDER = 0xFF3B465B;
    private static final int GOLD = 0xFFFFD35A;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF9AA5B8;
    private static final Map<String, Integer> RARITY = Map.ofEntries(
        Map.entry("COMMON", 0xFFFFFF), Map.entry("UNCOMMON", 0x55FF55), Map.entry("RARE", 0x5555FF), Map.entry("EPIC", 0xAA00AA),
        Map.entry("LEGENDARY", 0xFFAA00), Map.entry("MYTHIC", 0xFF55FF), Map.entry("DIVINE", 0x55FFFF), Map.entry("SPECIAL", 0xFF5555),
        Map.entry("VERY_SPECIAL", 0xFF5555), Map.entry("ULTIMATE", 0xAA0000), Map.entry("ADMIN", 0xAA0000));
    private static final String[] SKILLS = {"farming", "mining", "combat", "foraging", "fishing", "enchanting", "alchemy",
        "taming", "carpentry", "runecrafting", "social"};
    private static final String[] SLAYERS = {"zombie", "spider", "wolf", "enderman", "blaze", "vampire"};
    private static final String[] PET_TIERS = {"COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"};

    private final String username;
    private String requestId = "";
    private String error = "";
    private JsonObject result;
    private Tab tab = Tab.OVERVIEW;
    private Inv inv = Inv.INVENTORY;
    private int page;
    private int backpack;
    private int scroll;
    private boolean profileMenu;
    private final Map<JsonObject, ItemStack> stacks = new IdentityHashMap<>();
    /** Item tooltip to show this frame. */
    private JsonObject hovered;

    public SbcPvScreen(String username) {
        super(Component.literal("Profile Viewer"));
        this.username = username;
    }

    void waitFor(String id) {
        requestId = id;
        error = "";
    }

    void fail(String message) {
        error = message;
    }

    void receive(JsonObject packet) {
        String id = Sbc.str(packet, "requestId");
        if (!id.isEmpty() && !id.equals(requestId)) return;
        if (!Sbc.bool(packet, "ok")) {
            String message = Sbc.str(packet, "message");
            error = !message.isBlank() ? message : switch (Sbc.str(packet, "code")) {
                case "badName" -> "That isn't a Minecraft name.";
                case "unknownPlayer" -> "No player called " + username + ".";
                case "noProfiles" -> username + " hasn't played SkyBlock.";
                case "rateLimited" -> "Too many lookups; try again in a minute.";
                default -> "The profile couldn't be loaded right now.";
            };
            return;
        }
        result = packet;
        error = "";
        stacks.clear();
        page = 0;
        scroll = 0;
        profileMenu = false;
    }

    private JsonObject profile() {
        return Sbc.obj(result, "profile");
    }

    private int left() {
        return (width - Math.min(W, width - 10)) / 2;
    }

    private int panelW() {
        return Math.min(W, width - 10);
    }

    private int top() {
        return Math.max(26, (height - H) / 2);
    }

    @Override
    protected void init() {
        int x = left();
        for (Tab t : Tab.values()) {
            Button b = addRenderableWidget(Button.builder(Component.literal(t.label), btn -> {
                tab = t;
                page = 0;
                scroll = 0;
                rebuildWidgets();
            }).bounds(x, top() - 22, 64, 18).build());
            b.active = t != tab;
            x += 66;
        }
        if (tab == Tab.ITEMS) {
            int ix = left() + 4;
            for (Inv i : Inv.values()) {
                int w = font.width(i.label) + 10;
                Button b = addRenderableWidget(Button.builder(Component.literal(i.label), btn -> {
                    inv = i;
                    page = 0;
                    backpack = 0;
                    rebuildWidgets();
                }).bounds(ix, top() + 22, w, 16).build());
                b.active = i != inv;
                ix += w + 2;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        hovered = null;
        int left = left();
        int top = top();
        int right = left + panelW();
        int bottom = top + H;
        g.fill(left, top, right, bottom, PANEL);
        g.outline(left, top, panelW(), H, BORDER);

        String name = result == null ? username : Sbc.str(result, "username");
        g.text(font, name, left + 6, top + 6, GOLD, true);
        if (!error.isEmpty() && result == null) {
            g.centeredText(font, error, (left + right) / 2, top + 100, 0xFFFF5555);
            return;
        }
        if (result == null) {
            g.centeredText(font, "Loading " + username + "'s profile...", (left + right) / 2, top + 100, MUTED);
            return;
        }
        drawProfileSwitcher(g, right, top, mouseX, mouseY);
        if (!error.isEmpty()) g.centeredText(font, error, (left + right) / 2, bottom - 12, 0xFFFF5555);

        int y = top + 22;
        switch (tab) {
            case OVERVIEW -> drawOverview(g, left + 6, y, mouseX, mouseY);
            case SKILLS -> drawSkills(g, left + 6, y);
            case SLAYERS -> drawSlayers(g, left + 6, y);
            case DUNGEONS -> drawDungeons(g, left + 6, y);
            case PETS -> drawPets(g, left + 6, y, mouseX, mouseY);
            case ITEMS -> drawItems(g, left + 6, y + 20, mouseX, mouseY);
        }
        if (profileMenu) drawProfileMenu(g, right, top, mouseX, mouseY);
        if (hovered != null) g.setComponentTooltipForNextFrame(font, SbcItems.tooltip(hovered), mouseX, mouseY);
    }

    private JsonObject selectedProfile() {
        for (JsonElement e : Sbc.arr(result, "profiles")) {
            if (e.isJsonObject() && Sbc.bool(e.getAsJsonObject(), "selected")) return e.getAsJsonObject();
        }
        JsonArray all = Sbc.arr(result, "profiles");
        return all.isEmpty() || !all.get(0).isJsonObject() ? new JsonObject() : all.get(0).getAsJsonObject();
    }

    private String profileLabel(JsonObject p) {
        String mode = switch (Sbc.str(p, "gameMode")) {
            case "ironman" -> " ♲";
            case "bingo" -> " Ⓑ";
            case "island" -> " ☀";
            default -> "";
        };
        return Sbc.str(p, "cuteName") + mode;
    }

    private void drawProfileSwitcher(GuiGraphicsExtractor g, int right, int top, int mouseX, int mouseY) {
        String label = "Profile: " + profileLabel(selectedProfile()) + " ▼";
        int x = right - 6 - font.width(label);
        boolean hover = mouseX >= x - 3 && mouseX < right - 3 && mouseY >= top + 3 && mouseY < top + 15;
        if (hover) g.fill(x - 3, top + 3, right - 3, top + 15, 0x30FFFFFF);
        g.text(font, label, x, top + 6, TEXT, false);
    }

    private void drawProfileMenu(GuiGraphicsExtractor g, int right, int top, int mouseX, int mouseY) {
        JsonArray profiles = Sbc.arr(result, "profiles");
        int w = 120;
        int x = right - w - 3;
        int y = top + 16;
        g.fill(x, y, x + w, y + profiles.size() * 12 + 2, 0xF0101018);
        g.outline(x, y, w, profiles.size() * 12 + 2, BORDER);
        for (int i = 0; i < profiles.size(); i++) {
            JsonObject p = profiles.get(i).getAsJsonObject();
            int ry = y + 1 + i * 12;
            if (mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 12) g.fill(x + 1, ry, x + w - 1, ry + 12, 0x40FFFFFF);
            g.text(font, profileLabel(p), x + 4, ry + 2, Sbc.bool(p, "selected") ? GOLD : TEXT, false);
        }
    }

    private static double level(JsonElement e) {
        if (e == null || e.isJsonNull()) return 0;
        if (e.isJsonPrimitive()) return e.getAsDouble();
        if (e.isJsonObject()) return Sbc.dbl(e.getAsJsonObject(), "level", 0);
        return 0;
    }

    private static String coins(double value) {
        return Sbc.shortNumber(value);
    }

    private void drawOverview(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        JsonObject p = profile();
        JsonObject api = Sbc.obj(p, "apiEnabled");
        boolean bankApi = !api.has("bank") || Sbc.bool(api, "bank");
        boolean invApi = !api.has("inventory") || Sbc.bool(api, "inventory");
        JsonObject acc = Sbc.obj(p, "accessories");
        JsonObject dungeons = Sbc.obj(p, "dungeons");
        long joined = Sbc.lng(p, "joinedAt", 0);
        String[][] rows = {
            {"SkyBlock Level", String.format(Locale.ENGLISH, "%.2f", Sbc.dbl(p, "skyblockLevel", 0))},
            {"Purse", coins(Sbc.dbl(p, "purse", 0))},
            {"Bank", bankApi ? coins(Sbc.dbl(p, "bank", 0)) : "API off"},
            {"Fairy Souls", String.valueOf(Sbc.lng(p, "fairySouls", 0))},
            {"Skill Average", String.format(Locale.ENGLISH, "%.2f", Sbc.dbl(p, "skillAverage", 0))},
            {"Magical Power", invApi ? Sbc.number(Sbc.lng(acc, "magicalPower", 0)) + (Sbc.str(acc, "power").isEmpty() ? "" : " (" + Sbc.str(acc, "power") + ")") : "API off"},
            {"Catacombs", String.format(Locale.ENGLISH, "%.1f", level(dungeons.get("catacombs")))
                + (Sbc.str(dungeons, "selectedClass").isEmpty() ? "" : " (" + capital(Sbc.str(dungeons, "selectedClass")) + ")")},
            {"Joined", joined > 0 ? DateTimeFormatter.ofPattern("d MMM yyyy").format(Instant.ofEpochMilli(joined).atZone(ZoneId.systemDefault())) : "-"}};
        int ry = y;
        for (String[] row : rows) {
            g.text(font, row[0], x, ry, MUTED, false);
            g.text(font, row[1], x + 90, ry, row[1].equals("API off") ? 0xFFFF7F7F : TEXT, false);
            ry += 13;
        }
        JsonObject pet = Sbc.obj(p, "activePet");
        if (pet.size() > 0) {
            g.text(font, "Active pet:", x, ry + 4, MUTED, false);
            g.text(font, petName(pet), x + 90, ry + 4, 0xFF000000 | RARITY.getOrDefault(Sbc.str(pet, "tier"), 0xFFFFFF), false);
        }
        if (!invApi || !bankApi) {
            g.text(font, "Some API settings are off, so parts of this profile are hidden.", x, y + H - 50, 0xFFFF7F7F, false);
        }

        // Armor and equipment, like the player's inventory screen.
        JsonObject inventories = Sbc.obj(p, "inventories");
        int ax = x + 240;
        g.text(font, "Armor", ax, y, MUTED, false);
        g.text(font, "Equipment", ax + 44, y, MUTED, false);
        List<JsonObject> armor = items(Sbc.arr(inventories, "armor"));
        List<JsonObject> equipment = items(Sbc.arr(inventories, "equipment"));
        for (int i = 0; i < 4; i++) {
            // The API lists armor boots first.
            drawSlot(g, bySlot(armor, 3 - i, armor.size() > i ? armor.get(armor.size() - 1 - i) : null), ax, y + 12 + i * 20, mouseX, mouseY);
            drawSlot(g, bySlot(equipment, i, equipment.size() > i ? equipment.get(i) : null), ax + 44, y + 12 + i * 20, mouseX, mouseY);
        }
    }

    private static JsonObject bySlot(List<JsonObject> items, int slot, JsonObject fallback) {
        for (JsonObject o : items) if (o.has("slot") && Sbc.lng(o, "slot", -1) == slot) return o;
        return fallback;
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private void drawBar(GuiGraphicsExtractor g, int x, int y, int w, double progress, boolean maxed) {
        g.fill(x, y, x + w, y + 5, 0xFF2A3345);
        int filled = (int) Math.round(w * Math.max(0, Math.min(1, maxed ? 1 : progress)));
        g.fill(x, y, x + filled, y + 5, maxed ? 0xFFFFAA00 : 0xFF55CC55);
    }

    private void drawSkills(GuiGraphicsExtractor g, int x, int y) {
        JsonObject skills = Sbc.obj(profile(), "skills");
        if (skills.size() == 0) {
            g.text(font, "No skill data (the Skills API may be off).", x, y, 0xFFFF7F7F, false);
            return;
        }
        List<String> names = new ArrayList<>();
        for (String s : SKILLS) if (skills.has(s)) names.add(s);
        for (String s : skills.keySet()) if (!names.contains(s)) names.add(s);
        int colW = (panelW() - 18) / 2;
        for (int i = 0; i < names.size(); i++) {
            JsonObject s = Sbc.obj(skills, names.get(i));
            int cx = x + (i % 2) * (colW + 6);
            int cy = y + (i / 2) * 30;
            boolean maxed = Sbc.bool(s, "maxed");
            g.text(font, capital(names.get(i)) + " " + (int) level(s), cx, cy, maxed ? GOLD : TEXT, false);
            String xp = Sbc.shortNumber(Sbc.dbl(s, "xp", 0)) + " XP";
            g.text(font, xp, cx + colW - font.width(xp), cy, MUTED, false);
            drawBar(g, cx, cy + 11, colW, Sbc.dbl(s, "progress", 0), maxed);
        }
        g.text(font, "Skill average: " + String.format(Locale.ENGLISH, "%.2f", Sbc.dbl(profile(), "skillAverage", 0)), x, y + H - 44, GOLD, false);
    }

    private void drawSlayers(GuiGraphicsExtractor g, int x, int y) {
        JsonObject slayers = Sbc.obj(profile(), "slayers");
        int cy = y;
        for (String key : SLAYERS) {
            JsonObject s = Sbc.obj(slayers, key);
            if (s.size() == 0) continue;
            String name = Sbc.str(s, "name").isEmpty() ? capital(key) : Sbc.str(s, "name");
            g.text(font, name + "  Level " + Sbc.lng(s, "level", 0), x, cy, TEXT, false);
            g.text(font, Sbc.number(Sbc.lng(s, "xp", 0)) + " XP", x + 190, cy, MUTED, false);
            JsonObject kills = Sbc.obj(s, "kills");
            StringBuilder k = new StringBuilder();
            for (int t = 1; t <= 5; t++) {
                long n = Sbc.lng(kills, "t" + t, -1);
                if (n >= 0) k.append("T").append(t).append(": ").append(Sbc.number(n)).append("   ");
            }
            g.text(font, k.toString(), x + 8, cy + 11, MUTED, false);
            cy += 27;
        }
        if (cy == y) g.text(font, "No slayer data.", x, y, MUTED, false);
    }

    private void drawDungeons(GuiGraphicsExtractor g, int x, int y) {
        JsonObject d = Sbc.obj(profile(), "dungeons");
        JsonElement cata = d.get("catacombs");
        double cataLevel = level(cata);
        g.text(font, "Catacombs " + String.format(Locale.ENGLISH, "%.2f", cataLevel), x, y, GOLD, false);
        if (cata != null && cata.isJsonObject()) drawBar(g, x, y + 11, 150, Sbc.dbl(cata.getAsJsonObject(), "progress", cataLevel % 1), false);
        g.text(font, "Secrets: " + Sbc.number(Sbc.lng(d, "secrets", 0)), x + 170, y, TEXT, false);

        JsonObject classes = Sbc.obj(d, "classes");
        String selected = Sbc.str(d, "selectedClass");
        int cy = y + 24;
        for (String cls : classes.keySet()) {
            boolean sel = cls.equalsIgnoreCase(selected);
            g.text(font, (sel ? "▶ " : "") + capital(cls) + " " + String.format(Locale.ENGLISH, "%.1f", level(classes.get(cls))), x, cy, sel ? GOLD : TEXT, false);
            cy += 11;
        }

        // Floors: completions, master completions and fastest S / S+ times.
        int tx = x + 150;
        int ty = y + 24;
        g.text(font, "Floor", tx, ty, MUTED, false);
        g.text(font, "Runs", tx + 40, ty, MUTED, false);
        g.text(font, "M runs", tx + 80, ty, MUTED, false);
        g.text(font, "S", tx + 125, ty, MUTED, false);
        g.text(font, "S+", tx + 175, ty, MUTED, false);
        JsonObject completions = Sbc.obj(d, "completions");
        JsonObject master = Sbc.obj(d, "masterCompletions");
        JsonObject fastestS = Sbc.obj(d, "fastestS");
        JsonObject fastestSPlus = Sbc.obj(d, "fastestSPlus");
        for (int floor = 0; floor <= 7; floor++) {
            String key = String.valueOf(floor);
            int ry = ty + 12 + floor * 12;
            g.text(font, floor == 0 ? "E" : "F" + floor, tx, ry, TEXT, false);
            g.text(font, Sbc.number(Sbc.lng(completions, key, 0)), tx + 40, ry, TEXT, false);
            g.text(font, floor == 0 ? "-" : Sbc.number(Sbc.lng(master, key, 0)), tx + 80, ry, TEXT, false);
            g.text(font, time(Sbc.lng(fastestS, key, 0)), tx + 125, ry, TEXT, false);
            g.text(font, time(Sbc.lng(fastestSPlus, key, 0)), tx + 175, ry, TEXT, false);
        }
    }

    private static String time(long ms) {
        if (ms <= 0) return "-";
        return String.format(Locale.ENGLISH, "%d:%02d", ms / 60000, ms / 1000 % 60);
    }

    private String petName(JsonObject pet) {
        String type = Sbc.str(pet, "type").replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder name = new StringBuilder();
        for (String word : type.split(" ")) name.append(capital(word)).append(' ');
        return "[Lvl " + Sbc.lng(pet, "level", 1) + "] " + name.toString().trim();
    }

    private ItemStack petStack(JsonObject pet) {
        String skin = Sbc.str(pet, "skin");
        int tier = List.of(PET_TIERS).indexOf(Sbc.str(pet, "tier"));
        ItemStack stack = RepoItems.itemStack(Sbc.str(pet, "type") + ";" + Math.max(0, tier));
        if ((stack == null || stack.isEmpty()) && skin.length() > 40) stack = com.epic60869.skyballs.custom.util.Compat.createSkull(skin);
        return stack == null || stack.isEmpty() ? new ItemStack(Items.BONE) : stack;
    }

    private void drawPets(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        JsonArray pets = Sbc.arr(profile(), "pets");
        List<JsonObject> list = new ArrayList<>();
        for (JsonElement e : pets) if (e.isJsonObject()) list.add(e.getAsJsonObject());
        list.sort((a, b) -> Boolean.compare(Sbc.bool(b, "active"), Sbc.bool(a, "active")));
        if (list.isEmpty()) g.text(font, "No pets.", x, y, MUTED, false);
        int perRow = Math.max(1, (panelW() - 12) / 136);
        int rows = (H - 30) / 20;
        scroll = Math.max(0, Math.min(scroll, (list.size() + perRow - 1) / perRow - rows));
        for (int i = scroll * perRow; i < list.size() && i < (scroll + rows) * perRow; i++) {
            JsonObject pet = list.get(i);
            int px = x + (i % perRow) * 136;
            int py = y + (i / perRow - scroll) * 20;
            ItemStack stack = stacks.computeIfAbsent(pet, this::petStack);
            if (Sbc.bool(pet, "active")) g.fill(px - 1, py - 1, px + 17, py + 17, 0x6055FF55);
            g.item(stack, px, py);
            int colour = 0xFF000000 | RARITY.getOrDefault(Sbc.str(pet, "tier"), 0xFFFFFF);
            g.text(font, petName(pet), px + 20, py + 4, colour, false);
            if (mouseX >= px && mouseX < px + 134 && mouseY >= py && mouseY < py + 18) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(petName(pet)).withStyle(s -> s.withColor(colour & 0xFFFFFF)));
                tip.add(Component.literal(capital(Sbc.str(pet, "tier").toLowerCase(Locale.ROOT)) + (Sbc.bool(pet, "active") ? " · Active" : "")).withStyle(s -> s.withColor(0xAAAAAA)));
                tip.add(Component.literal(Sbc.number(Sbc.lng(pet, "exp", 0)) + " XP").withStyle(s -> s.withColor(0xAAAAAA)));
                String held = Sbc.str(pet, "heldItem");
                if (!held.isEmpty()) tip.add(Component.literal("Held: " + held.replace('_', ' ')).withStyle(s -> s.withColor(0x55FF55)));
                g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
            }
        }
    }

    /** The items of an inventory list (skipping empty entries). */
    private static List<JsonObject> items(JsonArray array) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : array) if (e.isJsonObject() && e.getAsJsonObject().size() > 0 && !Sbc.str(e.getAsJsonObject(), "id").isEmpty()) out.add(e.getAsJsonObject());
        return out;
    }

    private void drawSlot(GuiGraphicsExtractor g, JsonObject item, int x, int y, int mouseX, int mouseY) {
        g.fill(x, y, x + 18, y + 18, 0xFF1C2230);
        if (item == null) return;
        String rarity = Sbc.str(item, "rarity");
        if (RARITY.containsKey(rarity)) g.fill(x + 1, y + 1, x + 17, y + 17, 0x50000000 | RARITY.get(rarity));
        ItemStack stack = stacks.computeIfAbsent(item, SbcItems::stack);
        g.item(stack, x + 1, y + 1);
        g.itemDecorations(font, stack, x + 1, y + 1);
        if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
            hovered = item;
        }
    }

    /** Items by their slot, a page of {@code perPage} at a time, as a grid 9 wide. */
    private void drawGrid(GuiGraphicsExtractor g, List<JsonObject> items, int perPage, int x, int y, int mouseX, int mouseY) {
        int maxSlot = -1;
        for (JsonObject o : items) maxSlot = Math.max(maxSlot, (int) Sbc.lng(o, "slot", 0));
        int pages = Math.max(1, (maxSlot + perPage) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        JsonObject[] slots = new JsonObject[perPage];
        for (int i = 0; i < items.size(); i++) {
            JsonObject o = items.get(i);
            int slot = o.has("slot") ? (int) Sbc.lng(o, "slot", i) : i;
            if (slot / perPage == page) slots[slot % perPage] = o;
        }
        for (int i = 0; i < perPage; i++) drawSlot(g, slots[i], x + (i % 9) * 18, y + (i / 9) * 18, mouseX, mouseY);
        if (pages > 1) {
            int py = y + ((perPage + 8) / 9) * 18 + 4;
            g.text(font, "◀", x, py, page > 0 ? TEXT : 0xFF555555, false);
            g.centeredText(font, "Page " + (page + 1) + "/" + pages, x + 81, py, MUTED);
            g.text(font, "▶", x + 156, py, page < pages - 1 ? TEXT : 0xFF555555, false);
            pagerY = py;
            pagerX = x;
            pagerPages = pages;
        }
    }

    private int pagerY = -1;
    private int pagerX;
    private int pagerPages;

    private void drawItems(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        pagerY = -1;
        JsonObject p = profile();
        JsonObject api = Sbc.obj(p, "apiEnabled");
        if (api.has("inventory") && !Sbc.bool(api, "inventory")) {
            g.text(font, username + " has their Inventory API turned off.", x, y + 4, 0xFFFF7F7F, false);
            return;
        }
        JsonObject inventories = Sbc.obj(p, "inventories");
        int gx = x + (panelW() - 12 - 162) / 2;
        switch (inv) {
            case INVENTORY -> {
                // The main inventory, then the hotbar below it, like in game.
                List<JsonObject> items = items(Sbc.arr(inventories, "inventory"));
                JsonObject[] slots = new JsonObject[36];
                for (int i = 0; i < items.size(); i++) {
                    int slot = (int) Sbc.lng(items.get(i), "slot", i);
                    if (slot >= 0 && slot < 36) slots[slot] = items.get(i);
                }
                for (int i = 9; i < 36; i++) drawSlot(g, slots[i], gx + (i % 9) * 18, y + (i / 9 - 1) * 18, mouseX, mouseY);
                for (int i = 0; i < 9; i++) drawSlot(g, slots[i], gx + i * 18, y + 58, mouseX, mouseY);
            }
            case ARMOR -> {
                g.text(font, "Armor", gx, y, MUTED, false);
                g.text(font, "Equipment", gx + 60, y, MUTED, false);
                List<JsonObject> armor = items(Sbc.arr(inventories, "armor"));
                List<JsonObject> equipment = items(Sbc.arr(inventories, "equipment"));
                for (int i = 0; i < 4; i++) {
                    drawSlot(g, bySlot(armor, 3 - i, armor.size() > i ? armor.get(armor.size() - 1 - i) : null), gx, y + 12 + i * 20, mouseX, mouseY);
                    drawSlot(g, bySlot(equipment, i, equipment.size() > i ? equipment.get(i) : null), gx + 60, y + 12 + i * 20, mouseX, mouseY);
                }
            }
            case WARDROBE -> drawGrid(g, items(Sbc.arr(inventories, "wardrobe")), 36, gx, y, mouseX, mouseY);
            case ENDER -> drawGrid(g, items(Sbc.arr(inventories, "enderChest")), 45, gx, y, mouseX, mouseY);
            case ACCESSORIES -> {
                JsonObject acc = Sbc.obj(p, "accessories");
                g.text(font, Sbc.lng(acc, "count", 0) + " accessories · " + Sbc.number(Sbc.lng(acc, "magicalPower", 0)) + " Magical Power"
                    + (Sbc.str(acc, "power").isEmpty() ? "" : " · " + Sbc.str(acc, "power")), x, y + H - 64, GOLD, false);
                drawGrid(g, items(Sbc.arr(inventories, "accessoryBag")), 45, gx, y, mouseX, mouseY);
            }
            case VAULT -> drawGrid(g, items(Sbc.arr(inventories, "personalVault")), 27, gx, y, mouseX, mouseY);
            case BACKPACKS -> {
                JsonArray packs = Sbc.arr(inventories, "backpacks");
                if (packs.isEmpty()) {
                    g.text(font, "No backpacks.", x, y, MUTED, false);
                    return;
                }
                backpack = Math.max(0, Math.min(backpack, packs.size() - 1));
                for (int i = 0; i < packs.size(); i++) {
                    int bx = x + (i % 9) * 16;
                    int by = y + (i / 9) * 14;
                    boolean sel = i == backpack;
                    g.fill(bx, by, bx + 15, by + 13, sel ? 0xFF3B5B8B : 0xFF2A3345);
                    JsonObject pack = packs.get(i).getAsJsonObject();
                    g.centeredText(font, String.valueOf(Sbc.lng(pack, "slot", i) + 1), bx + 8, by + 3, TEXT);
                }
                backpackY = y;
                backpackX = x;
                JsonObject pack = packs.get(backpack).getAsJsonObject();
                drawGrid(g, items(Sbc.arr(pack, "items")), 54, gx + 60, y + 30, mouseX, mouseY);
            }
        }
    }

    private int backpackY = -1;
    private int backpackX;

    // ------------------------------------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int right = left() + panelW();
        int top = top();
        if (result != null) {
            if (profileMenu) {
                JsonArray profiles = Sbc.arr(result, "profiles");
                int x = right - 123;
                int y = top + 17;
                int index = (int) Math.floor((my - y) / 12);
                profileMenu = false;
                if (mx >= x && mx < x + 120 && index >= 0 && index < profiles.size()) {
                    JsonObject p = profiles.get(index).getAsJsonObject();
                    if (!Sbc.bool(p, "selected")) SbcProfileViewer.request(this, Sbc.str(result, "username"), Sbc.str(p, "cuteName"));
                }
                return true;
            }
            if (my >= top + 3 && my < top + 15 && mx >= right - 150 && mx < right) {
                profileMenu = true;
                return true;
            }
            if (tab == Tab.ITEMS && pagerY >= 0 && my >= pagerY - 2 && my < pagerY + 10) {
                if (mx >= pagerX && mx < pagerX + 10 && page > 0) page--;
                else if (mx >= pagerX + 150 && mx < pagerX + 166 && page < pagerPages - 1) page++;
                return true;
            }
            if (tab == Tab.ITEMS && inv == Inv.BACKPACKS && backpackY >= 0 && mx >= backpackX && my >= backpackY && my < backpackY + 28) {
                int i = (int) ((mx - backpackX) / 16) + (int) ((my - backpackY) / 14) * 9;
                if ((mx - backpackX) < 144 && i >= 0) {
                    backpack = i;
                    page = 0;
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.PETS) scroll -= (int) Math.signum(scrollY);
        else if (tab == Tab.ITEMS) page = Math.max(0, page - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
