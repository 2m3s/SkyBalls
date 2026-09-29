package com.epic60869.skyballs.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/**
 * Config sections for SkyBalls's skill features. They are referenced as categories or
 * accordions from {@link com.epic60869.skyballs.SkyBallsConfig}. HUD positions and scales are
 * set in /sj gui and stored in skyballs-huds.json.
 */
public final class FeatureConfigs {
    public static final class Combat {
        @Expose
        @Accordion
        @ConfigOption(name = "Cocoon Alert", desc = "Alert when you cocoon a mob.")
        public CocoonAlert cocoonAlert = new CocoonAlert();

        @Expose
        @Accordion
        @ConfigOption(name = "Rare Drops", desc = "Copy rare drops and animate big ones.")
        public RareDrops rareDrops = new RareDrops();

        @Expose
        @ConfigOption(name = "Arrow Counter", desc = "HUD showing the selected arrow type and how many arrows are left in your quiver.")
        @ConfigEditorBoolean
        public boolean arrowCounter = false;

        @Expose
        @ConfigOption(name = "Zealot Tracker", desc = "Tracker for the Zealots you kill in the End and the Summoning Eyes you drop, for this session or in total.")
        @ConfigEditorBoolean
        public boolean zealotCounter = false;

        @Expose
        @ConfigOption(name = "Legion Display", desc = "HUD showing how many players are within Legion range (30 blocks).")
        @ConfigEditorBoolean
        public boolean legionDisplay = false;
    }

    /** Mayors: a sub-category in the sidebar per mayor (shown under Mayors when it's open). */
    public static final class Mayors {
        @Expose
        @Category(name = "Diana", desc = "Share rare Diana mobs with your party and see when you've done enough damage to lootshare them, like Skysoft.")
        public Diana diana = new Diana();
    }

    /** Diana burrow detection and guesses, ported from SkyBlock Overhaul. */
    public static final class DianaBurrows {
        @Expose
        @ConfigOption(name = "Close Burrow Detection", desc = "Detects burrow locations when being close to them from the particles when holding shovel to register/update it as a Treasure, Mob or Start burrow. Needs Critical Hit and Enchant particles, and set /particlequality extreme. To reset waypoints type /sb clearburrows.")
        @ConfigEditorBoolean
        public boolean closeBurrowDetection = false;

        @Expose
        @ConfigOption(name = "Arrow Guess", desc = "Guesses the burrow location from the arrow direction after digging a burrow.\n§cHave Dust and Smoke Particles enabled and /particlequality extreme!\n§aDo every burrow you see and Use Spade when the mod tells you to for doing Diana the fastest way!")
        @ConfigEditorBoolean
        public boolean arrowGuess = false;

        @Expose
        @ConfigOption(name = "Spade Guess", desc = "Guess the burrow location when using spade ability. Needs Dripping Lava Particles and set /particlequality to Extreme for more accuracy.")
        @ConfigEditorBoolean
        public boolean spadeGuess = false;

        @Expose
        @ConfigOption(name = "Multi Guesses", desc = "Remember previous guess locations when guessing to a new location. Off: a new arrow or spade guess replaces the old ones.")
        @ConfigEditorBoolean
        public boolean multiGuesses = false;

        @Expose
        @ConfigOption(name = "Keep Waypoints on World Change", desc = "Keep your burrow and guess waypoints when you change servers (burrows are yours, so they're in the same places on every Hub). Off: they're cleared.")
        @ConfigEditorBoolean
        public boolean keepOnWorldChange = false;

        @Expose
        @ConfigOption(name = "Show Beacon Beam", desc = "Shows a beacon beam for waypoints going to the sky if enabled.")
        @ConfigEditorBoolean
        public boolean beaconBeam = false;

        @Expose
        @ConfigOption(name = "Show Progress", desc = "Show burrow click progress.")
        @ConfigEditorBoolean
        public boolean showProgress = true;

        @Expose
        @ConfigOption(name = "Progress Position", desc = "Where to show burrow click progress.")
        @ConfigEditorDropdown
        public ProgressPosition progressPosition = ProgressPosition.RIGHT;

        public enum ProgressPosition {
            RIGHT("Right"), LEFT("Left"), ABOVE("Above"), BELOW("Below");

            private final String label;

            ProgressPosition(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        @Expose
        @ConfigOption(name = "Guess Colour", desc = "Border (and beam) colour of arrow and spade guesses.")
        @ConfigEditorColour
        public String guessColour = "0:255:170:0:255";

        @Expose
        @ConfigOption(name = "Mob Colour", desc = "Border (and beam) colour of mob burrows.")
        @ConfigEditorColour
        public String mobColour = "0:255:255:85:85";

        @Expose
        @ConfigOption(name = "Treasure Colour", desc = "Border (and beam) colour of treasure burrows.")
        @ConfigEditorColour
        public String treasureColour = "0:255:255:170:0";

        @Expose
        @ConfigOption(name = "Start Colour", desc = "Border (and beam) colour of start burrows.")
        @ConfigEditorColour
        public String startColour = "0:255:85:255:85";
    }

    public static final class DianaProfitTracker {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the Diana profit tracker HUD in the Hub while you're doing Diana (and for 10 minutes after).")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Show", desc = "Which totals the HUD shows: this session, this mayor term (Diana's whole season) or all time. Also /sb dianatracker session|season|alltime, and /sb dianatracker reset to reset the one shown.")
        @ConfigEditorDropdown
        public com.epic60869.skyballs.features.combat.DianaProfitTracker.Period period = com.epic60869.skyballs.features.combat.DianaProfitTracker.Period.SESSION;
    }

    public static final class DianaLobbyCompromised {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Alert when too many non-party players join the lobby.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Stranger Limit", desc = "Non-party players before alerting.")
        @ConfigEditorSlider(minValue = 2, maxValue = 12, minStep = 1)
        public int strangerLimit = 10;

        @Expose
        @ConfigOption(name = "Title Alert", desc = "A \"Lobby compromised!\" title and a bell.")
        @ConfigEditorBoolean
        public boolean titleAlert = true;

        @Expose
        @ConfigOption(name = "Chat Alert", desc = "Say \"Lobby compromised!\" in party chat.")
        @ConfigEditorBoolean
        public boolean chatAlert = false;
    }

    /** The Diana warp keys (SBO's): set them in Options > Controls > Key Binds, under SkyBalls. */
    public static final class DianaWarp {
        @ConfigOption(name = "Diana Warp", desc = "You must configure the warp keys from vanilla Minecraft settings, under (ESC) -> Options -> Controls -> Key Binds... scroll till you find SkyBalls and configure it from there.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
        public boolean info = false;

        @Expose @ConfigOption(name = "Warp: Castle", desc = "Let the warp keys use /warp castle.") @ConfigEditorBoolean public boolean castle = true;
        @Expose @ConfigOption(name = "Warp: Wizard", desc = "Let the warp keys use /warp wizard.") @ConfigEditorBoolean public boolean wizard = true;
        @Expose @ConfigOption(name = "Warp: Dark Auction", desc = "Let the warp keys use /warp da.") @ConfigEditorBoolean public boolean da = true;
        @Expose @ConfigOption(name = "Warp: Crypt", desc = "Let the warp keys use /warp crypt.") @ConfigEditorBoolean public boolean crypt = false;
        @Expose @ConfigOption(name = "Warp: Stonks", desc = "Let the warp keys use /warp stonks.") @ConfigEditorBoolean public boolean stonks = false;
        @Expose @ConfigOption(name = "Warp: Taylor", desc = "Let the warp keys use /warp taylor.") @ConfigEditorBoolean public boolean taylor = false;
        @Expose @ConfigOption(name = "Warp: Museum", desc = "Let the warp keys use /warp museum.") @ConfigEditorBoolean public boolean museum = false;

        @Expose
        @ConfigOption(name = "Don't Warp If Close", desc = "The warp key won't warp you if you are already within 60 blocks of the target (burrow, guess or rare mob).")
        @ConfigEditorBoolean
        public boolean dontWarpIfClose = true;

        @Expose
        @ConfigOption(name = "Warp Block Difference", desc = "Only warp if the warp is at least this many blocks closer to the target than you are.")
        @ConfigEditorSlider(minValue = 0, maxValue = 60, minStep = 1)
        public int warpDiff = 22;
    }

    /** Diana rare mob sharing and the lootshare helper, ported from Skysoft. */
    public static final class Diana {
        @Expose
        @Accordion
        @ConfigOption(name = "Burrows", desc = "Burrow waypoints from the particles near you, the arrow after each burrow and your spade, like SkyBlock Overhaul (SBO).")
        public DianaBurrows burrows = new DianaBurrows();

        @Expose
        @Accordion
        @ConfigOption(name = "Diana Warp", desc = "Keys that warp you to the warp closest to your next burrow or to a shared rare mob.")
        public DianaWarp warp = new DianaWarp();

        @Expose
        @Accordion
        @ConfigOption(name = "Profit Tracker", desc = "What each Diana drop was worth, the total profit and the time spent, for this session, this mayor term or all time, like SkyHanni's. Move it in /sb hud.")
        public DianaProfitTracker profitTracker = new DianaProfitTracker();

        @Expose
        @Accordion
        @ConfigOption(name = "Lobby Compromised", desc = "Alert when too many non-party players join the lobby while you do Diana, like Skysoft.")
        public DianaLobbyCompromised lobbyCompromised = new DianaLobbyCompromised();

        @Expose
        @ConfigOption(name = "Sphinx Solver", desc = "Helps you solve the sphinx riddle by showing you the answer choices in chat and it automatically clicks the correct one for you when you click anywhere while the chat is open.\nTheres also the option to us a keybind in the mc keybinds menu but §c⚠ USE AT YOUR OWN RISK ⚠")
        @ConfigEditorBoolean
        public boolean sphinxSolver = false;

        @Expose
        @ConfigOption(name = "Rare Mob Sharing", desc = "When you dig up a rare mob, send its coordinates in party chat. Rare mobs your party shares get a waypoint and a title.")
        @ConfigEditorBoolean
        public boolean rareMobSharing = false;

        @Expose
        @ConfigOption(name = "All Rare Mobs", desc = "Share and receive every rare Diana mob. Off: only Minos Inquisitors and King Minos.")
        @ConfigEditorBoolean
        public boolean allRareMobs = false;

        @Expose
        @ConfigOption(name = "Own Mob Alerts", desc = "A title when you dig up a rare mob you share.")
        @ConfigEditorBoolean
        public boolean ownMobAlerts = true;

        @Expose
        @ConfigOption(name = "Line to Rare Mob", desc = "Draw a line from your crosshair to the nearest shared rare mob.")
        @ConfigEditorBoolean
        public boolean crosshairLine = true;

        @Expose
        @ConfigOption(name = "Show Party Messages", desc = "Keep the rare mob and lootshare party messages in chat. Off: they're hidden, and a shared mob shows as \"Name found a Minos Inquisitor at x y z\".")
        @ConfigEditorBoolean
        public boolean showPartyMessages = false;

        @Expose
        @ConfigOption(name = "Lootshare Helper", desc = "Count your damage on rare mobs your party shares and show whether you've done the 1% needed to lootshare them.")
        @ConfigEditorBoolean
        public boolean lootshare = false;

        @Expose
        @ConfigOption(name = "Share Secured Message", desc = "Say \"Loot share secured!\" in party chat once you've done enough damage.")
        @ConfigEditorBoolean
        public boolean shareSecuredMessage = true;

        @Expose
        @ConfigOption(name = "Party Checkmarks", desc = "A checkmark above party members who secured lootshare (cyan) or spawned the rare mob (pink).")
        @ConfigEditorBoolean
        public boolean partyCheckmarks = true;

        @Expose
        @ConfigOption(name = "Lootshare Radius", desc = "Draw the 30 block lootshare radius around the rare mob.")
        @ConfigEditorBoolean
        public boolean lootshareRadius = true;

        @Expose
        @ConfigOption(name = "Lootshare Missing Colour", desc = "Colour of \"Lootsharing\" above the mob before you've done enough damage.")
        @ConfigEditorColour
        public String lootshareMissingColor = "0:230:255:85:85";

        @Expose
        @ConfigOption(name = "Lootshare Ready Colour", desc = "Colour of \"Lootsharing\" above the mob once you've done enough damage.")
        @ConfigEditorColour
        public String lootshareReadyColor = "0:230:85:255:255";
    }

    /** Dungeons: every section is a sub-category in the sidebar (shown under Dungeons when it's open), like SkyHanni's. */
    public static final class Dungeons {
        @Expose
        @Category(name = "F7/M7", desc = "Floor 7 and Master Mode 7: dragons and relics, terminal and device solvers, and the 3x3 platform highlight.")
        public Floor7 f7 = new Floor7();

        @Expose
        @Category(name = "Dungeon Map", desc = "Dungeon map HUD.")
        public DungeonMap map = new DungeonMap();

        @Expose
        @Category(name = "Puzzle Solvers", desc = "Solutions for dungeon puzzles.")
        public Puzzles puzzles = new Puzzles();

        @Expose
        @Category(name = "Secrets and Routes", desc = "Secret waypoints and your recorded routes.")
        public Secrets secrets = new Secrets();

        @Expose
        @Category(name = "Mobs", desc = "Dungeon mob highlighting.")
        public DungeonMobs mobs = new DungeonMobs();

        @Expose
        @Category(name = "Timers and Alerts", desc = "Splits, tick timers, mask timers and debuff alerts.")
        public Timers timers = new Timers();

        @Expose
        @Category(name = "Score", desc = "270/300 score alerts and the score display.")
        public Score score = new Score();

        @Expose
        @Category(name = "Leap Menu", desc = "Odin-style Spirit Leap menu with a box per teammate, coloured by class.")
        public LeapMenu leapMenu = new LeapMenu();

        @Expose
        @Category(name = "Positional Messages", desc = "Party messages sent when you reach a spot (/sb posmsg), plus built-in waypoints like Py Stand Here.")
        public PositionalMessages positionalMessages = new PositionalMessages();

        @Expose
        @Category(name = "Blood Camp", desc = "Watcher move prediction and blood mob kill timers.")
        public BloodCamp bloodCamp = new BloodCamp();

        @Expose
        @Category(name = "Case Opening", desc = "Open Obsidian and Bedrock reward chests like a CS2 case (SkyOcean's Dungeon Gambling).")
        public CaseOpening caseOpeningMenu = new CaseOpening();
    }

    public static final class Floor7 {
        @Expose
        @Accordion
        @ConfigOption(name = "M7 Dragons and Relics", desc = "Master Mode floor 7 dragon phase (P5): spawn alerts with split priority, timers, kill areas and relic spots, like NoFrills' Wither Dragons and Relic Highlight.")
        public WitherDragons witherDragons = new WitherDragons();

        @Expose
        @Accordion
        @ConfigOption(name = "Terminals and Devices", desc = "Floor 7 terminal and device solvers.")
        public Terminals terminals = new Terminals();

        @Expose
        @Accordion
        @ConfigOption(name = "Platform Highlight (3x3)", desc = "One big box over the floor 7 3x3 platform (53-55, 63, 113-115), from when Goldor starts, like NoFrills.")
        public PlatformHighlight platformHighlight = new PlatformHighlight();
    }

    private FeatureConfigs() {}

    public static final class CocoonAlert {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show an on-screen alert when you cocoon a slayer boss, slayer miniboss, elusive mob or important boss.")
        @ConfigEditorBoolean
        public boolean enabled = false;
    }

    public static final class RareDrops {
        @Expose
        @ConfigOption(name = "Copy Rare Drops", desc = "Copy rare drop messages to your clipboard (RARE, VERY RARE, CRAZY RARE, INSANE, PRAY TO RNGESUS, PET and RNG METER drops, rare crops, rare rewards and outstanding catches). What was copied shows in chat, using Copy Chat's preview settings.")
        @ConfigEditorBoolean
        public boolean copy = false;

        @Expose
        @ConfigOption(name = "Copy RNG Drops", desc = "Copy only RNG drops to your clipboard: PRAY TO RNGESUS and RNG METER drops, and slayer, Diana and fishing RNG items (Skyblocker's list). Not needed with Copy Rare Drops on, which copies these too.")
        @ConfigEditorBoolean
        public boolean copyRng = false;

        @Expose
        @ConfigOption(name = "Copy Slayer Drops", desc = "Copy only slayer drops to your clipboard: anything on a slayer's drop list (SkyHanni's slayer profit tracker lists). Not needed with Copy Rare Drops on.")
        @ConfigEditorBoolean
        public boolean copySlayer = false;

        @Expose
        @ConfigOption(name = "Copy Garden Drops", desc = "Copy only Garden drops to your clipboard: RARE CROP drops and anything else that drops in the Garden, like pest drops. Not needed with Copy Rare Drops on.")
        @ConfigEditorBoolean
        public boolean copyGarden = false;

        @Expose
        @ConfigOption(name = "Big Drop Animation", desc = "Play an animation when a rare drop is worth more than the threshold.")
        @ConfigEditorBoolean
        public boolean animation = false;

        @Expose
        @ConfigOption(name = "Animation Threshold (millions)", desc = "Minimum drop value, in millions of coins, for the animation.")
        @ConfigEditorSlider(minValue = 1, maxValue = 500, minStep = 1)
        public float thresholdMillions = 10;

        @Expose
        @ConfigOption(name = "RNG Drop Totem Animation", desc = "Skyblocker's special effect: big RNG drops (slayer, Diana and fishing RNG items, and any PRAY TO RNGESUS or RNG METER drop) pop up like a Totem of Undying, with particles and a sound.")
        @ConfigEditorBoolean
        public boolean totemAnimation = false;
    }

    public static final class Slayer {
        @Expose
        @ConfigOption(name = "Boss Phase Display", desc = "HUD showing your slayer boss's nametag lines, including Voidgloom hits and Inferno attunement.")
        @ConfigEditorBoolean
        public boolean phaseDisplay = false;
    }

    /** Voidgloom Seraph helpers, ported from SkyHanni's Enderman slayer features. */
    public static final class EndermanSlayer {
        @Expose
        @ConfigOption(name = "Highlight Yang Glyph", desc = "Highlight the Yang Glyph (beacon) while a Voidgloom holds it, throws it and after it lands, with a timer until it explodes.")
        @ConfigEditorBoolean
        public boolean highlightBeacon = false;

        @Expose
        @ConfigOption(name = "Yang Glyph Colour", desc = "Colour of the Yang Glyph highlight.")
        @ConfigEditorColour
        public String beaconColor = "0:255:255:0:88";

        @Expose
        @ConfigOption(name = "Yang Glyph Warning", desc = "A title when a Voidgloom throws a Yang Glyph.")
        @ConfigEditorBoolean
        public boolean beaconWarning = false;

        @Expose
        @ConfigOption(name = "Line to Yang Glyph", desc = "Draw a line from your crosshair to the Yang Glyph.")
        @ConfigEditorBoolean
        public boolean beaconLine = false;

        @Expose
        @ConfigOption(name = "Yang Glyph Line Colour", desc = "Colour of the line to the Yang Glyph.")
        @ConfigEditorColour
        public String beaconLineColor = "0:255:255:0:88";

        @Expose
        @ConfigOption(name = "Yang Glyph Line Width", desc = "Width of the line to the Yang Glyph.")
        @ConfigEditorSlider(minValue = 1, maxValue = 10, minStep = 1)
        public int beaconLineWidth = 3;

        @Expose
        @ConfigOption(name = "Highlight Nukekubi Skulls", desc = "Highlight the Nukekubi Fixation skulls (the eyes) in gold.")
        @ConfigEditorBoolean
        public boolean highlightNukekubi = false;

        @Expose
        @ConfigOption(name = "Line to Nukekubi Skulls", desc = "Draw a line from your crosshair to each Nukekubi Fixation skull you can see.")
        @ConfigEditorBoolean
        public boolean lineToNukekubi = false;

        @Expose
        @ConfigOption(name = "Phase Numbers", desc = "Put the boss's phase (1/3, or 1/6 for tier IV) in front of its health in the Slayer Boss Phase HUD.")
        @ConfigEditorBoolean
        public boolean phaseDisplay = false;

        @Expose
        @ConfigOption(name = "Hide Particles", desc = "Hide the smoke, flame and witch particles around endermen in The End.")
        @ConfigEditorBoolean
        public boolean hideParticles = false;

        @Expose
        @ConfigOption(name = "Line to Boss", desc = "Draw a line from your crosshair to your Voidgloom Seraph while you can see it.")
        @ConfigEditorBoolean
        public boolean lineToBoss = false;

        @Expose
        @ConfigOption(name = "Boss Line Width", desc = "Width of the line to your Voidgloom Seraph.")
        @ConfigEditorSlider(minValue = 1, maxValue = 10, minStep = 1)
        public int bossLineWidth = 3;
    }

    /** Inferno Demonlord helpers, ported from SkyHanni's Blaze slayer features. */
    public static final class BlazeSlayer {
        @Expose
        @ConfigOption(name = "Hellion Shield Colours", desc = "Outline the Inferno Demonlord and its demons in their Hellion Shield's colour and show the shield above them.")
        @ConfigEditorBoolean
        public boolean coloredMobs = false;

        @Expose
        @ConfigOption(name = "Dagger Display", desc = "HUD with the attunements on your Twilight and Firedust daggers, updated as soon as you swap them. Hypixel's attunement title is hidden while it shows.")
        @ConfigEditorBoolean
        public boolean daggers = false;

        @Expose
        @ConfigOption(name = "Mark Right Dagger", desc = "In the dagger HUD, mark the attunement that matches the nearest boss or demon's shield.")
        @ConfigEditorBoolean
        public boolean markRightDagger = false;

        @Expose
        @ConfigOption(name = "First Dagger", desc = "Which dagger is shown on the left of the dagger HUD.")
        @ConfigEditorDropdown
        public com.epic60869.skyballs.features.slayer.BlazeSlayer.FirstDagger firstDagger = com.epic60869.skyballs.features.slayer.BlazeSlayer.FirstDagger.TWILIGHT;

        @Expose
        @ConfigOption(name = "Hide Dagger Chat", desc = "Hide Hypixel's \"Strike using the ... attunement\" and \"Your hit was reduced by Hellion Shield!\" messages.")
        @ConfigEditorBoolean
        public boolean hideDaggerChat = false;

        @Expose
        @ConfigOption(name = "Fire Pits Warning", desc = "A title and sound when a tier III or IV Inferno Demonlord drops below a third of its health and the fire pits start.")
        @ConfigEditorBoolean
        public boolean firePitsWarning = false;

        @Expose
        @ConfigOption(name = "Phase Numbers", desc = "Put the boss's phase (1/2, or 1/3 for tiers III and IV) in front of its health in the Slayer Boss Phase HUD.")
        @ConfigEditorBoolean
        public boolean phaseDisplay = false;

        @Expose
        @ConfigOption(name = "Clear View", desc = "Hide particles and fireballs within 10 blocks of an Inferno Demonlord.")
        @ConfigEditorBoolean
        public boolean clearView = false;

        @Expose
        @ConfigOption(name = "Fire Pillar Display", desc = "HUD with the time until a Fire Pillar explodes, for any player's boss. Move it in /sb hud.")
        @ConfigEditorBoolean
        public boolean firePillarDisplay = false;
    }

    public static final class Garden {
        @Expose
        @ConfigOption(name = "Yaw and Pitch", desc = "HUD showing your yaw, pitch and facing direction.")
        @ConfigEditorBoolean
        public boolean yawPitch = false;

        @Expose
        @ConfigOption(name = "Pest Cooldown", desc = "HUD counting down from the last pest spawn.")
        @ConfigEditorBoolean
        public boolean pestCooldown = false;

        @Expose
        @ConfigOption(name = "Pest Cooldown (seconds)", desc = "Your pest spawn cooldown in seconds.")
        @ConfigEditorSlider(minValue = 60, maxValue = 900, minStep = 5)
        public float pestCooldownSeconds = 300;

        @Expose
        @ConfigOption(name = "Blocks Per Second", desc = "HUD showing how many blocks per second you are breaking.")
        @ConfigEditorBoolean
        public boolean blocksPerSecond = false;

        @Expose
        @ConfigOption(name = "Special Drop Animation", desc = "Play an animation when you drop a farming dye or a Ray of Helios.")
        @ConfigEditorBoolean
        public boolean specialDropAnimation = false;
    }

    public static final class Fishing {
        @Expose
        @ConfigOption(name = "Stat Display", desc = "HUD with fishing speed, sea creature, trophy fish, double hook and treasure chance from your tab list stats.")
        @ConfigEditorBoolean
        public boolean statDisplay = false;

        @Expose
        @ConfigOption(name = "Hook Timer", desc = "HUD showing the time until your fish bites.")
        @ConfigEditorBoolean
        public boolean hookTimer = false;

        @Expose
        @ConfigOption(name = "Bait Display", desc = "HUD showing your bait and how much is left.")
        @ConfigEditorBoolean
        public boolean baitDisplay = false;

        @Expose
        @ConfigOption(name = "Rare Sea Creature Alert", desc = "Alert when you catch a rare sea or lava creature.")
        @ConfigEditorBoolean
        public boolean rareCreatureAlert = false;

        @Expose
        @ConfigOption(name = "Alert Rarity", desc = "Minimum sea creature rarity to alert for.")
        @ConfigEditorDropdown
        public CreatureRarity alertRarity = CreatureRarity.LEGENDARY;
    }

    public enum CreatureRarity {
        RARE, EPIC, LEGENDARY, MYTHIC;

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    public static final class MiningFeatures {
        @Expose
        @ConfigOption(name = "Commissions For Current Area Only", desc = "Only show the commission HUD in the Dwarven Mines, Crystal Hollows, Glacite Tunnels and Mineshafts.")
        @ConfigEditorBoolean
        public boolean commissionsAreaOnly = true;

        @Expose
        @ConfigOption(name = "Crystal Hollows Map", desc = "HUD map of the Crystal Hollows with your position and the structures you have found.")
        @ConfigEditorBoolean
        public boolean crystalHollowsMap = false;

        @Expose
        @ConfigOption(name = "Crystal Hollows Waypoints", desc = "Mark Mines of Divan, Jungle Temple, Goblin Queen's Den and other places when you find them, like Skyblocker. /sb crystalwaypoints add|share|remove|clear.")
        @ConfigEditorBoolean
        public boolean crystalWaypoints = false;

        @Expose
        @ConfigOption(name = "Waypoints From Chat", desc = "Turn Crystal Hollows coordinates in chat into waypoints.")
        @ConfigEditorBoolean
        public boolean crystalWaypointsFromChat = true;

        @Expose
        @ConfigOption(name = "Pickaxe Ability HUD", desc = "Cooldown of your pickaxe ability (Mining Speed Boost, Pickobulus, ...) on the mining islands (Gold Mine, Deep Caverns, Dwarven Mines, Crystal Hollows, Mineshafts). Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean pickaxeAbilityHud = false;

        @Expose
        @ConfigOption(name = "Pickaxe Ability Ready Alert", desc = "Show a title when your pickaxe ability is ready again.")
        @ConfigEditorBoolean
        public boolean pickaxeAbilityAlert = false;

        @Expose
        @ConfigOption(name = "Mines of Divan Tools Alert", desc = "Alert when you are holding all four scavenged tools for the Jade Crystal.")
        @ConfigEditorBoolean
        public boolean divanToolsAlert = false;

        @Expose
        @ConfigOption(name = "Mineshaft Timer", desc = "HUD with your time in the mineshaft and time until you freeze.")
        @ConfigEditorBoolean
        public boolean mineshaftTimer = false;

        @Expose
        @ConfigOption(name = "Pristine Record", desc = "Keep your highest pristine proc, overall and per gemstone, and alert on a new PB. /sb pristine to see them.")
        @ConfigEditorBoolean
        public boolean pristineRecord = false;
    }

    public static final class Foraging {
        @Expose
        @ConfigOption(name = "Sweep Display", desc = "HUD showing your Sweep stat and how many logs you will cut.")
        @ConfigEditorBoolean
        public boolean sweepDisplay = false;
    }

    public static final class Enchanting {
        @Expose
        @ConfigOption(name = "Chronomatron Solver", desc = "Highlight the Chronomatron pattern.")
        @ConfigEditorBoolean
        public boolean chronomatron = false;

        @Expose
        @ConfigOption(name = "Superpairs Solver", desc = "Show revealed Superpairs cards.")
        @ConfigEditorBoolean
        public boolean superpairs = false;

        @Expose
        @ConfigOption(name = "Ultrasequencer Solver", desc = "Highlight the Ultrasequencer order.")
        @ConfigEditorBoolean
        public boolean ultrasequencer = false;

        @Expose
        @ConfigOption(name = "Ultrasequencer Numbers", desc = "Show the click order as numbers on every Ultrasequencer slot, instead of only highlighting the next one.")
        @ConfigEditorBoolean
        public boolean ultrasequencerNumbers = true;
    }

    public static final class Runecrafting {
        @Expose
        @ConfigOption(name = "Valuable Rune Alert", desc = "Alert when you get a rune from the list below.")
        @ConfigEditorBoolean
        public boolean valuableRuneAlert = false;

        @Expose
        @ConfigOption(name = "Runes", desc = "Comma-separated rune names to alert for.")
        @ConfigEditorText
        public String runes = "Music, Enchant, Grand Searing, Rainbow, Spellbound, Grand Freezing, Primal Fear, Golden Carpet";
    }

    public static final class CaseOpening {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Open Obsidian and Bedrock reward chests like a CS2 case (SkyOcean's Dungeon Gambling): the items spin past and stop on the best one. Esc skips it.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "All Chests", desc = "Spin every reward chest, not only Obsidian and Bedrock.")
        @ConfigEditorBoolean
        public boolean allChests = false;

        @Expose
        @ConfigOption(name = "Croesus", desc = "Also spin the chests you open at Croesus in the Dungeon Hub, with that run's floor drops.")
        @ConfigEditorBoolean
        public boolean croesus = false;

        @Expose
        @ConfigOption(name = "Hide Contents In Croesus", desc = "In Croesus's menu for a run, hide what's in the chests that will spin, so the spin shows you.")
        @ConfigEditorBoolean
        public boolean hideCroesusContents = true;

        @Expose
        @ConfigOption(name = "Seconds", desc = "How long the spin takes.")
        @ConfigEditorSlider(minValue = 2, maxValue = 12, minStep = 1)
        public int seconds = 6;

        @Expose
        @ConfigOption(name = "Gold Sound", desc = "Play the \"GOLD GOLD GOLD\" sound as soon as a chest whose best item is Legendary (gold) or better starts spinning.")
        @ConfigEditorBoolean
        public boolean goldSound = true;
    }

    public static final class DungeonMap {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the dungeon map.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Fancy Map", desc = "Show player heads and room colours on the map.")
        @ConfigEditorBoolean
        public boolean fancy = true;

        @Expose
        @ConfigOption(name = "Room Labels", desc = "Show room names and secret counts on the map.")
        @ConfigEditorBoolean
        public boolean roomLabels = true;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a blurred background behind the dungeon map.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose public int x = 2;
        @Expose public int y = 2;
        @Expose public float scale = 1f;
    }

    /** Ice Fill, Boulder, Creeper Beams, Three Weirdos, Quiz, Teleport Maze, Water Board and Blaze use Odin's solvers. */
    public static final class Puzzles {
        @Expose @ConfigOption(name = "Tic Tac Toe", desc = "Show the best move.") @ConfigEditorBoolean public boolean ticTacToe = false;
        @Expose @ConfigOption(name = "Silverfish", desc = "Show the path.") @ConfigEditorBoolean public boolean silverfish = false;
        @Expose @ConfigOption(name = "Three Weirdos", desc = "Odin: the chest with the reward in green, wrong chests in red.") @ConfigEditorBoolean public boolean threeWeirdos = false;
        @Expose @ConfigOption(name = "Creeper Beams", desc = "Odin: each pair of lanterns to connect in its own colour, with a line between them.") @ConfigEditorBoolean public boolean creeperBeams = false;
        @Expose @ConfigOption(name = "Water Board", desc = "Odin: when to flip each lever, with CLICK ME! and countdowns, and a line to the next lever.") @ConfigEditorBoolean public boolean waterBoard = false;
        @Expose @ConfigOption(name = "Water Board Optimized", desc = "Use Odin's faster Water Board solutions.") @ConfigEditorBoolean public boolean waterOptimized = false;
        @Expose @ConfigOption(name = "Water Board Path Preview", desc = "Skyblocker: show where the water will flow on the board right now.") @ConfigEditorBoolean public boolean waterPreviewPath = true;
        @Expose @ConfigOption(name = "Water Board Lever Preview", desc = "Skyblocker: while looking at a lever, show which blocks flipping it adds (green) and removes (red).") @ConfigEditorBoolean public boolean waterPreviewLevers = true;
        @Expose @ConfigOption(name = "Blaze", desc = "Odin: the next three blazes to shoot (green, orange, white) with lines between them.") @ConfigEditorBoolean public boolean blaze = false;
        @Expose @ConfigOption(name = "Show All Blazes", desc = "Also box every other blaze.") @ConfigEditorBoolean public boolean blazeShowAll = false;
        @Expose @ConfigOption(name = "Boulder", desc = "Odin: the boulder to push next. The box clears when you click its button.") @ConfigEditorBoolean public boolean boulder = false;
        @Expose @ConfigOption(name = "Show All Boulder Clicks", desc = "Show every boulder to push instead of only the next one.") @ConfigEditorBoolean public boolean boulderShowAll = false;
        @Expose @ConfigOption(name = "Ice Fill", desc = "Odin: the path over each Ice Fill floor.") @ConfigEditorBoolean public boolean iceFill = false;
        @Expose @ConfigOption(name = "Ice Fill Optimized Patterns", desc = "Use Odin's shorter (harder) Ice Fill paths.") @ConfigEditorBoolean public boolean iceFillOptimized = false;
        @Expose @ConfigOption(name = "Quiz", desc = "Odin: a box and beam on the right answer.") @ConfigEditorBoolean public boolean trivia = false;
        @Expose @ConfigOption(name = "Teleport Maze", desc = "Odin: visited pads in red, the right pad in green (orange while there are several), and a line to the best next pad.") @ConfigEditorBoolean public boolean teleportMaze = false;
    }

    public static final class Secrets {
        @Expose
        @ConfigOption(name = "Secret Waypoints", desc = "Show waypoints for each room's secrets.")
        @ConfigEditorBoolean
        public boolean secretWaypoints = false;

        @Expose
        @ConfigOption(name = "Show Routes", desc = "Show a secret route for the current room: yours if you recorded one, otherwise Stella's. Record with /sb route start and /sb route stop; share with /sb export; import a Stella (or SecretRoutes) export with /sb route import (clipboard) or /sb route import <file>.")
        @ConfigEditorBoolean
        public boolean routes = false;

        @Expose
        @ConfigOption(name = "Door Highlight", desc = "Outline wither and blood doors: green when your team has the key, red when locked.")
        @ConfigEditorBoolean
        public boolean doorHighlight = false;

        @Expose
        @ConfigOption(name = "Key Highlight", desc = "Outline dropped Wither and Blood keys, visible through walls.")
        @ConfigEditorBoolean
        public boolean keyHighlight = false;

        @Expose
        @ConfigOption(name = "Announce Key Spawn", desc = "Show a title when a Wither or Blood key spawns.")
        @ConfigEditorBoolean
        public boolean announceKeySpawn = false;
    }

    public static final class DungeonMobs {
        @Expose
        @ConfigOption(name = "Highlight Starred Mobs", desc = "Draw a box around starred (✯) dungeon mobs you can see. Hidden behind walls.")
        @ConfigEditorBoolean
        public boolean starredMobs = false;

        @Expose
        @ConfigOption(name = "Starred Mob Colour", desc = "Colour of the box around starred mobs.")
        @ConfigEditorColour
        public String starredColor = "0:255:255:217:51";

        @Expose
        @ConfigOption(name = "Fill Opacity", desc = "How see-through the fill inside the box is. 0 draws only the outline.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.05f)
        public float starredFill = 0f;

        @Expose
        @ConfigOption(name = "Line Width", desc = "Thickness of the box outline.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 0.5f)
        public float starredLineWidth = 2f;
    }

    public enum TerminalStyle {
        ODIN("Odin"), NOAMM("NoammAddons");

        private final String label;

        TerminalStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum NoammSlotStyle {
        RECT("Rect"), BORDERED("Bordered Rect"), BUTTON("Button");

        private final String label;

        NoammSlotStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class Terminals {
        @Expose @ConfigOption(name = "Terminal Solver", desc = "Which terminal solver to use. Odin: covers the terminal and shows what to click. NoammAddons: its big centred panel with the terminal's name, slot styles and colours.") @ConfigEditorDropdown public TerminalStyle solverStyle = TerminalStyle.ODIN;
        @Expose @ConfigOption(name = "NoammAddons: Scale", desc = "Size of the NoammAddons terminal panel.") @ConfigEditorSlider(minValue = 0.3f, maxValue = 2f, minStep = 0.05f) public float noammScale = 1f;
        @Expose @ConfigOption(name = "NoammAddons: Slot Style", desc = "How solution slots are drawn in the NoammAddons panel.") @ConfigEditorDropdown public NoammSlotStyle noammSlotStyle = NoammSlotStyle.RECT;
        @Expose @ConfigOption(name = "NoammAddons: Show Numbers", desc = "Show the number on each slot in Click in order!") @ConfigEditorBoolean public boolean noammShowNumbers = false;
        @Expose @ConfigOption(name = "Block Misclicks", desc = "In terminals, ignore clicks on slots that aren't part of the solution (and wrong-button rubix clicks).") @ConfigEditorBoolean public boolean blockMisclicks = true;
        @Expose @ConfigOption(name = "Client Prediction", desc = "Update the solution as soon as you click instead of waiting for the server.") @ConfigEditorBoolean public boolean clickPrediction = true;
        @Expose @ConfigOption(name = "Resolve Timeout (ms)", desc = "How long a predicted click waits for the server before the terminal is re-read.") @ConfigEditorSlider(minValue = 300, maxValue = 1200, minStep = 10) public int resolveTimeout = 600;
        @Expose @ConfigOption(name = "First Click Protection (ms)", desc = "Clicks this soon after a terminal opens are ignored (Odin recommends 500 minus your ping).") @ConfigEditorSlider(minValue = 350, maxValue = 800, minStep = 10) public int firstClickProt = 500;
        @Expose @ConfigOption(name = "Account For Server Lag", desc = "Also block clicks until the terminal has been open for the ticks below.") @ConfigEditorBoolean public boolean lagProtection = false;
        @Expose @ConfigOption(name = "Lag Protection Ticks", desc = "Server ticks (50ms each) before clicks go through.") @ConfigEditorSlider(minValue = 7, maxValue = 16, minStep = 1) public int lagProtectionTicks = 8;
        @Expose @ConfigOption(name = "Rubix Left Clicks Only", desc = "Only use left clicks for the rubix terminal instead of the fewest clicks.") @ConfigEditorBoolean public boolean rubixLeftClicksOnly = false;
        @Expose @ConfigOption(name = "Melody Solver", desc = "Show the melody solver.") @ConfigEditorBoolean public boolean melodySolver = false;
        @Expose @ConfigOption(name = "Odin Device Solvers", desc = "Odin's Simon Says, Arrow Align and Sharp Shooter (i4) solvers. Off: Skyblocker's.") @ConfigEditorBoolean public boolean odinDevices = false;
        @Expose @ConfigOption(name = "SS Block Wrong Clicks", desc = "Simon Says: ignore clicks on any button but the next one. Hold shift to click anyway.") @ConfigEditorBoolean public boolean ssBlockWrong = true;
        @Expose @ConfigOption(name = "SS Announce Progress", desc = "Send \"SS n/5\" to party chat when you click the last button of a round.") @ConfigEditorBoolean public boolean ssAnnounce = false;
        @Expose @ConfigOption(name = "SS First", desc = "") @ConfigEditorColour public String ssFirstColor = "0:128:85:255:85";
        @Expose @ConfigOption(name = "SS Second", desc = "") @ConfigEditorColour public String ssSecondColor = "0:128:255:170:0";
        @Expose @ConfigOption(name = "SS Rest", desc = "") @ConfigEditorColour public String ssThirdColor = "0:128:255:85:85";
        @Expose @ConfigOption(name = "Arrow Align Block Wrong Clicks", desc = "Arrow Align: ignore clicks on frames that are already right. Hold shift to click anyway.") @ConfigEditorBoolean public boolean arrowAlignBlockWrong = true;
        @Expose @ConfigOption(name = "i4 Aim Positions", desc = "Sharp Shooter: also show the three best places to aim to hit two blocks at once.") @ConfigEditorBoolean public boolean i4AimPositions = false;
        @Expose @ConfigOption(name = "i4 Complete Alert", desc = "Sharp Shooter: show a title when you complete the device.") @ConfigEditorBoolean public boolean i4CompleteAlert = false;
        @Expose @ConfigOption(name = "Background", desc = "") @ConfigEditorColour public String backgroundColor = "0:128:38:38:38";
        @Expose @ConfigOption(name = "Panes", desc = "") @ConfigEditorColour public String panesColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Rubix 1", desc = "") @ConfigEditorColour public String rubix1Color = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Rubix 2", desc = "") @ConfigEditorColour public String rubix2Color = "0:255:42:127:42";
        @Expose @ConfigOption(name = "Rubix -1", desc = "") @ConfigEditorColour public String rubixMinus1Color = "0:255:170:0:0";
        @Expose @ConfigOption(name = "Rubix -2", desc = "") @ConfigEditorColour public String rubixMinus2Color = "0:255:85:0:0";
        @Expose @ConfigOption(name = "Numbers Next", desc = "Click in order: the slot to click now.") @ConfigEditorColour public String numbersNextColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Numbers Second", desc = "Click in order: the slot after that.") @ConfigEditorColour public String numbersSecondColor = "0:255:255:255:85";
        @Expose @ConfigOption(name = "Numbers Third", desc = "Click in order: the third slot.") @ConfigEditorColour public String numbersThirdColor = "0:255:255:85:85";
        @Expose @ConfigOption(name = "Starts With", desc = "") @ConfigEditorColour public String startsWithColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Select", desc = "") @ConfigEditorColour public String selectColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Melody Column", desc = "") @ConfigEditorColour public String melodyColumnColor = "0:255:170:0:170";
        @Expose @ConfigOption(name = "Melody Pointer", desc = "") @ConfigEditorColour public String melodyPointerColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Melody Background", desc = "The Melody lanes and buttons that aren't lit (Odin style).") @ConfigEditorColour public String melodyBackgroundColor = "0:255:38:38:38";
        @Expose @ConfigOption(name = "Simon Says", desc = "Highlight the buttons to press.") @ConfigEditorBoolean public boolean simonSays = false;
        @Expose @ConfigOption(name = "Lights On", desc = "Highlight the levers to flip.") @ConfigEditorBoolean public boolean lightsOn = false;
        @Expose @ConfigOption(name = "Arrow Align", desc = "Show how many clicks each frame needs.") @ConfigEditorBoolean public boolean arrowAlign = false;
        @Expose @ConfigOption(name = "Target Practice (i4)", desc = "Highlight the targets to shoot.") @ConfigEditorBoolean public boolean targetPractice = false;
    }

    public static final class Timers {
        @Expose
        @ConfigOption(name = "Splits", desc = "Odin-style split HUD (Blood Open, Blood Clear, Portal Entry, each boss phase, Total) with personal bests per floor and a chat message after each split.")
        @ConfigEditorBoolean
        public boolean splits = false;

        @Expose
        @ConfigOption(name = "Split Messages", desc = "Send \"<split> took <time>\" with your PB to chat when a split finishes.")
        @ConfigEditorBoolean
        public boolean splitMessages = true;

        @Expose
        @ConfigOption(name = "Boss Entry Split", desc = "Add a Boss Entry row (Blood Open + Blood Clear + Portal Entry) to the split HUD.")
        @ConfigEditorBoolean
        public boolean bossEntrySplit = true;

        @Expose
        @ConfigOption(name = "Show Tick Time", desc = "Show the split time counted in server ticks next to the real time.")
        @ConfigEditorBoolean
        public boolean splitTickTime = true;

        @Expose
        @ConfigOption(name = "Tick Timers", desc = "HUD for Storm's pillars (20 ticks) and Goldor's death tick (50 ticks by default).")
        @ConfigEditorBoolean
        public boolean tickTimers = false;

        @Expose
        @ConfigOption(name = "Goldor Tick Period", desc = "Server ticks between Goldor's death ticks.")
        @ConfigEditorSlider(minValue = 20, maxValue = 100, minStep = 1)
        public int goldorTickPeriod = 50;

        @Expose
        @ConfigOption(name = "Mask Timers", desc = "HUD with the Spirit Mask, Bonzo's Mask and Phoenix pet: invincibility time (gold), cooldown (red) or ready (green), counted in server ticks. Your worn mask is marked with a purple bar.")
        @ConfigEditorBoolean
        public boolean maskTimers = false;

        @Expose
        @ConfigOption(name = "Mask Proc Alert", desc = "Show a title and play a sound when a mask or Phoenix procs.")
        @ConfigEditorBoolean
        public boolean maskAlert = false;

        @Expose
        @ConfigOption(name = "Announce Mask Procs", desc = "Send \"<Mask> Procced! (n/3)\" to party chat when one of your masks or Phoenix procs.")
        @ConfigEditorBoolean
        public boolean maskAnnounce = false;

        @Expose
        @ConfigOption(name = "Max Debuff Alert", desc = "Alert when your own Last Breath shots (5), Ice Spray uses (1) and Lethality hits (5) reach the max debuff on an M7 dragon. Counts reset when a new dragon spawns.")
        @ConfigEditorBoolean
        public boolean debuffAlert = false;

        @Expose
        @ConfigOption(name = "Last Breath Release", desc = "Play a sound and show RELEASE once you have charged Last Breath for the set number of server ticks.")
        @ConfigEditorBoolean
        public boolean lastBreathRelease = false;

        @Expose
        @ConfigOption(name = "Last Breath Ticks", desc = "Server ticks of charging before the release cue.")
        @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1)
        public float lastBreathTicks = 5;

        @Expose
        @ConfigOption(name = "Last Breath Sound", desc = "Sound played with RELEASE.")
        @ConfigEditorDropdown
        public ReleaseSound lastBreathSound = ReleaseSound.BELL;

        @Expose
        @ConfigOption(name = "Last Breath Volume", desc = "Volume of the RELEASE sound.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 1f, minStep = 0.05f)
        public float lastBreathVolume = 1f;
    }

    public enum ReleaseSound {
        BELL("Bell"), NOTE_BELL("Note Block Bell"), DING("Ding"), ORB("XP Orb"), NONE("None");

        private final String label;

        ReleaseSound(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class Score {
        @Expose
        @ConfigOption(name = "270 Score Alert", desc = "Show a title and play a sound when the run reaches 270 score (S).")
        @ConfigEditorBoolean
        public boolean alert270 = false;

        @Expose
        @ConfigOption(name = "300 Score Alert", desc = "Show a title and play a sound when the run reaches 300 score (S+).")
        @ConfigEditorBoolean
        public boolean alert300 = false;

        @Expose
        @ConfigOption(name = "Score Time Message", desc = "Like NoammAddons: a chat message when the run reaches 270 and 300 score, with how long it took and the floor (\"300 score reached in 6m 12s || M7.\"). Only you see it.")
        @ConfigEditorBoolean
        public boolean timeMessage = false;

        @Expose
        @ConfigOption(name = "Send 270 to Party", desc = "Also send \"[SB] 270 Score Reached!\" to party chat.")
        @ConfigEditorBoolean
        public boolean party270 = false;

        @Expose
        @ConfigOption(name = "Send 300 to Party", desc = "Also send \"[SB] 300 Score Reached!\" to party chat.")
        @ConfigEditorBoolean
        public boolean party300 = false;

        @Expose
        @ConfigOption(name = "270 Message", desc = "Text for the 270 title and party message. [score] is replaced with the score. Party messages start with [SB].")
        @ConfigEditorText
        public String message270 = "270 Score Reached!";

        @Expose
        @ConfigOption(name = "300 Message", desc = "Text for the 300 title and party message. [score] is replaced with the score. Party messages start with [SB].")
        @ConfigEditorText
        public String message300 = "300 Score Reached!";

        @Expose
        @ConfigOption(name = "Score Display", desc = "NoammAddons' score HUD: the estimated score, coloured red below 270, yellow below 300 and green at 300.")
        @ConfigEditorBoolean
        public boolean display = false;

        @Expose
        @ConfigOption(name = "Detailed Score Display", desc = "Also show secrets, crypts, deaths and mimic/prince under the score, like the info under NoammAddons' map.")
        @ConfigEditorBoolean
        public boolean detailed = false;

        @Expose
        @ConfigOption(name = "Force Paul", desc = "Count Paul's +10 bonus score even when the EZPZ perk isn't detected.")
        @ConfigEditorBoolean
        public boolean forcePaul = false;
    }

    public enum ClassOverride {
        AUTO("Auto"), ARCHER("Archer"), BERSERK("Berserk"), HEALER("Healer"), MAGE("Mage"), TANK("Tank");

        private final String label;

        ClassOverride(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum LeapCorner {
        TOP_LEFT("Top Left"), TOP_RIGHT("Top Right"), BOTTOM_LEFT("Bottom Left"), BOTTOM_RIGHT("Bottom Right");

        private final String label;

        LeapCorner(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Defaults follow Odin's leap menu: class colours and quadrants. */
    public static final class LeapMenu {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Replace the Spirit Leap / Infinileap menu with four large boxes. Click a box to leap to that teammate.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Top Left Key", desc = "Leap to the teammate in the top left box.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind(defaultKey = org.lwjgl.glfw.GLFW.GLFW_KEY_1)
        public int keyTopLeft = org.lwjgl.glfw.GLFW.GLFW_KEY_1;

        @Expose
        @ConfigOption(name = "Top Right Key", desc = "Leap to the teammate in the top right box.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind(defaultKey = org.lwjgl.glfw.GLFW.GLFW_KEY_2)
        public int keyTopRight = org.lwjgl.glfw.GLFW.GLFW_KEY_2;

        @Expose
        @ConfigOption(name = "Bottom Left Key", desc = "Leap to the teammate in the bottom left box.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind(defaultKey = org.lwjgl.glfw.GLFW.GLFW_KEY_3)
        public int keyBottomLeft = org.lwjgl.glfw.GLFW.GLFW_KEY_3;

        @Expose
        @ConfigOption(name = "Bottom Right Key", desc = "Leap to the teammate in the bottom right box.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind(defaultKey = org.lwjgl.glfw.GLFW.GLFW_KEY_4)
        public int keyBottomRight = org.lwjgl.glfw.GLFW.GLFW_KEY_4;

        @Expose
        @ConfigOption(name = "Colored Boxes", desc = "Fill each box with the class colour. Off: dark boxes with class-coloured names.")
        @ConfigEditorBoolean
        public boolean coloredBoxes = false;

        @Expose
        @ConfigOption(name = "Leap Announce", desc = "Send \"Leaped to <name>!\" to party chat after you leap.")
        @ConfigEditorBoolean
        public boolean announce = false;

        @Expose @ConfigOption(name = "Archer Colour", desc = "Archer box colour.") @ConfigEditorColour public String archerColor = "0:255:255:170:0";
        @Expose @ConfigOption(name = "Archer Position", desc = "Where the Archer goes. If two classes want the same corner, the extra one takes a free corner.") @ConfigEditorDropdown public LeapCorner archerCorner = LeapCorner.TOP_LEFT;
        @Expose @ConfigOption(name = "Berserk Colour", desc = "Berserk box colour.") @ConfigEditorColour public String berserkColor = "0:255:170:0:0";
        @Expose @ConfigOption(name = "Berserk Position", desc = "Where the Berserk goes.") @ConfigEditorDropdown public LeapCorner berserkCorner = LeapCorner.TOP_RIGHT;
        @Expose @ConfigOption(name = "Healer Colour", desc = "Healer box colour.") @ConfigEditorColour public String healerColor = "0:255:170:0:170";
        @Expose @ConfigOption(name = "Healer Position", desc = "Where the Healer goes.") @ConfigEditorDropdown public LeapCorner healerCorner = LeapCorner.BOTTOM_LEFT;
        @Expose @ConfigOption(name = "Mage Colour", desc = "Mage box colour.") @ConfigEditorColour public String mageColor = "0:255:85:170:255";
        @Expose @ConfigOption(name = "Mage Position", desc = "Where the Mage goes.") @ConfigEditorDropdown public LeapCorner mageCorner = LeapCorner.BOTTOM_RIGHT;
        @Expose @ConfigOption(name = "Tank Colour", desc = "Tank box colour.") @ConfigEditorColour public String tankColor = "0:255:0:170:0";
        @Expose @ConfigOption(name = "Tank Position", desc = "Where the Tank goes.") @ConfigEditorDropdown public LeapCorner tankCorner = LeapCorner.BOTTOM_RIGHT;
    }

    public static final class BloodCamp {
        @Expose
        @ConfigOption(name = "Move Prediction", desc = "Predict when the Watcher moves after its first spawns and show a Move Timer HUD.")
        @ConfigEditorBoolean
        public boolean movePrediction = false;

        @Expose
        @ConfigOption(name = "Move Message", desc = "Print \"Watcher will move in Xs.\" in chat.")
        @ConfigEditorBoolean
        public boolean moveMessage = false;

        @Expose
        @ConfigOption(name = "Party Move Message", desc = "Send \"Watcher will move in Xs.\" to party chat.")
        @ConfigEditorBoolean
        public boolean partyMoveMessage = false;

        @Expose
        @ConfigOption(name = "Kill Title", desc = "Show a \"Kill Mobs\" title when it is time to kill the first spawns.")
        @ConfigEditorBoolean
        public boolean killTitle = false;

        @Expose
        @ConfigOption(name = "Mob Kill Timers", desc = "Box where each blood mob will land, with a countdown until it spawns (green > 1.5s, gold, red, then aqua once spawned). Like Odin, only heads the Watcher throws are tracked, not the heads on the walls.")
        @ConfigEditorBoolean
        public boolean killTimers = false;

        @Expose
        @ConfigOption(name = "Spawn Colour", desc = "Box where the mob will land.")
        @ConfigEditorColour
        public String spawnColor = "0:255:255:85:85";

        @Expose
        @ConfigOption(name = "Final Colour", desc = "Box once the head has reached where the mob spawns.")
        @ConfigEditorColour
        public String finalColor = "0:255:0:170:170";

        @Expose
        @ConfigOption(name = "Position Colour", desc = "Box on the flying head (moved ahead by your ping).")
        @ConfigEditorColour
        public String positionColor = "0:255:85:255:85";

        @Expose
        @ConfigOption(name = "Box Size", desc = "Size of the boxes.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 1f, minStep = 0.1f)
        public float boxSize = 1f;

        @Expose
        @ConfigOption(name = "Line", desc = "Draw a line from the head to where it lands.")
        @ConfigEditorBoolean
        public boolean line = true;

        @Expose
        @ConfigOption(name = "Time Left", desc = "Show the time until the mob spawns.")
        @ConfigEditorBoolean
        public boolean timeLeft = true;

        @Expose
        @ConfigOption(name = "Offset (ms)", desc = "Shifts the countdown to match when mobs really spawn.")
        @ConfigEditorSlider(minValue = -100, maxValue = 100, minStep = 1)
        public int offset = 40;

        @Expose
        @ConfigOption(name = "Spawn Tick", desc = "Tick the mob is assumed to spawn on (mobs spawn 37-41 ticks after the throw).")
        @ConfigEditorSlider(minValue = 35, maxValue = 41, minStep = 1)
        public int tick = 38;

        @Expose
        @ConfigOption(name = "Interpolation", desc = "Smooth the landing box between ticks.")
        @ConfigEditorBoolean
        public boolean interpolation = true;

        @Expose
        @ConfigOption(name = "Ping Offset", desc = "Move the position box ahead by your ping.")
        @ConfigEditorBoolean
        public boolean pingOffset = true;

        @Expose
        @ConfigOption(name = "Watcher Bar", desc = "Show how many blood mobs are left in the Watcher's boss bar.")
        @ConfigEditorBoolean
        public boolean watcherBar = true;
    }

    public static final class PositionalMessages {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Send your positional messages to party chat when you reach them. Add them with /sb posmsg add here <radius> <delay ticks> <message>.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Only in Boss", desc = "Only send and show positional messages in a dungeon boss fight.")
        @ConfigEditorBoolean
        public boolean onlyInBoss = true;

        @Expose
        @ConfigOption(name = "Show Positions", desc = "Draw each positional message's circle or box and text in the world.")
        @ConfigEditorBoolean
        public boolean showPositions = true;

        @Expose
        @ConfigOption(name = "Ring Height", desc = "Height of the ring drawn around radius messages.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 5f, minStep = 0.1f)
        public float ringHeight = 0.2f;

        @Expose
        @ConfigOption(name = "Show Message", desc = "Show each message's text above its spot.")
        @ConfigEditorBoolean
        public boolean showMessage = true;

        @Expose
        @ConfigOption(name = "Message Size", desc = "Size of the text above each spot.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 4f, minStep = 0.1f)
        public float messageSize = 1f;

        @Expose
        @ConfigOption(name = "Built-in Waypoints", desc = "Show SkyBalls's hard-coded waypoints on F7 and M7: Py Stand Here (95, 165.5, 94.4), Mage Stop (34, 169, 65), Arch Stand Here (102-104, 168, 49), Healer Stand Here After Lighting (58, 169, 66), Tank Stand Here (109, 170, 93) and SS: the block at 109, 120, 93 is highlighted and standing at 108, 120, 93 sends \"At SS\" to party chat once each time you step on it.")
        @ConfigEditorBoolean
        public boolean builtInWaypoints = true;

        @Expose
        @ConfigOption(name = "Your Class", desc = "Which class's built-in waypoints to show. Auto reads it from the tab list; pick one if it isn't detected.")
        @ConfigEditorDropdown
        public ClassOverride classOverride = ClassOverride.AUTO;
    }

    public enum BoxStyle {
        OUTLINE("Outline"), FILLED("Filled"), BOTH("Outline and Fill");

        private final String label;

        BoxStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class PlatformHighlight {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Highlight the 3x3 platform on floor 7 once Goldor starts (after Storm).")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Healer Only", desc = "Only show it while you are Healer.")
        @ConfigEditorBoolean
        public boolean healerOnly = false;

        @Expose
        @ConfigOption(name = "Style", desc = "Outline, fill, or both.")
        @ConfigEditorDropdown
        public BoxStyle style = BoxStyle.OUTLINE;

        @Expose @ConfigOption(name = "Outline Colour", desc = "Colour of the outline.") @ConfigEditorColour public String outlineColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Fill Colour", desc = "Colour of the fill.") @ConfigEditorColour public String fillColor = "0:127:85:255:85";
    }

    public static final class WitherDragons {
        @Expose @ConfigOption(name = "Relic Highlight", desc = "While you hold a relic (hotbar slot 9), fill the cauldron it goes in with the relic's colour.") @ConfigEditorBoolean public boolean relicHighlight = false;
        @Expose @ConfigOption(name = "Spawn Alert", desc = "Title, sound and chat line when a dragon starts spawning. On the first double spawn it names your priority dragon, based on your class and the Power settings below.") @ConfigEditorBoolean public boolean alert = false;
        @Expose @ConfigOption(name = "Split Power", desc = "Power blessing level (Time counts as half) needed to split the first double spawn: Archer and Tank take one dragon, Berserk, Mage and Healer the other. 0 always splits; leave it at 0 for party finder teams.") @ConfigEditorSlider(minValue = 0, maxValue = 32, minStep = 0.1f) public float power = 0;
        @Expose @ConfigOption(name = "Easy Power", desc = "Power needed to split when one of the two dragons is Purple.") @ConfigEditorSlider(minValue = 0, maxValue = 32, minStep = 0.1f) public float powerEasy = 0;
        @Expose @ConfigOption(name = "Spawn Timer", desc = "Seconds until each spawning dragon appears, above its spawn point.") @ConfigEditorBoolean public boolean timer = false;
        @Expose @ConfigOption(name = "Kill Areas", desc = "Outline the kill area of every spawning or alive dragon.") @ConfigEditorBoolean public boolean boxes = false;
        @Expose @ConfigOption(name = "Dragon Hitboxes", desc = "Outline each part of every alive dragon in its colour.") @ConfigEditorBoolean public boolean hitboxes = false;
        @Expose @ConfigOption(name = "Tracer", desc = "Line to your priority spawning dragon.") @ConfigEditorBoolean public boolean tracers = false;
        @Expose @ConfigOption(name = "Stack Waypoints", desc = "Where to stack while a dragon spawns. Simple: one box on the spawn point. Advanced: boxes the shape of the dragon.") @ConfigEditorDropdown public DragonWaypoints waypoints = DragonWaypoints.OFF;
        @Expose @ConfigOption(name = "Dragon Health", desc = "Each alive dragon's health on it.") @ConfigEditorBoolean public boolean health = false;
        @Expose @ConfigOption(name = "Ice Spray Tracker", desc = "Chat line with how many ticks after spawning each dragon was Ice Sprayed.") @ConfigEditorBoolean public boolean trackIceSpray = false;
    }

    public enum DragonWaypoints {
        OFF("Off"), SIMPLE("Simple"), ADVANCED("Advanced");

        private final String label;

        DragonWaypoints(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class PartyCommands {
        @Expose
        @ConfigOption(name = "Enabled", desc = "When you are party leader, run commands that party members type in party chat.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose @ConfigOption(name = "!warp", desc = "Runs /party warp.") @ConfigEditorBoolean public boolean warp = true;
        @Expose @ConfigOption(name = "!allinvite", desc = "Runs /party settings allinvite.") @ConfigEditorBoolean public boolean allInvite = true;
        @Expose @ConfigOption(name = "!pt / !transfer", desc = "Transfers the party to the player who asked.") @ConfigEditorBoolean public boolean transfer = true;
        @Expose @ConfigOption(name = "!promote", desc = "Promotes the player who asked.") @ConfigEditorBoolean public boolean promote = false;
    }

    public static final class ItemNotification {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show items from your list on a HUD when you get them (in your sacks or your inventory), in the RNG HUD (with the farming RNG drops): amount, name and total price. Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @ConfigOption(name = "Items", desc = "Open the list of items to watch for: one per line, with item name suggestions as you type. Also /sb itemnotify.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton(buttonText = "EDIT")
        public Runnable editItems = com.epic60869.skyballs.features.misc.ItemNotification::openEditor;

        /** The items, one per line (edited in the Items window). */
        @Expose
        public String items = "";

        @Expose
        @ConfigOption(name = "Use Item Rarity Colour", desc = "Show each item's name in its own rarity colour on the RNG HUD. Turn off to use the Name Colour below.")
        @ConfigEditorBoolean
        public boolean rarityColour = true;

        @Expose
        @ConfigOption(name = "Name Colour", desc = "Colour of item names on the RNG HUD when Use Item Rarity Colour is off.")
        @ConfigEditorColour
        public String nameColour = "0:255:255:255:255";

        @Expose
        @ConfigOption(name = "Check Sacks", desc = "Watch the [Sacks] messages for items on the list.")
        @ConfigEditorBoolean
        public boolean checkSacks = true;

        @Expose
        @ConfigOption(name = "Check Inventory", desc = "Watch your inventory for items on the list.")
        @ConfigEditorBoolean
        public boolean checkInventory = true;

        @Expose
        @ConfigOption(name = "Show For (seconds)", desc = "How long an item stays on the HUD after you get it. Getting more of it keeps it there and adds to the amount.")
        @ConfigEditorSlider(minValue = 2, maxValue = 30, minStep = 1)
        public int seconds = 5;

        @Expose
        @ConfigOption(name = "Sound", desc = "Play a sound when an item on the list comes in.")
        @ConfigEditorBoolean
        public boolean sound = true;
    }
}
