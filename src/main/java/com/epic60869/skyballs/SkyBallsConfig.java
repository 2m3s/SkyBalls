package com.epic60869.skyballs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import org.lwjgl.glfw.GLFW;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SkyBalls's configuration model.
 *
 * The configuration screen is powered directly by MoulConfig, the same
 * configuration engine used by SkyHanni/NotEnoughUpdates.
 */
public final class SkyBallsConfig extends Config {
    public static final class General {
        @Expose
        @Accordion
        @ConfigOption(name = "Item Custom", desc = "Item and armor customization settings.")
        public ItemCustom itemCustom = new ItemCustom();

        @ConfigOption(name = "Notes", desc = "Open your SkyBalls notes.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable notes = () -> openNotes();

        @ConfigOption(name = "Command Keys", desc = "Configure SkyBalls command shortcuts.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable commandKeys = () -> openCommandKeys();

        @ConfigOption(name = "Gui Editor", desc = "Open the transparent HUD/GUI editor.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable guiEditor = () -> openHudEditor();

        @Expose
        public boolean firstBootAcknowledged = false;
    }

    public static final class Chat {
        @Expose
        @Accordion
        @ConfigOption(name = "Custom Chat", desc = "Control how SkyBalls command output from other players appears in chat.")
        public CustomChat customChat = new CustomChat();

        @Expose
        @Accordion
        @ConfigOption(name = "Copy Chat", desc = "Right-click chat messages to copy them, like NoFrills' Chat Tweaks.")
        public CopyChat copyChat = new CopyChat();

        @Expose
        @ConfigOption(name = "Compact Chat", desc = "Compact repeated chat messages into one message with an occurrence counter.")
        @ConfigEditorBoolean
        public boolean compactChat = true;

        @Expose
        @ConfigOption(name = "Chat Emoji", desc = "Replace :emoji: shortcodes with SkyBalls emoji sprites and provide emoji autocomplete while typing chat.")
        @ConfigEditorBoolean
        public boolean chatEmoji = true;

        @Expose
        @ConfigOption(name = "Hypixel Item Emojis", desc = "In any chat (all, party, guild, private, SkyBalls), :item_id: shows that SkyBlock item's icon, like the SkyHelper Discord: :summoning_eye:, :hyperion:, :enchanted_diamond:. Client side: everyone with SkyBalls sees the icon, others see the text. Hover the icon for the item's name.")
        @ConfigEditorBoolean
        public boolean itemEmojis = true;

        @Expose
        @ConfigOption(name = "Emoji Autocomplete", desc = "Suggest emojis (with a picture of each) while you type :name in chat. Turn off to hide the emoji suggestions.")
        @ConfigEditorBoolean
        public boolean emojiAutocomplete = true;

        @Expose
        @ConfigOption(name = "Current Chat Display", desc = "Show which chat you are typing in (All, Party, Guild, Officer, Co-op, a private conversation or SkyBalls chat) just above the chat box while it is open.")
        @ConfigEditorBoolean
        public boolean currentChatDisplay = true;
    }

    public static final class Farming {
        @Expose
        @Accordion
        @ConfigOption(name = "Farming RNG HUD", desc = "Click to expand the farming RNG HUD options.")
        public FarmingRng rng = new FarmingRng();

        @Expose
        @Accordion
        @ConfigOption(name = "Mouse Lock", desc = "Fully lock the camera while holding a farming tool.")
        public MouseLock mouseLock = new MouseLock();

        @Expose
        @Accordion
        @ConfigOption(name = "Garden", desc = "Yaw/pitch, pest cooldown, blocks per second and special drop animations.")
        public com.epic60869.skyballs.features.FeatureConfigs.Garden garden = new com.epic60869.skyballs.features.FeatureConfigs.Garden();

        @Expose
        @Accordion
        @ConfigOption(name = "Pest Highlight", desc = "Outline pests in the Garden, with an optional line or beacon to the nearest one and a pests/plots HUD.")
        public com.epic60869.skyballs.features.sbc.SbcConfig.PestHighlight pestHighlight = new com.epic60869.skyballs.features.sbc.SbcConfig.PestHighlight();

    }

    public static final class Mining {
        @Expose
        @Accordion
        @ConfigOption(name = "Mining Commissions", desc = "Show and configure the Mining Commission HUD.")
        public MiningCommissions commissions = new MiningCommissions();

        @Expose
        @Accordion
        @ConfigOption(name = "Mining Features", desc = "Crystal Hollows map, Divan tools alert and mineshaft timer.")
        public com.epic60869.skyballs.features.FeatureConfigs.MiningFeatures features = new com.epic60869.skyballs.features.FeatureConfigs.MiningFeatures();
    }

    public static final class Slayers {
        @Expose
        @Accordion
        @ConfigOption(name = "Slayer HUDs", desc = "Slayer boss phase HUD.")
        public com.epic60869.skyballs.features.FeatureConfigs.Slayer huds = new com.epic60869.skyballs.features.FeatureConfigs.Slayer();

        @Expose
        @ConfigOption(name = "Kills Since Rare Drop", desc = "Show the kills-since-drop counter.")
        @ConfigEditorBoolean
        public boolean killsSinceDrop = true;
    }

    public static final class Pets {
        @Expose
        @Accordion
        @ConfigOption(name = "Pets Display", desc = "Pet display, overflow XP and positioning.")
        public PetDisplay display = new PetDisplay();
    }

    public static final class Misc {
        @Expose
        @Accordion
        @ConfigOption(name = "Party Commands", desc = "Let party members use !warp, !allinvite and !pt when you are leader.")
        public com.epic60869.skyballs.features.FeatureConfigs.PartyCommands partyCommands = new com.epic60869.skyballs.features.FeatureConfigs.PartyCommands();

        @Expose
        @Accordion
        @ConfigOption(name = "Slot Locking & Binding", desc = "Lock inventory slots (L) and bind hotbar slots to inventory slots (B).")
        public SlotLocking slotLocking = new SlotLocking();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Notification", desc = "Show items from your list on a HUD when they go into your sacks or inventory (SkyOcean's Sack Notification as a HUD).")
        public com.epic60869.skyballs.features.FeatureConfigs.ItemNotification itemNotification = new com.epic60869.skyballs.features.FeatureConfigs.ItemNotification();

        @Expose
        @Accordion
        @ConfigOption(name = "Auto Welcome", desc = "Welcome players on your list in guild chat or with /msg when they come online.")
        public com.epic60869.skyballs.features.FeatureConfigs.AutoWelcome autoWelcome = new com.epic60869.skyballs.features.FeatureConfigs.AutoWelcome();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Rarity", desc = "Rarity-coloured backgrounds behind SkyBlock items.")
        public ItemRarity itemRarity = new ItemRarity();

        @Expose
        @Accordion
        @ConfigOption(name = "Experimental Table", desc = "Experimentation table solvers: Chronomatron, Superpairs and Ultrasequencer.")
        public com.epic60869.skyballs.features.FeatureConfigs.Enchanting experimentalTable = new com.epic60869.skyballs.features.FeatureConfigs.Enchanting();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Price Tooltip", desc = "Add prices to SkyBlock item tooltips, like Skyblocker.")
        public PriceTooltip priceTooltip = new PriceTooltip();

        @Expose
        @Accordion
        @ConfigOption(name = "Museum & Accessory Tooltips", desc = "Show in item tooltips whether you've donated the item to your museum and whether you're missing an accessory, like Skyblocker.")
        public CollectionTooltips collectionTooltips = new CollectionTooltips();

        @Expose
        @Accordion
        @ConfigOption(name = "Random", desc = "Low fire overlay, hidden explosions and other small visual tweaks.")
        public Random random = new Random();

        @Expose
        @Accordion
        @ConfigOption(name = "Player Size", desc = "Make yourself, other players, or both bigger or smaller (client side only), like Odin.")
        public PlayerSize playerSize = new PlayerSize();

        @Expose
        @Accordion
        @ConfigOption(name = "Held Item", desc = "Move, rotate and scale the item in your hand, change its swing speed and style, and show vanilla textures, globally or per item, ported from Skysoft. Open the editor with /sb helditem.")
        public com.epic60869.skyballs.features.helditem.HeldItemConfig heldItem = new com.epic60869.skyballs.features.helditem.HeldItemConfig();

        /** The old Held Item Model settings, only read to copy them into Held Item once. */
        @Expose
        public LegacyHeldItemModel heldItemModel = null;


        @Expose
        @Accordion
        @ConfigOption(name = "Mouse Reset", desc = "Reset the mouse cursor when selected SkyBlock menus open.")
        public MouseReset mouseReset = new MouseReset();

        @Expose
        @Accordion
        @ConfigOption(name = "Tooltip Scroll", desc = "Move tooltips with the mouse wheel and keys so long tooltips can be read (Skysoft's Tooltip Scroll). Off automatically when Skysoft is installed.")
        public TooltipScroll tooltipScroll = new TooltipScroll();

        @Expose
        @ConfigOption(name = "Join Commands", desc = "Quick commands to join dungeons and Kuudra: /f0 (Entrance) to /f7, /m1 to /m7, and /t1 to /t5 for Kuudra (Basic to Infernal). Applies next time you join a server.")
        @ConfigEditorBoolean
        public boolean joinCommands = true;

        @Expose
        @ConfigOption(name = "Screenshot Sharing", desc = "After F2, the screenshot message gets an [Upload] button that gives you a link to post in /sbc, like Skysoft. Uploads are public to anyone with the link.")
        @ConfigEditorBoolean
        public boolean screenshotSharing = true;

        @Expose
        @ConfigOption(name = "Screenshot Upload Host", desc = "Where screenshots are uploaded. Litterbox deletes them after the chosen time; Catbox keeps them.")
        @ConfigEditorDropdown
        public com.epic60869.skyballs.features.misc.ScreenshotShare.Host screenshotHost = com.epic60869.skyballs.features.misc.ScreenshotShare.Host.LITTERBOX_72H;

        @Expose
        @ConfigOption(name = "Hypixel Button", desc = "A Hypixel button on the title screen, next to Multiplayer, that joins play.hypixel.net in one click.")
        @ConfigEditorBoolean
        public boolean hypixelButton = true;

        @Expose
        @ConfigOption(name = "Storage Overlay", desc = "Firmament's storage overlay (ported from Firmament): /storage, your Ender Chest pages and backpacks open as one scrollable view of every page, with your inventory and a search box. Click a page to open it; the open page works like the normal menu. \"Edit Pages\" shows the normal Storage menu. /sb storage opens it even with this off.")
        @ConfigEditorBoolean
        public boolean storageOverlay = true;

        @Expose
        @Accordion
        @ConfigOption(name = "Storage Overlay Settings", desc = "Layout and search options for the storage overlay.")
        public StorageOverlaySettings storageOverlaySettings = new StorageOverlaySettings();

        @Expose
        @ConfigOption(name = "Recipe HUD", desc = "While a /sb recipe is selected, show a movable HUD with the item and the base ingredients you still need (like SkyOcean's craft helper overlay). Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean recipeHud = true;

        @Expose
        @ConfigOption(name = "Recipe HUD Hide Completed", desc = "Hide ingredients you already have enough of in the Recipe HUD.")
        @ConfigEditorBoolean
        public boolean recipeHudHideCompleted = false;

        @Expose
        @ConfigOption(name = "Calendar Time to Real Time", desc = "When enabled, hovering a SkyBlock calendar date adds the equivalent real-world date and time in your computer's local time zone.")
        @ConfigEditorBoolean
        public boolean calendarTimeToRealTime = true;

        @Expose
        @ConfigOption(name = "Price Paid", desc = "Remember what you paid for items you buy on the auction house and show it in their tooltip, like NoFrills.")
        @ConfigEditorBoolean
        public boolean pricePaid = true;

        @Expose
        @ConfigOption(name = "Update Notifications", desc = "Tell you in chat when a newer SkyBalls version is out (\"New SkyBalls Mod Version 1.2.3 --> 1.2.5\"), with a download link.")
        @ConfigEditorBoolean
        public boolean updateNotifications = true;

        @Expose
        @ConfigOption(name = "Collection Tracker", desc = "While you mine, farm, forage or fish, show the collection you're gathering, what you've gained this session and per hour, like SkyHanni's farming display. Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean collectionTracker = true;

        @Expose
        @ConfigOption(name = "Collection Tracker Elite Rank", desc = "Also show your rank on the Elite (elitebot.dev) collection leaderboard and how much you need to pass the next player.")
        @ConfigEditorBoolean
        public boolean collectionTrackerRank = true;

        /** Collection pinned with /sj trackcollection (a Hypixel item id), or "" to follow what you gather. */
        @Expose
        public String collectionTrackerItem = "";

        /** Goal set with /sj trackcollection &lt;item&gt; &lt;goal&gt;, or 0. */
        @Expose
        public long collectionTrackerGoal = 0;

        @Expose
        @ConfigOption(name = "Warp Shortcuts", desc = "Type /dhub instead of /warp dhub (and the same for every name in the list below). Applies next time you join a server.")
        @ConfigEditorBoolean
        public boolean warpShortcuts = true;

        @Expose
        @ConfigOption(name = "Warp Shortcut List", desc = "Warps that get their own command, separated by commas.")
        @ConfigEditorText
        public String warpShortcutList = "dhub, dungeon_hub, garden, barn, desert, trapper, park, howl, jungle, gold, deep, mines, forge, crystals, nucleus, base, camp, tunnels, end, drag, void, spider, nest, arachne, crimson, isle, kuudra, smold, museum, da, castle, wiz, jerry, rift, galatea, murkwater";

        @Expose
        @ConfigOption(name = "Toggle Sprint", desc = "Always sprint, like Odin's Auto Sprint. Set a \"Toggle Sprint\" key in Controls to switch it on and off.")
        @ConfigEditorBoolean
        public boolean toggleSprint = false;

        @Expose
        @ConfigOption(name = "Toggle Sprint HUD", desc = "Show [Sprinting (Toggled)] while toggle sprint is on. Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean toggleSprintHud = true;

        @Expose
        @Accordion
        @ConfigOption(name = "Item Cooldowns", desc = "Show item ability cooldowns on the item's slot (and optionally a HUD).")
        public com.epic60869.skyballs.features.sbc.SbcConfig.ItemCooldowns itemCooldowns = new com.epic60869.skyballs.features.sbc.SbcConfig.ItemCooldowns();

        @Expose
        @Accordion
        @ConfigOption(name = "Event Calendar", desc = "Upcoming SkyBlock events with countdowns (/sb calendar), a HUD and reminders before the events you pick.")
        public com.epic60869.skyballs.features.sbc.SbcConfig.EventCalendar eventCalendar = new com.epic60869.skyballs.features.sbc.SbcConfig.EventCalendar();

        @Expose
        @Accordion
        @ConfigOption(name = "Accessory Helper", desc = "A list next to the Accessory Bag of the accessories and upgrades you're missing, cheapest magical power first.")
        public com.epic60869.skyballs.features.sbc.SbcConfig.AccessoryHelper accessoryHelper = new com.epic60869.skyballs.features.sbc.SbcConfig.AccessoryHelper();
    }

    private static final Gson LEGACY_GSON = new Gson();

    private static ManagedConfig<SkyBallsConfig> managed;

    @Expose
    @Category(name = "General", desc = "Core SkyBalls settings and utilities.")
    public General general = new General();

    @Expose
    @Category(name = "Chat", desc = "Chat quality-of-life features.")
    public Chat chat = new Chat();

    @Expose
    @Category(name = "SkyBalls Online", desc = "SkyBalls chat replies, reactions and item sharing, friends, cosmetics, the casino and cloud settings.")
    public com.epic60869.skyballs.features.sbc.SbcConfig.Online online = new com.epic60869.skyballs.features.sbc.SbcConfig.Online();

    @Expose
    @Category(name = "Combat", desc = "Arrow counter, legion display, cocoon alerts and rare drops.")
    public com.epic60869.skyballs.features.FeatureConfigs.Combat combat = new com.epic60869.skyballs.features.FeatureConfigs.Combat();

    @Expose
    @Category(name = "Slayers", desc = "Slayer boss phases and drop tracking.")
    public Slayers slayers = new Slayers();

    @Expose
    @Category(name = "Farming", desc = "Garden HUDs, mouse lock and farming RNG tools.")
    public Farming farming = new Farming();

    @Expose
    @Category(name = "Fishing", desc = "Fishing stats, hook timer, bait and rare creature alerts.")
    public com.epic60869.skyballs.features.FeatureConfigs.Fishing fishing = new com.epic60869.skyballs.features.FeatureConfigs.Fishing();

    @Expose
    @Category(name = "Mining", desc = "Commissions, Crystal Hollows map, Divan tools and mineshaft timer.")
    public Mining mining = new Mining();

    @Expose
    @Category(name = "Foraging", desc = "Sweep display.")
    public com.epic60869.skyballs.features.FeatureConfigs.Foraging foraging = new com.epic60869.skyballs.features.FeatureConfigs.Foraging();

    @Expose
    @Category(name = "Runecrafting", desc = "Valuable rune alerts.")
    public com.epic60869.skyballs.features.FeatureConfigs.Runecrafting runecrafting = new com.epic60869.skyballs.features.FeatureConfigs.Runecrafting();

    @Expose
    @Category(name = "Dungeons", desc = "Map, puzzle solvers, secrets, terminals, splits and timers.")
    public com.epic60869.skyballs.features.FeatureConfigs.Dungeons dungeons = new com.epic60869.skyballs.features.FeatureConfigs.Dungeons();

    @Expose
    @Category(name = "Pets", desc = "Pet displays and overflow XP tools.")
    public Pets pets = new Pets();

    @Expose
    @Category(name = "Misc", desc = "Nickname and small quality-of-life options.")
    public Misc misc = new Misc();

    public static final class TooltipScroll {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Allow tooltips to be moved with the mouse wheel and movement keys.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Enable Scroll Wheel", desc = "Move tooltips with the mouse wheel.")
        @ConfigEditorBoolean
        public boolean enableScrollWheel = true;

        @Expose
        @ConfigOption(name = "Enable in Chat", desc = "Allow tooltip movement while chat is open.")
        @ConfigEditorBoolean
        public boolean enabledInChat = false;

        @Expose
        @ConfigOption(name = "Enable WASD", desc = "Use WASD to move the hovered tooltip.")
        @ConfigEditorBoolean
        public boolean enableWASD = false;

        @Expose
        @ConfigOption(name = "Mouse Scrolling Speed", desc = "Pixels moved per mouse-wheel step.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
        public int mouseScrollingSpeed = 10;

        @Expose
        @ConfigOption(name = "Keyboard Scrolling Speed", desc = "Pixels moved per tick while a tooltip movement key is held.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
        public int keyboardScrollingSpeed = 5;

        @Expose
        @ConfigOption(name = "Move Up Key", desc = "Move the hovered tooltip up.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_PAGE_UP)
        public int moveUpKey = GLFW.GLFW_KEY_PAGE_UP;

        @Expose
        @ConfigOption(name = "Move Down Key", desc = "Move the hovered tooltip down.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_PAGE_DOWN)
        public int moveDownKey = GLFW.GLFW_KEY_PAGE_DOWN;

        @Expose
        @ConfigOption(name = "Horizontal Movement Key", desc = "Hold this key to make up and down movement horizontal.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        public int horizontalMovementKey = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Reset Tooltip Key", desc = "Reset the hovered tooltip's moved position.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        public int resetTooltipKey = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Start On Top", desc = "Show the top of oversized tooltips when they first appear.")
        @ConfigEditorBoolean
        public boolean startOnTop = false;

        @Expose
        @ConfigOption(name = "Reset Position When Not Hovered", desc = "Reset tooltip movement after the tooltip disappears.")
        @ConfigEditorBoolean
        public boolean resetWhenNotHovered = true;

        @Expose
        @ConfigOption(name = "Use Left Shift", desc = "Hold left shift to move tooltips horizontally with the mouse wheel.")
        @ConfigEditorBoolean
        public boolean useLeftShift = true;

        @Expose
        @ConfigOption(name = "Invert Horizontal Movement", desc = "Invert horizontal tooltip movement.")
        @ConfigEditorBoolean
        public boolean invertHorizontal = false;

        @Expose
        @ConfigOption(name = "Invert Vertical Movement", desc = "Invert vertical tooltip movement.")
        @ConfigEditorBoolean
        public boolean invertVertical = false;

        @Expose
        @ConfigOption(name = "Scroll Smoothness", desc = "How quickly tooltips slide toward the moved position. 100 is instant.")
        @ConfigEditorSlider(minValue = 5f, maxValue = 100f, minStep = 5f)
        public int scrollSmoothness = 25;
    }

    public static final class ItemCustom {
        @ConfigOption(name = "Open Item Editor", desc = "Open the item and armor customization screen.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable openItemEditor = () -> openCustom();

        @Expose
        @ConfigOption(name = "Show Customize Button", desc = "Show a button in the inventory that opens the item and armor customization screen.")
        @ConfigEditorBoolean
        public boolean showCustomizeButton = true;
    }

    public static final class SlotLocking {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Slot locking and slot binding. Locked slots can't be clicked, moved or dropped; bound slots swap with a shift-click (Odin's Slot Binds).")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Lock Key", desc = "Press over a slot in your inventory to lock or unlock it.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_L)
        public int lockKey = GLFW.GLFW_KEY_L;

        @Expose
        @ConfigOption(name = "Bind Key", desc = "In your inventory: press over a slot, then over another (one in the hotbar), to bind them. Press on a bound slot to unbind.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_B)
        public int bindKey = GLFW.GLFW_KEY_B;

        @Expose
        @ConfigOption(name = "Bind Line Only With Shift", desc = "Only show the line between bound slots while holding Shift.")
        @ConfigEditorBoolean
        public boolean lineOnlyWithShift = false;

        /** Locked player inventory slots (0-8 hotbar, 9-35 inventory). */
        @Expose
        public java.util.List<Integer> locked = new java.util.ArrayList<>();

        /** Slot binds, by inventory screen slot (36-44 is the hotbar). */
        @Expose
        public java.util.Map<Integer, Integer> binds = new java.util.HashMap<>();
    }

    public static final class CopyChat {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Copy chat messages, like NoFrills' Chat Tweaks: with chat open, press the Copy Message Key over a message to copy it. SkyBalls rank prefixes aren't copied.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        /** A keyboard key, or a mouse button stored the MoulConfig way (-100 + button; right click is -99). */
        @Expose
        @ConfigOption(name = "Copy Message Key", desc = "Key or mouse button that copies the message under the mouse while chat is open. Right click by default.")
        @ConfigEditorKeybind(defaultKey = -100 + GLFW.GLFW_MOUSE_BUTTON_RIGHT)
        public int copyMessageKey = -100 + GLFW.GLFW_MOUSE_BUTTON_RIGHT;

        @Expose
        @ConfigOption(name = "Copy Preview", desc = "Show what was copied in chat.")
        @ConfigEditorBoolean
        public boolean preview = true;

        @Expose
        @ConfigOption(name = "Preview Length", desc = "How many characters of the copied text to show (0 just says it was copied).")
        @ConfigEditorSlider(minValue = 0, maxValue = 200, minStep = 10)
        public int previewLength = 50;

        @Expose
        @ConfigOption(name = "Trim On Copy", desc = "Remove spaces at the start and end of what's copied.")
        @ConfigEditorBoolean
        public boolean trim = false;
    }

    public static final class CustomChat {
        @Expose
        @ConfigOption(name = "Show SB Chat", desc = "Show SkyBalls chat (/sbc) messages from other players. Off hides them; you can still send with /sbc.")
        @ConfigEditorBoolean
        public boolean showSjChat = true;

        @Expose
        @ConfigOption(name = "Show Ranks", desc = "Show SkyBalls ranks ([OWNER], [TESTER], ...) in front of names in /sbc. Turn off to hide them.")
        @ConfigEditorBoolean
        public boolean showRanks = true;

        @Expose
        @ConfigOption(name = "SB Chat Ping", desc = "Play a little ping when someone sends a message in /sbc.")
        @ConfigEditorBoolean
        public boolean pingSound = false;

        @Expose
        @ConfigOption(name = "Hide Other Players' Commands", desc = "Hide SkyBalls command result messages when they belong to another player. Your own command results remain visible.")
        @ConfigEditorBoolean
        public boolean hideOtherCommands = true;
    }

    public static final class MiningCommissions {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the commission HUD when commission data is present in the tab list.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a dark background behind the commission HUD.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose
        @ConfigOption(name = "Scale", desc = "Scale the commission HUD.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
        public float scale = 1.0f;

        @Expose
        public int x = 8;

        @Expose
        public int y = 80;

        @ConfigOption(name = "Edit Position", desc = "Open the HUD editor and drag the Mining Commissions HUD.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openHudEditor();
    }

    public static final class FarmingRng {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the farming RNG/progress overlay.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a background behind the farming RNG HUD.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose
        @ConfigOption(name = "Scale", desc = "Scale the farming RNG HUD.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
        public float scale = 1.0f;

        @Expose
        @ConfigOption(name = "Drop Colour", desc = "Colour of RNG drop names on the RNG HUD.")
        @ConfigEditorColour
        public String dropColour = "0:255:85:255:255";

        @Expose
        @ConfigOption(name = "Pet Rarity Colours", desc = "Show slug pets in their rarity colour (Epic purple, Legendary gold) instead of the Drop Colour.")
        @ConfigEditorBoolean
        public boolean petRarityColours = true;

        @Expose
        @ConfigOption(name = "Price Colour", desc = "Colour of the prices on the RNG HUD (RNG drops and Item Notification).")
        @ConfigEditorColour
        public String priceColour = "0:255:184:184:184";

        @Expose
        public int x = 8;

        @Expose
        public int y = 8;

        @ConfigOption(name = "Edit Position", desc = "Open the SkyBalls HUD editor and drag the Farming RNG HUD.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openRngEditor();
    }

    public static final class PetDisplay {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the active pet HUD.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Overflow Pet Levels", desc = "Show pet XP beyond the normal maximum level.")
        @ConfigEditorBoolean
        public boolean overflowLevels = true;

        @Expose
        @ConfigOption(name = "Auto-Pet Display", desc = "Keep the pet display synced with the active pet.")
        @ConfigEditorBoolean
        public boolean autoDisplay = true;

        @Expose
        @ConfigOption(name = "Scale", desc = "Scale the pet HUD.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
        public float scale = 1.0f;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a dark background behind the pet display.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose public int x = 10;
        @Expose public int y = 10;

        @ConfigOption(name = "Edit Position", desc = "Open the HUD editor and drag the Pet Display.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openHudEditor();
    }

    public static final class MouseLock {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Fully lock the camera while holding a farming tool.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Garden Only", desc = "Only lock the camera on the Garden.")
        @ConfigEditorBoolean
        public boolean gardenOnly = true;

        @Expose
        @ConfigOption(name = "Ground Only", desc = "Only apply Mouse Lock while the player is on the ground.")
        @ConfigEditorBoolean
        public boolean groundOnly = true;
    }

    public static final class PriceTooltip {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show item prices in tooltips.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Bazaar Prices", desc = "For items sold on the bazaar instead of the auction house, show the bazaar insta-buy and insta-sell price where the lowest BIN and 3 day average would be (for the whole stack, or the whole sack in the Sacks menu).")
        @ConfigEditorBoolean
        public boolean bazaar = true;

        @Expose
        @ConfigOption(name = "NPC Sell Price", desc = "How much an NPC buys the item for.")
        @ConfigEditorBoolean
        public boolean npcPrice = true;

        @Expose
        @ConfigOption(name = "Lowest BIN Price", desc = "The item's current lowest Buy It Now price on the auction house.")
        @ConfigEditorBoolean
        public boolean lowestBin = true;

        @Expose
        @ConfigOption(name = "3 Day Avg. Price", desc = "The item's average lowest BIN price over the last 3 days.")
        @ConfigEditorBoolean
        public boolean threeDayAverage = true;
    }

    public static final class CollectionTooltips {
        @Expose
        @ConfigOption(name = "Museum", desc = "Show whether the item is donated to your museum. Open your Museum's category menus once to fill this in; it's saved per profile.")
        @ConfigEditorBoolean
        public boolean museum = true;

        @Expose
        @ConfigOption(name = "Accessories", desc = "Show whether you're missing an accessory, already have it, or whether it's an upgrade or downgrade of the one you have from the same family. Open each page of your Accessory Bag once to fill this in; it's saved per profile.")
        @ConfigEditorBoolean
        public boolean accessories = true;
    }

    public static final class ItemRarity {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show a background behind SkyBlock items in your inventory, containers and hotbar using the item's rarity color.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Style", desc = "The shape of the item rarity background.")
        @ConfigEditorDropdown
        public SkyBallsItemBackgrounds.Style style = SkyBallsItemBackgrounds.Style.SQUARE;

        @Expose
        @ConfigOption(name = "Opacity", desc = "How opaque the item rarity background is.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.05f)
        public float opacity = 0.5f;
    }

    public static final class PlayerSize {
        @Expose
        @ConfigOption(name = "Scale Yourself", desc = "Change your own player's size (third person, inventory preview).")
        @ConfigEditorBoolean
        public boolean self = false;

        @Expose @ConfigOption(name = "Your Width", desc = "X scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float selfX = 0.6f;
        @Expose @ConfigOption(name = "Your Height", desc = "Y scale (1 = normal, negative = upside down).") @ConfigEditorSlider(minValue = -1f, maxValue = 3f, minStep = 0.05f) public float selfY = 0.6f;
        @Expose @ConfigOption(name = "Your Depth", desc = "Z scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float selfZ = 0.6f;

        @Expose
        @ConfigOption(name = "Scale Others", desc = "Change the size of every other real player (NPCs are left alone).")
        @ConfigEditorBoolean
        public boolean others = false;

        @Expose @ConfigOption(name = "Others Width", desc = "X scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float othersX = 0.6f;
        @Expose @ConfigOption(name = "Others Height", desc = "Y scale (1 = normal, negative = upside down).") @ConfigEditorSlider(minValue = -1f, maxValue = 3f, minStep = 0.05f) public float othersY = 0.6f;
        @Expose @ConfigOption(name = "Others Depth", desc = "Z scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float othersZ = 0.6f;
    }

    /** The settings of the old Held Item Model (replaced by Skysoft's Held Item). */
    public static final class LegacyHeldItemModel {
        @Expose public boolean enabled = false;
        @Expose public float x = 0f;
        @Expose public float y = 0f;
        @Expose public float z = 0f;
        @Expose public float scale = 1f;
        @Expose public float rotationX = 0f;
        @Expose public float rotationY = 0f;
        @Expose public float rotationZ = 0f;
        @Expose public float swingSpeed = 1f;
        @Expose public boolean ignoreMiningEffects = false;
    }

    public static final class Random {
        @Expose
        @ConfigOption(name = "Low Fire", desc = "Lower the burning overlay on your screen so it covers less of the view.")
        @ConfigEditorBoolean
        public boolean lowFire = false;

        @Expose
        @ConfigOption(name = "Fire Height", desc = "How far to lower the fire overlay (0 = vanilla, 1 = off the screen).")
        @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.05f)
        public float fireOffset = 0.3f;

        @Expose
        @ConfigOption(name = "Hide Explosions", desc = "Hide explosion particles (TNT, Bonzo staff, Wither impact and other server explosions).")
        @ConfigEditorBoolean
        public boolean hideExplosions = false;
    }

    /** Firmament's storage overlay options (Firmament's StorageOverlay.TConfig). */
    public static final class StorageOverlaySettings {
        @Expose @ConfigOption(name = "Dark Mode", desc = "Draw the storage overlay's backgrounds, slots and scroll bar dark instead of Minecraft's light grey.") @ConfigEditorBoolean public boolean darkMode = false;
        @Expose @ConfigOption(name = "Dark Mode Shade", desc = "How dark Dark Mode is: the colour the overlay's textures are tinted with (darker is darker).") @ConfigEditorColour public String darkModeShade = "0:255:70:70:78";
        @Expose @ConfigOption(name = "Outline Active Page", desc = "Put a border around the selected storage page in the storage overlay.") @ConfigEditorBoolean public boolean outlineActivePage = false;
        @Expose @ConfigOption(name = "Outline Colour", desc = "Change the colour of the border around your selected storage page.") @ConfigEditorColour public String outlineActivePageColour = "0:255:255:255:0";
        @Expose @ConfigOption(name = "Inactive Page Tooltips", desc = "Show item tooltips when hovering over items on pages other than the active one.") @ConfigEditorBoolean public boolean inactivePageTooltips = false;
        @Expose @ConfigOption(name = "Columns", desc = "Max columns used by the storage overlay and overview.") @ConfigEditorSlider(minValue = 1, maxValue = 10, minStep = 1) public int columns = 3;
        @Expose @ConfigOption(name = "Storage Height", desc = "The height of the scrollable storage panel.") @ConfigEditorSlider(minValue = 80, maxValue = 3000, minStep = 10) public int height = 3 * 18 * 6;
        @Expose @ConfigOption(name = "Retain Scroll Position", desc = "Retain scroll position when closing storage overlay and overview.") @ConfigEditorBoolean public boolean retainScroll = true;
        @Expose @ConfigOption(name = "Scroll Speed", desc = "Scroll speed inside of the storage overlay and overview.") @ConfigEditorSlider(minValue = 1, maxValue = 50, minStep = 1) public int scrollSpeed = 10;
        @Expose @ConfigOption(name = "Invert Scroll", desc = "Invert the mouse wheel scrolling in the storage overlay.") @ConfigEditorBoolean public boolean inverseScroll = false;
        @Expose @ConfigOption(name = "Padding", desc = "Padding inside of the storage overview.") @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1) public int padding = 5;
        @Expose @ConfigOption(name = "Margin", desc = "Margin inside of the storage overview.") @ConfigEditorSlider(minValue = 1, maxValue = 60, minStep = 1) public int margin = 20;
        @Expose @ConfigOption(name = "Block Scrolling on Items", desc = "Disables scrolling the storage overlay screen while you are hovering over an item. Useful if you have a tooltip scrolling mod.") @ConfigEditorBoolean public boolean itemsBlockScroll = false;
        @Expose @ConfigOption(name = "Highlight Search Results", desc = "Highlight the search results in the storage overlay.") @ConfigEditorBoolean public boolean highlightSearchResults = true;
        @Expose @ConfigOption(name = "Highlight Search Colour", desc = "Change the colour of the highlighted search result.") @ConfigEditorColour public String highlightSearchResultsColour = "0:255:0:176:0";
    }

    public static final class MouseReset {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable automatic mouse reset for selected menus.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Accessory Bag", desc = "Reset the cursor when the Accessory Bag opens.")
        @ConfigEditorBoolean
        public boolean accessoryBag = true;

        @Expose
        @ConfigOption(name = "Ender Chest", desc = "Reset the cursor when an Ender Chest opens.")
        @ConfigEditorBoolean
        public boolean enderChest = true;

        @Expose
        @ConfigOption(name = "Backpack", desc = "Reset the cursor when a Backpack opens.")
        @ConfigEditorBoolean
        public boolean backpack = true;
    }

    public static final class Discord {
        @ConfigOption(name = "Open Discord", desc = "Open SkyBalls's personal Discord linking screen.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable open = () -> openDiscord();
    }

    private static void openCustom() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> SkyBallsCustom.open(mc, mc.gui.screen()));
    }

    private static void openNotes() {
        Minecraft mc = Minecraft.getInstance();
        Path dir = mc.gameDirectory.toPath().resolve("config");
        mc.execute(() -> mc.gui.setScreen(new SkyBallsNotesScreen(dir)));
    }

    private static void openCommandKeys() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(com.epic60869.skyballs.commandkeys.CommandKeys.getConfigScreen(mc.gui.screen())));
    }

    private static void openRngEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsRngHudScreen(mc.gui.screen())));
    }

    private static void openHudEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsHudEditorScreen(mc.gui.screen())));
    }


    private static void openDiscord() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsDiscordScreen(mc.gui.screen())));
    }

    @Override
    public StructuredText getTitle() {
        // "SkyBalls Mod v1.2.3": the installed version, from fabric.mod.json.
        String version = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("skyballs")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
        return StructuredText.of("§dSkyBalls Mod" + (version.isEmpty() ? "" : " §7v" + version));
    }

    private static final Gson SNAPSHOT_GSON = new com.google.gson.GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static String lastSaved;
    private static int saveCheckTicks;

    private static String snapshot() {
        try {
            return managed == null ? null : SNAPSHOT_GSON.toJson(managed.getInstance());
        } catch (Exception e) {
            return null;
        }
    }

    public static SkyBallsConfig load(Path path) {
        try {
            migrateLegacy(path);
            migrateConfigShape(path);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Legacy config migration failed: " + e.getMessage());
        }

        FileHolder holder = new FileHolder(path);
        boolean configExisted = Files.exists(path);

        managed = new ManagedConfig<>(new io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder<>(
            holder.file, SkyBallsConfig.class
        ));

        // Always materialize the current config after loading. This is important
        // for newly-added settings such as Mouse Reset: older SkyBalls config files
        // may not contain the new nested section yet, and leaving defaults only
        // in memory makes them appear to reset after a restart.
        try {
            Files.createDirectories(path.getParent());
            managed.saveToFile();
        } catch (Exception e) {
            System.err.println("[SkyBalls] Failed to persist config after load: " + e.getMessage());
        }

        // MoulConfig calls saveNow() when its GUI closes, which only runs these runnables.
        // Without this, changes made in /sj were lost unless something else saved later.
        managed.getInstance().saveRunnables.add(managed::saveToFile);

        // Also save whenever any setting changes. Relying on the GUI-close hook alone lost changes
        // (for example the item rarity style) when the screen was closed in ways that skip it.
        lastSaved = snapshot();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++saveCheckTicks % 20 != 0 || managed == null) return;
            String now = snapshot();
            if (now != null && !now.equals(lastSaved)) {
                lastSaved = now;
                try {
                    managed.saveToFile();
                } catch (Exception e) {
                    System.err.println("[SkyBalls] Failed to save config: " + e.getMessage());
                }
            }
        });

        return managed.getInstance();
    }

    public static SkyBallsConfig current() {
        return managed == null ? null : managed.getInstance();
    }

    /** The saved config file as JSON, for settings cloud sync (saves first so it's up to date). */
    public static JsonObject exportJson() {
        if (managed == null) return null;
        try {
            managed.saveToFile();
            return LEGACY_GSON.fromJson(Files.readString(managed.getFile().toPath(), StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Couldn't read the config for upload: " + e.getMessage());
            return null;
        }
    }

    /**
     * Replaces every setting with {@code settings} (from settings cloud sync). The current file is kept as
     * skyballs-mod.json.bak, and put back if the new settings can't be loaded.
     */
    public static boolean importJson(JsonObject settings) {
        if (managed == null || settings == null) return false;
        Path path = managed.getFile().toPath();
        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        try {
            managed.saveToFile();
            Files.copy(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(path, LEGACY_GSON.toJson(settings), StandardCharsets.UTF_8);
            migrateConfigShape(path);
            managed.reloadFromFile();
        } catch (Exception e) {
            System.err.println("[SkyBalls] Couldn't load downloaded settings: " + e.getMessage());
            try {
                Files.copy(backup, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                managed.reloadFromFile();
            } catch (Exception ignored) {}
            return false;
        }
        managed.getInstance().saveRunnables.add(managed::saveToFile);
        // The open editor shows the old instance; build a new one next time.
        editor = null;
        managed.saveToFile();
        lastSaved = snapshot();
        return true;
    }

    /**
     * The config editor, kept between openings so /sb opens where you left it (same category, scroll position and
     * open sections). MoulConfig's openConfigGui() builds a new editor every time, which always started at the top.
     */
    private static io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor<SkyBallsConfig> editor;

    public static void openGui() {
        if (managed == null) return;
        if (editor == null) editor = managed.getEditor();
        io.github.notenoughupdates.moulconfig.common.IMinecraft.INSTANCE.openWrappedScreen(editor);
    }

    public static void saveCurrent(SkyBallsConfig config) {
        if (managed != null && managed.getInstance() == config) {
            managed.saveToFile();
            return;
        }

        Path path = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("skyballs-mod.json");
        config.save(path);
    }

    public void save(Path path) {
        if (managed != null && managed.getInstance() == this) {
            managed.saveToFile();
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, LEGACY_GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SkyBalls] Failed to save config: " + e.getMessage());
        }
    }

    private static void migrateLegacy(Path path) throws IOException {
        if (Files.notExists(path)) return;

        String raw = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject old = LEGACY_GSON.fromJson(raw, JsonObject.class);
        if (old == null || old.has("general")) return;

        SkyBallsConfig migrated = new SkyBallsConfig();
        if (old.has("enabled")) 
        if (old.has("firstBootAcknowledged")) migrated.general.firstBootAcknowledged = old.get("firstBootAcknowledged").getAsBoolean();

        if (old.has("farmingRngEnabled")) migrated.farming.rng.enabled = old.get("farmingRngEnabled").getAsBoolean();
        if (old.has("farmingRngBackground")) migrated.farming.rng.background = old.get("farmingRngBackground").getAsBoolean();
        if (old.has("farmingRngScale")) migrated.farming.rng.scale = old.get("farmingRngScale").getAsFloat();
        if (old.has("farmingRngX")) migrated.farming.rng.x = old.get("farmingRngX").getAsInt();
        if (old.has("farmingRngY")) migrated.farming.rng.y = old.get("farmingRngY").getAsInt();
        if (old.has("farmingRngX")) 
        if (old.has("mouseLockEnabled")) migrated.farming.mouseLock.enabled = old.get("mouseLockEnabled").getAsBoolean();
        if (old.has("mouseLockGroundOnly")) migrated.farming.mouseLock.groundOnly = old.get("mouseLockGroundOnly").getAsBoolean();


        Path backup = path.resolveSibling(path.getFileName() + ".legacy-backup");
        Files.move(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Write the migrated object as ordinary JSON. ManagedConfig will load it
        // immediately on the next line.
        Files.writeString(path, LEGACY_GSON.toJson(migrated), StandardCharsets.UTF_8);
    }

    private static void migrateConfigShape(Path path) throws IOException {
        if (Files.notExists(path)) return;
        JsonObject root = LEGACY_GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
        if (root == null) return;
        boolean changed = false;

        if (root.has("pets") && root.get("pets").isJsonObject()) {
            JsonObject pets = root.getAsJsonObject("pets");
            if (pets.has("display") && pets.get("display").isJsonPrimitive()) {
                JsonObject display = new JsonObject();
                display.addProperty("enabled", pets.get("display").getAsBoolean());
                if (pets.has("overflowLevels")) display.add("overflowLevels", pets.remove("overflowLevels"));
                if (pets.has("autoDisplay")) display.add("autoDisplay", pets.remove("autoDisplay"));
                if (pets.has("x")) display.add("x", pets.remove("x"));
                if (pets.has("y")) display.add("y", pets.remove("y"));
                pets.add("display", display);
                changed = true;
            }
        }

        // Materialize Mouse Reset defaults in older configs so the setting
        // is persisted instead of falling back to an in-memory default.
        if (!root.has("misc") || !root.get("misc").isJsonObject()) {
            JsonObject misc = new JsonObject();
            root.add("misc", misc);
            changed = true;
        }
        JsonObject misc = root.getAsJsonObject("misc");
        if (!misc.has("mouseReset") || !misc.get("mouseReset").isJsonObject()) {
            JsonObject mouseReset = new JsonObject();
            mouseReset.addProperty("enabled", true);
            mouseReset.addProperty("accessoryBag", true);
            mouseReset.addProperty("enderChest", true);
            mouseReset.addProperty("backpack", true);
            misc.add("mouseReset", mouseReset);
            changed = true;
        } else {
            JsonObject mouseReset = misc.getAsJsonObject("mouseReset");
            if (!mouseReset.has("enabled")) { mouseReset.addProperty("enabled", true); changed = true; }
            if (!mouseReset.has("accessoryBag")) { mouseReset.addProperty("accessoryBag", true); changed = true; }
            if (!mouseReset.has("enderChest")) { mouseReset.addProperty("enderChest", true); changed = true; }
            if (!mouseReset.has("backpack")) { mouseReset.addProperty("backpack", true); changed = true; }
        }

        // The Enchanting tab became the Experimental Table section in Misc.
        if (root.has("enchanting") && root.get("enchanting").isJsonObject()) {
            if (!misc.has("experimentalTable")) misc.add("experimentalTable", root.get("enchanting"));
            root.remove("enchanting");
            changed = true;
        }

        if (root.has("farming") && root.get("farming").isJsonObject()) {
            JsonObject farming = root.getAsJsonObject("farming");
            if (farming.has("commissions")) {
                JsonObject mining = root.has("mining") && root.get("mining").isJsonObject()
                    ? root.getAsJsonObject("mining") : new JsonObject();
                if (!mining.has("commissions")) mining.add("commissions", farming.remove("commissions"));
                root.add("mining", mining);
                changed = true;
            }
        }

        if (changed) Files.writeString(path, LEGACY_GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static final class FileHolder {
        private final java.io.File file;
        private FileHolder(Path path) {
            this.file = path.toFile();
        }
    }
}
