# Changelog

All notable changes to SkyBalls are listed here, newest first.

## test-1.3.7 — 2026-09-28

### Added
- Pet HUD shows your pet's held item (Pets → Pets Display → Show Held Item). The tab list doesn't show it, so it's learned from the Pets menu (open it once), "Your pet is now holding ..." and Autopet messages, and remembered per pet.
- Overflow pet level ups in chat again (NopoMod's): "[SB] Your Golden Dragon leveled up to level 158!" when your pet reaches the next overflow level. Needs Overflow Pet Levels on (Pets).
- Boss Profit (Slayers): after each slayer boss, "Profit: +50k" (or "-10k") in chat, and a Boss Profit HUD that shows the boss's profit when it dies and hides 2 seconds after its drops are in. Only that boss's drops count (SkyHanni's slayer drop lists), not mob drops or other items picked up meanwhile. Hover the chat line for each drop and its value, the drops' total, the quest's cost and the total. The HUD shows the moment the boss dies and fills in as the drops arrive (sack drops come with Hypixel's "[Sacks]" message); the drops are read from the items the boss shows on the ground, your inventory and your sacks, and the chat line goes out once they've landed (or the "[Sacks]" message with them comes). The cost is Hypixel's price for the boss and tier, or half of it with Aatrox's Slashed Pricing when your purse shows that.
- Sign Calculator (Misc), from Skyblocker: on SkyBlock's number signs (auction prices, bazaar amounts) the number you typed shows above the sign, e.g. `15m = 15,000,000`, and sums like `2.5m*3` are worked out and sent as the number.
- RNG Drop Totem Animation (Combat → Rare Drops), from Skyblocker: big RNG drops pop up like a Totem of Undying, with particles and a sound.
- Miniboss Alert (Slayers): a title and a ding when one of your slayer minibosses spawns (from Hypixel's "SLAYER MINI-BOSS ... has spawned!", so other players' minibosses don't count).
- RNG Meter Value (Slayers): in a slayer or dungeon RNG Meter menu, each drop shows its price and how many coins one Slayer XP (or Score) of meter progress is worth, e.g. a 10M drop needing 100k XP shows "1 XP = 100 coins". Above the menu, "Best Profit" names the drop worth the most per point.
- Slayer time messages (SkyHanni's): how long your boss took to kill, timed to the moment it dies (its health hits 0 or it's gone; a cocooned boss when it's cocooned), not to Hypixel's chat message, your personal best for that boss and tier on the same line, e.g. "It took 34.52s to kill Revenant Horror V (PB: 30.10s)" or "(NEW PERSONAL BEST! Previous: 36.00s)", and how long the whole quest took. Each can be turned off, and there's a compact style.
- The slayer boss phase display shows your Tarantula's egg sacs (time left and hits) under its health.
- Slayer kill time leaderboard: your slayer personal bests are shared with the SkyBalls leaderboard (Slayers → Personal Best → Share Slayer PBs, on by default; bests from before this update are sent the next time you connect). `/sb leaderboard slayer` shows every boss and tier with its record holder and your time and rank (click a row for its top 10), and `/sb leaderboard slayer <boss>` shows the top 10 for one boss, e.g. `Revenant Horror V`. Without a connection it shows your own times. The board is also in Discord (`/leaderboard slayer`).
- Slayer PB Leaderboard HUD (Slayers → PB Leaderboard HUD): the kill time standings for your current or last slayer boss (or one you pin), your place, and while your boss is alive a live timer against your PB and the #1 time. Shows during a slayer quest by default (or always, or on slayer islands), with 3 to 10 rows; move it in `/sb hud`. Offline it keeps the last standings it got.

### Changed
- New installs start with almost every feature off: turn on what you want in /sb. Options inside a feature keep their defaults, and SBC chat stays on. Existing settings don't change.
- /sb gui only shows the HUDs whose feature is turned on.
- Performance: the slayer boss scan only runs during a slayer quest and skips non-boss nametags early; starred mob highlighting checks every 4 ticks; SkyBlock item ids are cached instead of copying each item's data; the pickaxe ability reads the tab list every 4 ticks; settings are checked for changes every 5 seconds outside menus (and saved when the game closes); two chat regexes are built once.
- Museum tooltip: your donations are also read from Hypixel's museum API (through SBC) when you join and every 10 minutes, so you don't have to open the museum menus first.
- Auto Welcome's description warns USE AT OWN RISK.
- The jumpscare lasts 1 second.
- `/sb leaderboard` lists the running event boards plus a clickable Slayer PBs entry, and tab-completes slayer bosses. Event boards keep their names: one called "slayer" still opens that board.

### Fixed
- /storage: the mouse wheel scrolled a little, then stopped once an item came under the mouse. Block Scrolling on Items was still on for settings saved before 1.3.4; it is switched off once (you can turn it back on).
- The PB HUD's live timer froze while a Tier 5 Tarantula swapped phases. It now keeps running until the boss's health hits 0.
- Slayer PBs: a Tier 5 Tarantula was two bosses ("Tarantula Broodfather V", then "Conjoined Brood"), so the PB HUD switched to an empty board at the start of each fight. Both phases are now one boss, Tarantula Broodfather V, and older Conjoined Brood times move over to it.
- Screenshot upload: when the host failed (e.g. Litterbox's HTTP 500) its whole error web page was pasted into chat. It now tries once more, then says what went wrong in one line with a [Retry] button.
- Storage overview: smooth-scrolling mice and touchpads barely scrolled (each small wheel step was rounded to nothing).
- Storage overlay and overview: Retain Scroll Position was lost the first time you opened them after starting the game.

## 1.3.6 — 2026-09-28

### Removed
- The SS skip helper (start-click counter and limit) from the Simon Says solver.
- The accessory helper next to the Accessory Bag.
- The profile viewer (`/sb pv`), and the Profile buttons in `/sb who` and the friends list.
- The Hide Menu terminal option: the vanilla terminal menu is now always hidden while a solver shows (Melody too, unless its solver is off).

### Fixed
- Simon Says: the SS skip reordering could highlight the wrong buttons and block the right one; a quick correct click on the next button was blocked until the server showed the previous press. Every highlighted button now shows its number in the sequence.
- Terminals: the NoammAddons style looked the same as Odin's. It now draws like NoammAddons: only the terminal's own cells in a compact grid, with its title, padding and border.
- Cocoon alerts now fire for Special Zealots and other elusive mobs (and names with an apostrophe).
- Items shared in SkyBalls chat show their real icon when clicked (off Hypixel too) instead of paper or a barrier.
- Copy Rare Drops (and the big drop animation) now work for every rare drop line: VERY RARE, CRAZY RARE, INSANE, PRAY TO RNGESUS, PET and RNG METER drops, the Garden's RARE CROP lines, RARE REWARD, OUTSTANDING CATCH and Diana's "You dug out a ..." drops.
- The slayer boss phase display only shows your boss's health (or its hits), not nearby pets or damage numbers.

### Changed
- The accessory tooltip is now a 1:1 port of Skyblocker's: it also remembers recombobulated accessories, and shows nothing until your SkyBlock profile is known. As in Skyblocker, "↑ Upgradable" means you own that accessory and a higher tier of it exists.

## 1.3.5 — 2026-09-28

### Added
- Museum tooltip (from Skyblocker): item tooltips say whether the item is donated to your museum, and which museum category it's in. Look through your Museum's category menus once and it's remembered per profile.
- Accessory tooltip (from Skyblocker): accessory tooltips say whether you're missing it, already have it, or whether it's an upgrade or downgrade of the one you have from the same family, with the tiers, e.g. "✦ Upgrade (2→3/4)". Look through each page of your Accessory Bag once and it's remembered per profile.

- SkyBalls Online (new settings category): replies and a ↩ button in SkyBalls chat, highlighted mentions with a ping, @name completion, emoji reactions (hover a message with chat open), item sharing (`/sb share`, the Share Item key, `[item]`), and clear notices when a message is refused or you're muted.
- `/sb ignore`, `/sb unignore`, `/sb friend add|remove|accept|deny`, and `/sb friends` with requests and privacy settings. Popups when friends come online.
- `/sb who` is now a screen with nicknames, ranks, badges, friends, location, AFK, time online and mod version. Your area, server and AFK status are shared (you can turn this off).
- Casino: coinflip, dice, roulette, slots and higher/lower next to blackjack, plus the daily reward (with a reminder), stats and leaderboards (`/sb casino`, `/sb casino daily`).
- Profile viewer: `/sb pv [player] [profile]`.
- Gradient and Chroma nickname styles, badges and supporter symbols before names, capes, and `/sb cosmetics`.
- Update notices from the SkyBalls server (required ones on every join and on the title screen), announcements and the message of the day, feature flags, crash reports (can be turned off) and settings cloud sync (`/sb settings`).
- Item cooldowns on item slots, the event calendar with reminders (`/sb calendar`), the accessory helper next to the Accessory Bag, and pest highlight in the Garden.
- `/sb help` lists every command.

### Changed
- Copy Rare Drops shows what was copied in chat, using Copy Chat's preview settings.
- SkyBalls chat logs in on every connection and reconnects with a growing delay, saying "SBC offline" once instead of on every retry. Casino actions are queued to the server's limit of 5 a second.
- Performance: nicknames in nametags and the tab list are worked out once per name change instead of every frame, and HUD text is built once per tick instead of every frame.

### Removed
- The Slayer Tracker HUD.

### Fixed
- The slayer boss phase display never showed when you had a nickname set, because it looked for your nickname instead of your username on the boss's "Spawned by" line. It also no longer needs the sidebar to show your slayer quest, and keeps the colours of multi-line nametags.

## 1.3.4.5 — 2026-09-27

### Fixed
- "Took N bosses to drop" said 0 bosses the first time you got a drop. It now counts every boss since tracking started, and waits a second before counting so the kill that gave the drop is included (as in NopoMod). Drops in the Rift count for Vampire slayer. After you change worlds, a drop only counts if that slayer has given it before, until you kill another boss.
- Leaderboards and the casino wait for the server to confirm your Minecraft login before sending anything. A leaderboard drop the server refused because you weren't logged in is sent again once after logging back in, so it isn't lost.

## 1.3.4 — 2026-09-27

### Added
- Discord leaderboards: leaderboards run from the SkyBalls Discord (for example "most Summoning Eyes this week") count your drops and other chat events through the mod automatically. Enchanted book drops count by their enchantment (a Chimera leaderboard counts Chimera books), and a drop counts once even if Hypixel sends it twice. /sb leaderboard lists the running ones; /sb leaderboard <name> shows the standings.

### Changed
- The [SB] in front of SkyBalls messages is light purple instead of dark green.

### Fixed
- Hovering an image link in chat shows just the image, without the link's hover text on top.
- Storage overlay: the mouse wheel still didn't scroll over items whose tooltip is taller than the screen (Tooltip Scroll took it). In the storage overlay and /sb storageoverview the wheel now always scrolls the storage; Tooltip Scroll's keys still move long tooltips.

## 1.3.3 — 2026-09-27

### Added
- /sb search is now a port of SkyOcean's item search (MIT): category buttons (All, Storage, Island, Museum, Inventory), sorting by Amount, Price or Rarity (up or down), a $ button with the total value of what's listed, and copies of the same item shown as one entry with the total count. The tooltip lists where each copy is. Clicking an item opens its Ender Chest page or backpack, warps to your island and draws a rainbow box around its chest, or warps to the museum; the item then stays highlighted in menus for a minute (it used to flash for one frame).
- /sb search remembers the chests on your private island: open a chest and its items are saved (per profile, like your storage). Breaking the chest or turning it into a Minion Chest forgets it.
- /sb search remembers what's in your museum: browse the museum's category menus in the Hub and the items kept there are saved. Items shown as not donated or taken out are forgotten. Museum items don't count for the craft helper or the portfolio.
- Nicknames now live on shadowisabot.com (run /website in the SkyBalls Discord to get a login code) and show for your own name and everyone else's. Once your account is linked there, /sb nick sets it in game too: /sb nick <name>, /sb nick <colour> <name>, /sb nick rainbow <name>, /sb nick #hex <name> or /sb nick off. Without a linked account /sb nick doesn't show. /sb togglenick, the nickname editor and the Nickname settings are removed, and everyone's nickname always shows.
- /sb discord: the link to the SkyBalls Discord.
- /sb casino: blackjack with play money. Everyone starts with $100; bet, then Hit, Stand or Double (blackjack pays 3:2, the dealer stands on 17). If you go broke you get $100 again a day later. A leaderboard beside the table ranks everyone with the mod by most money. The SBC chat server holds the balances and deals the cards, and your Minecraft login is checked, so nobody can edit their money.
- Held Item Model is replaced by a port of Skysoft's Held Item (Misc > Held Item, or /sb helditem): an editor over the game where you drag the item to move it (right-drag for depth), scroll to resize it, and use sliders for position, scale, swing speed and rotation, for every item (Global) or just the one in your hand (This Item), with undo and redo. New: an Item Only swing style (the item swings, not your arm), switching an item to its vanilla texture instead of Hypixel's, and the Held Item Update Fix (on by default: the item doesn't dip when Hypixel updates it, and mining isn't interrupted). Your old settings and items saved with /sb helditem save are copied over; check them in the editor, since positions are measured differently. Swing X/Y/Z, Swing Rotation, No Swing Animation and No Re-equip Animation are gone.
- SBC chat rank announcements: when someone is given a rank prefix or has it removed, SBC chat shows "Steve Has Been Granted [VIP] By Console" (or "Steve's [VIP] Prefix Has Been Removed By Console") with the prefix in its exact colour. Hidden when SBC chat is hidden.

### Fixed
- Mouse Lock now works like Skyblocker's: it locks with every farming tool (all Theoretical Hoe tiers including Sunflower and Wild Rose, Fungi Cutter, Cactus Knife, Melon and Pumpkin Dicers, Coco Chopper, Gardening Hoes and Axes, Binghoe), goes by the SkyBlock id so renamed tools still lock, and pauses in the Garden's barn. Before, it never locked with Theoretical Hoes (it looked for the tool's id in the item's name) or renamed tools.
- Storage overlay: pages opened after you searched (like /ec 8) didn't show, and searching could hide Ender Chest pages until the search changed.
- Storage overlay: searching left the open page's items in the top left corner, over the other pages.
- Storage overlay: the mouse wheel didn't scroll while over an item. Block Scrolling on Items is now off by default, and Tooltip Scroll no longer takes the wheel from it.
- Storage overlay: the scroll bar keeps the knob under the mouse when clicked or dragged, and reaches the top and bottom.
- Tooltip Scroll took the mouse wheel whenever any tooltip showed (settings, lists, menus). It now only moves tooltips too big to fit on screen.
- /sb search did nothing off Hypixel, and could be closed again by the chat closing.

## 1.3.2.5 — 2026-09-27

### Added
- Hypixel item emojis in every chat (all, party, guild, private and SkyBalls chat; everyone with SkyBalls sees the icons), like the SkyHelper Discord: :summoning_eye:, :hyperion:, :enchanted_diamond: (any SkyBlock item id in lower case) show the item's real SkyBlock icon; hover it for the name. The icons are downloaded once from Coflnet and kept in config/skyballs/item-icons, so they don't need Hypixel's resource pack (without it the Summoning Eye is just paper). Chat > Hypixel Item Emojis.
- Emoji autocomplete shows each emoji's picture next to its name, includes the item emojis, and can be turned off (Chat > Emoji Autocomplete).

### Fixed
- Typing ":" in chat lagged: it listed every emoji. The list is now up to 50 matches (names starting with what you typed first).
- Storage overlay Dark Mode (Misc > Storage Overlay Settings): the backgrounds, slots and scroll bar are drawn dark, with a Dark Mode Shade to pick how dark.

## 1.3.2 — 2026-09-27

### Added
- /sb opens where you left it: the same category, scroll position and open sections, until you restart the game.
- RNG HUD colours: Farming > Farming RNG HUD has Drop Colour, Pet Rarity Colours and Price Colour; Misc > Item Notification has Use Item Rarity Colour and Name Colour.

### Changed
- Update notifications are checked every minute while you play (was every 10 minutes), so a new release shows in chat within a minute. Checks that find nothing new don't count toward GitHub's limit.
- Current Chat Display ("Chat: All") is drawn under the command and emoji suggestions instead of over them.
- Storage overlay is now Firmament's, ported from Firmament (GPL-3.0-or-later, textures CC-BY-4.0): one scrollable view of every Ender Chest page and backpack with your inventory, a search box (matching pages only, results highlighted) and "Edit Pages" for the normal Storage menu. The open page is the real menu, so clicking, dragging, shift-click and tooltips work as normal, and it stays open while Hypixel switches pages (no mouse jump). New settings under Misc > Storage Overlay Settings (columns, height, scroll, outline, highlight colours). New commands: /sb storage, /sb storageoverview, /sb storagename enderchest|backpack <page> [name]. Pages saved before still show.
- SkyBalls is now licensed GPL-3.0-or-later (needed for the Firmament code).
- Messages from Discord in SkyBalls chat start with the green [SB] like the rest, then a blue [Discord]: "[SB] [Discord] [name]: message".

### Fixed
- Item Notification stopped counting (and hid) with a personal compactor putting items into your sacks, with Check Sacks off: each new enchanted item that came and went within 10 seconds was taken for the same one. Only a quick swap (under half a second) is ignored now, once.
- Storage overlay: hovering an item didn't show its name or tooltip (the overlay drew after the game had already drawn this frame's tooltips).
- Collection Tracker went up by millions when you supercrafted (Enchanted Gold Blocks into your sacks were counted, the Gold Ingots taken out weren't). Each "[Sacks]" message is now counted per collection with what went in and what came out together.
- Collection Tracker counted items taken out of your sacks as gathered: they landed in your inventory and looked like a pickup. When the "[Sacks]" message shows them coming out, that pickup is taken back.

## 1.3.1 — 2026-09-27

### Fixed
- Item Notification (and the Collection Tracker) counted items after you opened and closed a menu such as your sacks: Hypixel resends your inventory a moment after a menu closes. They now wait a second after a menu closes or you change area, and items taken out of your sacks (the Sacks menu or /gfs) don't count.
- Putting items back into your sacks showed them as a drop (twice) and added them to the Collection Tracker again (a stack too many). Hypixel puts the same list on several parts of the "[Sacks]" message and it was read for each; each list is now read once, what the message takes out is subtracted, and items that just left your inventory aren't a drop.
- Storage overlay: moving an item could throw it on the ground (releasing the mouse reached the hidden menu, which saw a click outside it). Switching pages no longer jumps the mouse to the middle of the screen and back.

## 1.3.0 — 2026-09-27

### Added
- Hypixel Button (Misc, on by default): a Hypixel button on the title screen, next to Multiplayer, that joins play.hypixel.net in one click.
- Held Item Model > No Re-equip Animation (on by default): the held item doesn't dip and roll back up when Hypixel updates it or you switch items. This was the "roll across the screen" that happened sometimes even with Swing X, Y and Z at 0.

### Changed
- Built-in positional messages are class specific again (on F7/M7): Py Stand Here and Mage Stop for Mage, Arch for Archer, Healer and SS for Healer, Tank for Tank.
- Platform Highlight (3x3) shows whenever you're on F7/M7 (it waited for Goldor's chat line and often didn't show).
- Pickaxe Ability HUD and ready alert only show on the mining islands (Gold Mine, Deep Caverns, Dwarven Mines, Crystal Hollows, Mineshafts), not in the End or on the Crimson Isle.
- Update notifications are checked every 10 minutes while you play (not only when you join, at most every 3 hours), so a new release shows in chat without restarting.
- One RNG HUD: Item Notification items now show in the Farming RNG HUD (renamed RNG HUD) instead of a HUD of their own, and an item already shown as an RNG drop isn't listed twice.
- Collection Tracker: your inventory and sack messages are a live guess on top of the API number, and each API refresh only replaces what it covers, so the total no longer jumps back while the API catches up.
- Storage overlay: pages listed in the Storage menu that aren't saved yet show as boxes you can click to open, and the normal menu shows until there's something to put on the overlay.

### Fixed
- "At SS" was sent again every time you stepped back onto the spot; it's sent once per run.
- Simon Says solver didn't work: it waited for the floor and Skyblocker's boss-room detection; it now works anywhere in a dungeon (it only reacts to the Simon Says blocks).
- Copy Chat could copy a line a couple of rows above the one under the mouse; it now uses where chat really drew each line.
- Copy Chat's key set to right click (or another mouse button) did nothing.
- /sb search, the Storage Search key and saving storage pages did nothing when your server address had a port (play.hypixel.net:25565) or until Hypixel said your Profile ID again; the profile is now remembered and any Hypixel address works. Pages saved before still show.
- Collection Tracker counted too much: items picked up and then moved into your sacks were counted again from the "[Sacks]" message, and a compactor adding the enchanted item a tick before removing the 160 base items counted 160 each time. Gains now count once they've stayed for half a second, and a sack message only adds what didn't just come out of your inventory.
- Item Notification could show the same drop twice (Hypixel swapping the stack out and back, or the drop also going into your sacks).

## 1.2.9 — 2026-09-26

### Added
- Terminals > Hide Menu (on by default): while a terminal is being solved, only the solver shows. The chest, its items, your inventory and item tooltips are hidden, like other terminal solvers. The Odin style shows the terminal's name above the solver, and Melody keeps its lanes and buttons.
- Score Time Message (Dungeons > Score, on by default): like NoammAddons, a chat message when the run reaches 270 and 300 score with how long it took and the floor, e.g. "300 score reached in 6m 12s || M7." It no longer needs the 270/300 alerts on, and a run that jumps straight past 300 gets both messages.
- Summoning Eye drops show how many Zealot kills that eye took, on Hypixel's drop message: "RARE DROP! Summoning Eye (Kills: 100)". The Zealot that dropped it is counted too.
- Held Item Model > Swing Rotation: how far the item tilts and rolls when you swing (1 = vanilla, 0 = it doesn't turn). Swing X, Y and Z only move the item, so at 0/0/0 it stayed in place but still did the big forward roll; lower Swing Rotation to shrink that.

### Removed
- The NoammAddons dungeon map (and its Map Style, checkmark, name, scale and colour options). The dungeon map is always the Skyblocker map now.

### Changed
- Click in order! colours are now Numbers Next / Second / Third under Terminals (green, yellow and red by default).
- The 1 in 10,000 jumpscare can't happen in Kuudra runs either (it already couldn't in dungeons), or while SkyBalls hasn't read your location yet.
- Copy Chat has one setting, Copy Message Key: any key or mouse button (right click by default) copies the message under the mouse. Right-Click To Copy, Shift+right-click and Copy Line Key are gone.
- The "GOLD GOLD GOLD" sound is louder again.

### Fixed
- The dungeon floor still showed as "-" for some players. SkyBalls now also reads the sidebar straight from the scoreboard's teams (like NoammAddons), so it's found even when a scoreboard mod hides or replaces the sidebar, and doesn't take the Dungeon Hub's queue line for the floor. The backup that reads "[MVP++] Name entered The Catacombs, Floor VII!" now matches the message (it has lines of dashes around it) and is kept on the way into the run, and the Floor 7 bosses talking also set floor 7. This fixes F7/M7 positional messages, the Score Display, Platform Highlight and the device solvers when the sidebar can't be read.
- Held Item Model: Swing X/Y/Z and Swing Speed were skipped for players who also run another mod with swing animations (NoammAddons, Odin, ...), so the item still moved across the screen. SkyBalls now applies them last. If it still moves, turn off the other mod's animations.
- Arrow Align misclick protection: when more than one layout fitted, frames that were already right could still be clicked. Only the first layout that fits is used now.
- Nicknames above heads (yours and other SkyBalls users') kept the plain name with no colour or font. The nick is now put into the player name tag right where it's made, like NoammAddons does for its badges, and also into every entity's name tag, so it shows however the tag was made (vanilla or another mod). A nickname that contains the real username as a word is no longer replaced twice, and text before a skipped partial match (like "2m3sx") is no longer dropped from the name.
- Platform Highlight (3x3) didn't show: it needed the floor from the sidebar and could be switched off by a moment where the tab list area wasn't read. It now uses the F7/M7 detection from chat and the boss, turns on from any Goldor line (or Storm's death) in case the first was missed, and Healer Only uses the Your Class setting.
- Zealot Tracker sometimes counted one Summoning Eye twice, and pasting "RARE DROP! Summoning Eye" into chat counted as a drop. Only Hypixel's own drop line counts now: it has to start the message and "RARE DROP!" has to be bold like Hypixel writes it, which players can't do. The same line arriving twice at once counts once. The tracker also saves when the game closes.
- Copy Chat sometimes copied the message above the one you clicked. It now finds the line under the mouse with the same maths as vanilla chat.
- Quiz solver didn't highlight the answer when it couldn't find the room's corner block; it now uses the dungeon room matching to place the answers.

## 1.2.8.5 — 2026-09-26

### Changed
- Click in order! terminal: the next three numbers are green, yellow and red (they were three greens that were hard to tell apart), with the Odin and NoammAddons solvers. Settings still on the old greens are updated; Numbers 1-3 can still be changed.
- Dungeon Case Opening: the "GOLD GOLD GOLD" sound is much louder (about 4x), and plays as soon as the spin starts when it is going to land on a gold (Legendary or better) item, instead of when it stops.

### Fixed
- The dungeon floor wasn't recognised (`/sb debug` showed "Dungeon floor: -"), so F7/M7 positional messages (Py Stand Here, Mage Stop, SS, ...) didn't show, and anything else that needs the floor (Score Display, dungeon map, device solvers) could break. SkyBalls now finds the SkyBlock sidebar with any scoreboard mod, even one that puts its own sidebar in its place, reads the floor even when one of Hypixel's hidden padding characters sits inside "(F7)", and falls back to the "... entered The Catacombs, Floor VII!" message when a run starts. `/sb debug` also shows the sidebar's title and location line.
- Terminal Solver: terminals could open as the normal menu with no solver, because the solver waited for SkyBalls to know you were in a dungeon. It now starts for any terminal on Hypixel (their names only exist in F7/M7).
- Melody (Odin style): with the menu's items hidden, only the lit slots were drawn, so the board and buttons couldn't be seen. Like Odin's Melody GUI, every lane and button now has a background cell (Melody Background colour), with the column, pointer and button to press lit on top.
- Arrow Align Block Wrong Clicks still didn't block anything: the solver only ran once the dungeon floor was known. It now runs next to the Arrow Align board in any dungeon, and wrong clicks are stopped before they're sent, the same way as Odin.

## 1.2.8 — 2026-09-26

### Added
- Dungeon Case Opening at Croesus (Dungeons > Case Opening > Croesus, on by default): the chests you open at Croesus spin too, with that run's floor drops in the reel, like SkyOcean. Hide Contents In Croesus (on by default) hides what's in those chests in the run's menu so the spin isn't spoiled.

### Changed
- Zealot Tracker: in Total mode the kills line says "Total Kills:" (Session mode still says "Kills:"), and the lines are now kills, Since last eye, then Summoning Eyes.
- Storage Overlay: the top row of each page (the glass panes, Go Back arrow, Close barrier and page buttons) isn't shown any more, only your items.

### Fixed
- Dungeon Case Opening: the chest could still be seen through the spin, and its tooltips drawn over it; the chest is now hidden until the spin ends.
- Terminal Solver: the vanilla terminal showed through. With NoammAddons the real menu isn't drawn behind the panel any more; with Odin the terminal's own items and their tooltips are hidden and only the solution shows, like Odin.
- Arrow Align Block Wrong Clicks: each click on a frame was counted twice, so the last click a frame needed could be blocked as wrong. Clicks are counted once, and frames that are already right can't be clicked (hold shift to click anyway), like Odin.

## 1.2.7 — 2026-09-26

### Fixed
- With a scoreboard mod like CustomScoreboard, SkyBalls couldn't read the sidebar, so the dungeon floor and time were unknown: the Score Display stayed at 0, the NoammAddons map drew rooms at the wrong size and place, and F7/M7 positional messages didn't show. It now finds the SkyBlock sidebar either way.

## 1.2.6 — 2026-09-26

### Added
- Join commands (Misc > Join Commands): `/f0` (Entrance) to `/f7` and `/m1` to `/m7` join that Catacombs floor, and `/t1` to `/t5` join Kuudra (Basic, Hot, Burning, Fiery, Infernal).
- Held Item Model: Swing X, Swing Y and Swing Z. Set them to 0 and the item still rotates when you swing but doesn't move across the screen, like NoammAddons.
- Screenshot Sharing (Misc): after F2, the "Saved screenshot as ..." message has an [Upload] button. It uploads the picture to Litterbox (or Catbox) and gives you the link with [Send in /sbc] and [Copy Link]; the link previews when hovered.

### Changed
- Positional messages: the built-in spots (Py Stand Here, Mage Stop, Arch/Healer/Tank Stand Here, SS) show on F7 and M7 while Built-in Waypoints is on, instead of only for your class in the right boss phase.
- Terminals and Devices: the Odin Terminal Solver switch is now a Terminal Solver dropdown, like SkyHanni's: Odin, NoammAddons or Skyblocker Highlights. NoammAddons shows its big centred panel with the terminal's name, a Scale setting, three slot styles (Rect, Bordered Rect, Button) and its colours; you click on the panel, and rubix picks the right mouse button for you.

### Fixed
- Dungeon Case Opening showed almost only enchanted books; the reel now mixes in the floor's drops (Necron's Handle, Giant's Sword, Shadow Fury, master stars, ...) with the chest's real items, like SkyOcean.
- Teleport maze solver could send you round in a loop back to the pad you came from; the pad you leave now counts as used, like Odin's solver.
- The game could crash when entering a dungeon (the dungeon map crashed while logging a player it couldn't match). The same hidden problem in waypoint groups is fixed too.
- Storage Overlay: opening /ec showed the real Ender Chest page underneath the overlay; now only the overlay shows.
- Messages from the SkyBalls chat bot showed as "[SkyJew]: [SJ] ..."; they show as SkyBalls and [SB].
- Hovering an ImgBB link in chat (like Skysoft's "Screenshot uploaded: https://ibb.co/...") showed no preview. Links to image pages on ImgBB, Imgur, Gyazo, Lightshot and Postimages now preview the picture on the page.
- Update notifications check the renamed SkyBalls GitHub page (they stopped arriving after the rename).

## 1.2.5 — 2026-09-26

### Changed
- SkyJew is now **SkyBalls**: `/sb`, `/skyballs`, `/sbc` for SkyBalls chat, and [SB] in chat (the old `/sj` commands are gone). The [SB] in SkyBalls chat is dark green, like Hypixel's "Guild >". Your settings carry over automatically. **Delete the old SkyJew jar** when you install this one, or both will load.
- Ranks ([OWNER], [TESTER], ...) only show in SkyBalls chat, and Chat > Custom Chat > Show Ranks turns them off completely. Rank changes made on the website show up right away when the server announces them.
- The Collection Tracker is just two lines now: "Collection: 12,345,678" (with the item's icon), and the next player above you on the Elite leaderboard with how far ahead of you they are.
- The Pickaxe Ability HUD reads Hypixel's Pickaxe Ability tab list widget, like SkyHanni, instead of guessing from chat. Turn that widget on in Hypixel's tab list settings.
- The Enchanting Runes nickname font is made of letters now (the Unicode enchanting table alphabet), so it shows in every tab list, including other mods'.
- Leap menu: press 1-4 (changeable) to leap to the teammate in that box; each box shows its key.
- Chat image previews work with Discord, Imgur and Gyazo links, show "Loading image..." while downloading, and sit above the link's tooltip.

### Added
- `/sb bugreport`, `/sb suggest` and `/sb feedback`: send a bug report, suggestion or feedback (a title and a description) to the SkyBalls team.
- Dungeon Case Opening (Dungeons > Case Opening, off by default): Obsidian and Bedrock chests open like a CS2 case (SkyOcean's Dungeon Gambling) and stop on the best item. Legendary or better plays "GOLD GOLD GOLD".
- Item Notification (Misc > Item Notification): SkyOcean's Sack Notification as a HUD. When an item on your list goes into your sacks or inventory it shows like the farming RNG HUD ("5x Enchanted Diamond 8.5k"). The list is edited in its own window with item name suggestions (EDIT, or `/sb itemnotify`).
- Storage Overlay (Misc, on by default): every Ender Chest page and backpack at once in /storage and in any page, like Firmament. Click a page's name to open it; the open page and your inventory work as normal.
- Slot Locking & Binding (Misc): press L over a slot to lock it (it can't be clicked, moved or dropped), and B to bind a hotbar slot to an inventory slot so a shift-click swaps them (Odin's Slot Binds).
- Copy Chat (Chat > Copy Chat): right-click a chat message to copy it, Shift+right-click for one line, with a preview (NoFrills' Chat Tweaks). Rank prefixes aren't copied.
- `/sb toggle <setting>` turns any setting on or off, `/sb togglenick <player>` hides or shows someone's nickname for you, `/sb who` lists who is online with the mod, and `/sb disableall` (or `/sbdisableall`) turns everything off after you click to confirm.
- No Swing Animation (Misc > Held Item Model, off by default), like NoFrills.
- SB Chat Ping (Chat > Custom Chat, off by default): a ping when someone sends a message in /sbc.
- Farming RNG HUD: Epic and Legendary slug pets from pests, shown in their rarity colour ("1x Legendary Slug Pet") at a set price of 500k (Epic) and 5m (Legendary).

### Fixed
- `/sb nick`: picking a preset colour also updates the colour picker and hex box.
- Calendar dates are also added from the menu's own tooltip, so another mod's tooltip code can't stop them showing.
- Mouse Reset puts the cursor in the middle of the screen (it went near the top-left at GUI scales above 1).
- `/sb log` said the changelog wasn't available; it shows it again.
- No Swing Animation didn't do anything; it works now.

## 1.2.4 — 2026-09-26

### Added
- Update notifications: when a newer SkyJew is out you get "New SkyJew Mod Version 1.2.4 --> 1.2.6" in chat (always the newest one), with a Download link (Misc > Update Notifications).
- Auto Welcome (Misc > Auto Welcome): put players on your list and SkyJew welcomes them when they come online, in guild chat or with /msg, with your own message ({name} is their name). It can also welcome new guild members. Manage the list with `/sj welcome add|remove <name>` and `/sj welcome list`.

### Changed
- SkyJew's chat messages start with [SJ] instead of [SkyJew].
- SkyJew ranks ([OWNER], [TESTER] and the ones set on tastyfish.org) now show in front of those players' names in every chat, not only /sjc (Chat > Custom Chat > Ranks In All Chat).
- The Collection Tracker HUD copies SkyHanni's Crop Milestones display, for any collection: "Collection Milestones", the item's icon with your collection tier ("Cobblestone 11➜12"), your progress in that tier ("12,345/20,000"), the time to the next tier, Items/Hour and the percentage, then your Elite rank and how much you need to pass the next player. `/sj trackcollection <item> [goal]` pins one collection with an optional goal (e.g. `/sj trackcollection wheat 10m`), like SkyHanni's /shtrackcollection; `/sj trackcollection` follows what you gather again and `/sj trackcollection stop` hides it.
- Ranked players can use their own name as a nickname.

### Fixed
- `/sj nick` Font button: the ◀ arrow went forward too; it now goes back.
- The Enchanting Runes and Illager Runes nickname fonts showed broken characters for anything they don't have (numbers and symbols in the enchanting one); those now use the normal font.
- Rune prices: the lowest BIN list only has runes someone is selling right now, so most rune levels had no price. The price tooltip and Portfolio now fall back to the 3 day average and then to the last price seen (with how long ago), and Portfolio knows the names of runes that are in the 3 day average too.
- Nicknames (with their font) now show in tab lists drawn by other mods (SkyHanni, Skyblocker) too.
- The `/sj nick` menu couldn't be closed: Esc and Done sent you back to the chat box, or did nothing when the name wasn't allowed. Esc now always closes (a name that isn't allowed just isn't saved) and Done closes once the name is allowed.

## 1.2.3 — 2026-09-26

### Changed
- Dungeon map: new NoammAddons (Legit) style, now the default. Rooms are redrawn in clean colours with checkmarks, room names or secret counts, bordered player heads with optional names (Holding Leap / Always), and optional extra info under the map. Every colour and size can be changed. The old look is still there as Map Style: Skyblocker.
- Score Display now uses NoammAddons' score calculation, read straight from the tab list and sidebar, so it no longer depends on the dungeon start being detected. Optional Detailed mode and Force Paul. The 270/300 alerts use it too.
- Terminals: Odin's terminal solvers for all six terminals, with misclick blocking (clicks on wrong slots are ignored), first click protection and client prediction. Skyblocker's highlights are still available with Odin Terminal Solver off.
- Devices: Odin's Simon Says, Arrow Align and Sharp Shooter (i4) solvers. Simon Says and Arrow Align block wrong clicks (hold Shift to click anyway).
- Blood Camp now follows Odin: a head is only tracked once the Watcher throws it, so the heads hanging on the walls no longer throw off the landing boxes. Adds a position box moved ahead by your ping, box colours and size, offset, spawn tick, interpolation and the Watcher bar (blood mobs left in the boss bar).
- Goldor tick timer counts 50 ticks by default (adjustable).
- `/sj nick` has a new screen laid out like `/sj custom`: name and toggles, colour swatches with Rainbow and a custom colour picker, and live TAB / chat / nametag previews.
- `/sj recipe` only uses the Recipe HUD now; the panel next to the inventory is gone.
- HUDs keep their place relative to the screen when you change GUI scale or window size, instead of piling up at an edge. Storage search fits on the screen at high GUI scale.
- All boxes, lines and text in the world are drawn every frame, so they follow moving mobs and your camera smoothly.
- Last Breath RELEASE plays a bell by default (Bell, Note Block Bell, Ding, XP Orb or None) at full volume.
- Compact chat stacks repeats onto the newest message at the bottom instead of the old one higher up.
- Price tooltips: items sold on the bazaar show the bazaar insta-buy and insta-sell price where the lowest BIN and 3 day average would be (for the whole sack in the Sacks menu).
- The settings title shows the installed version (e.g. SkyJew Mod v1.2.3).
- `/sj log` opens a changelog window instead of printing in chat: arrows (or the left and right keys) switch versions, and the list scrolls.
- Launchers show the mod as "SkyJew by 2m3s", with 2m3s's head as its icon.

### Added
- Dungeon routes folder: put any route file in `config/skyjew/dungeon route/` (Stella / SkyJew, SecretRoutes or Dungeon Rooms Mod style). Rooms without a route there use Stella's routes. Changes load automatically; `/sj route reload` and `/sj route folder` too.
- Water Board: Skyblocker's water path and lever previews, next to Odin's solver.
- SS skip helper: counts your Simon Says start button clicks above the button and blocks clicks past the limit (default 4).
- Starred mob highlight colour, fill and line width.
- Scrollable tooltips: scroll to move long tooltips (Shift for sideways).
- Toggle Sprint, with a key in Controls and a small HUD.
- "Your Class" option for the built-in positional waypoints, if your class isn't detected.
- `/sj waypoints`: Skyblocker's waypoint editor, with groups per island, ordered waypoints (`/sj waypoints ordered next|previous|reset`) and Skyblocker / Skytils / Coleweight import and export.
- `/sj log`: the changes in the version you have installed, in game.
- Crystal Hollows waypoints, like Skyblocker: places you find are marked (Mines of Divan, Jungle Temple, Goblin Queen's Den, ...), coordinates in chat become waypoints, and `/sj crystalwaypoints add|share|remove|clear`.
- Pickaxe ability HUD: cooldown of Mining Speed Boost, Pickobulus and the other abilities, with a ready alert.
- Warp shortcuts: `/dhub` instead of `/warp dhub`, and the same for every warp in Misc > Warp Shortcut List.
- Price Paid tooltip, like NoFrills: items you buy on the auction house show what you paid for them.
- Collection tracker, like SkyHanni's farming display but for every collection: while you mine, farm, forage or fish it shows your collection, what you've gained this session (and per hour) and your rank on the Elite collection leaderboard, with how much you need to pass the next player. Enchanted items from compactors count as the items they're made of (Enchanted Cobblestone = 160 Cobblestone).
- /sj nick fonts: a Font button in the nickname menu with Bold, Italic, Small Caps, Full Width, Bubble, Script, Fraktur, Double Struck, Monospace, Sans Bold, Minecraft's enchanting and illager runes and Uniform. The nickname filter also sees through look-alike letters now.
- Custom Chat > Show SJ Chat: turn off to hide other players' `/sjc` messages.
- SJ chat rank prefixes, tied to the accounts themselves and managed from the tastyfish.org admin page (the mod checks for changes every 5 minutes). [OWNER] and [TESTER] are built in. Nicknames that copy a ranked player's name or a rank are not allowed.
- Portfolio: auction house and bazaar prices have their own buttons (AH: Lowest BIN / 3-day avg, BZ: Sell / Buy price), so all four combinations work.
- Portfolio: new rows get today's lowest BIN (or bazaar buy price) as their buy price; you can still change it.
- Portfolio: click any column header to sort by it, click again to reverse; the sorted column shows an arrow.
- Portfolio: item icons next to names, columns sized to fit so Qty and Buy each no longer overlap, sales can be removed from the Sold list, and the summary shows profit per hour (each row's profit divided by the hours since you added it).

### Fixed
- Portfolio: runes (e.g. Barkshatter Rune III) showed "?" as their price. Runes, pets and enchanted books are now counted and priced by their market ID, and rune names can be typed in.
- Water Board: the line to the next lever jumped around while moving. Only Odin's solver draws it now (Skyblocker's solver drew a second line), and it starts from the camera.
- Built-in positional waypoints (Py Stand Here, Mage Stop, ...) didn't show: your class is now read from any tab line with your name and remembered for the run, even as a ghost.
- Sharp Shooter (i4) solver didn't work.
- Pet display was blank when the server address had a port or wasn't exactly hypixel.net, and when Hypixel put icons around "[Lvl N]".
- Calendar real-time dates didn't show: calendar menus and dates written in tooltips are matched more loosely.
- Slayer tracker and boss phase HUDs didn't work: Hypixel's padding emoji in the sidebar broke the matching, and boss nametags are now text displays.
- Last Breath, Ice Spray, masks, arrows, farming tools and other items renamed with `/sj custom` are still recognised.
- The 1/10,000 jumpscare no longer happens in dungeons.
- Storm pad tick timer was one tick short; it now counts down to 0 on the pad tick like Odin (the Goldor timer too).
- Superpairs solver didn't show the cards you had revealed; they now stay visible on their slots.
- Scrollable tooltips are now Skysoft's Tooltip Scroll: smooth panning, WASD / Page Up / Page Down keys, a reset key, speed and smoothness settings (Misc > Tooltip Scroll). It steps aside when Skysoft itself is installed.

### Removed
- Farming Profit Tracker (the Skysoft port).

## 1.2.2 — 2026-09-25

### Added
- Leap Menu (Dungeons): an Odin-style Spirit Leap menu with one box per teammate. Each class has its own colour (Archer orange, Mage blue, Berserk red, Healer purple, Tank green) and corner, and both can be changed.
- Blood Camp (Dungeons): Watcher move prediction with a Move Timer HUD and a "Kill Mobs" title, and kill timers that box where each blood mob lands with a countdown until it spawns.
- Door and key highlight: wither and blood doors are outlined green when your team has the key and red when locked; dropped keys are outlined and announced.
- Score (Dungeons): 270 and 300 score titles, sounds and chat messages, a separate party-chat toggle and editable message for each (sent as "[SJ] ..."), and a Score Display HUD with score, secrets, crypts, deaths and mimic/prince.
- Positional messages: `/sj posmsg add here|at|in ...` sends a party message when you reach a spot, like Odin. Built-in waypoints include "Py Stand Here" at 95, 165.5, 94.4 during Storm on floor 7 (Mage only), "Mage Stop" at 34, 169, 65 during Storm (Mage only), "Arch Stand Here" at 102-104, 168, 49 during Storm (Archer only), "Tank Stand Here" at 109, 170, 93 during Storm (Tank only), "Healer Stand Here After Lighting" at 58, 169, 66 during Storm (Healer only), and "SS" during Goldor until Necron (Healer only): the block at 109, 120, 93 is highlighted and standing at 108, 120, 93 sends "At SS" to party chat once. Radius messages draw an Odin-style ring.
- `/sj route import`: imports routes from your clipboard or a file (Stella / SkyJew format, or SecretRoutes files).
- Last Breath release cue: a sound and RELEASE title once you have charged Last Breath for 5 server ticks (adjustable).
- Misc > Random: Low Fire (lowers the burning overlay) and Hide Explosions.
- Platform Highlight (3x3) (Dungeons): one box over the floor 7 3x3 platform (x 53-55, z 113-115) from when Goldor starts, like NoFrills, with Healer Only, style and colour options.
- Pristine Record (Mining): remembers your highest pristine proc, overall and per gemstone, and alerts on a new PB. `/sj pristine` lists them.
- Portfolio (`/sj portfolio`): track items you own (e.g. pet skins) with live prices. Quantities are counted from your inventory and every Ender Chest / backpack page you have opened, plus an extra amount you can type. Set a buy price to see profit/loss. Auction and bazaar sales of tracked items can be logged (asking first or automatically), value history is saved every 30 minutes with an in-game graph, and Export CSV writes portfolio.csv, history.csv and sold.csv for Excel.
- `/sj recipe` now works like SkyOcean's craft helper: the panel next to your inventory has -/+ buttons for the amount (Shift 10, Ctrl 64), and a movable Recipe HUD shows the item and the base ingredients you still need while you play (Misc > Recipe HUD).
- Misc > Player Size: make yourself, other players, or both smaller or bigger (client side), like Odin.
- Misc > Held Item Model: move, rotate and scale the item in your hand and change swing speed, like Skysoft; `/sj helditem save` stores settings for one item.

### Changed
- Splits now work like Odin's: Blood Open, Blood Clear, Portal Entry, each boss phase and Total, with Boss Entry, tick times, per-floor personal bests and a "took" message after each split.
- Mask timers now follow Odin: Spirit, Bonzo and Phoenix always show invincibility time, cooldown or ready, counted in server ticks, with your Bonzo cooldown read from the mask. Optional party announce.
- Nicknames now also replace your name in Hypixel's name lines above heads, not just the vanilla nametag.
- Starred mob boxes no longer show through walls.
- The puzzle solvers now use Odin's: Ice Fill (with optional shorter paths), Boulder, Creeper Beams, Three Weirdos, Quiz, Teleport Maze, Water Board (with optional faster solutions) and Blaze. Tic Tac Toe and Silverfish keep their solvers.

### Fixed
- SkyJew now reads chat straight from the server, so it still sees [BOSS] and [NPC] lines when another mod hides them. This was why Simon Says, splits, boss tick timers, the dungeon score and the map did not start.
- Simon Says also turns on near the device on floor 7 if the Maxor message was missed.
- Terminal solver highlights are drawn with the slots, so mods that hide tooltips in terminals no longer hide them.
- The dungeon map now updates whenever Hypixel sends a map update, not only when a room is identified.
- The dungeon floor is read correctly from the sidebar ("Floor pattern doesn't match").
- Beacon beams from mods built for Minecraft 26.1 (such as Odin's quiz and terminal beams) no longer show the missing texture.
- The "Kills Since Rare Drop" setting was not saved.
- Storage search (`/sj search`) kept one storage for every profile, so your ironman showed your main profile's items. Each SkyBlock profile now has its own storage (reopen your Ender Chest and backpacks once per profile).

## 1.2.1 — 2026-09-25

### Fixed
- The Zealot Tracker could count zealots other players killed right next to you. A kill now only counts when you also get the Combat XP for it.
- The Item Price Tooltip didn't show NPC sell prices. The lines are now in the order NPC Sell Price, Lowest BIN Price, 3 Day Avg. Price, and the settings are in the same order.

## 1.2 — 2026-09-25

### Added
- Commands are no longer case-sensitive: `/SJ GUI` works the same as `/sj gui`.
- Item Price Tooltip (Misc): 3 day average price, lowest BIN and NPC sell price on SkyBlock items, like Skyblocker.
- Current chat display: shows which chat you're typing in (All, Party, Guild, Officer, Co-op, a private conversation or SkyJew chat) above the chat box, like SkyHanni.
- Ultrasequencer order numbers: every slot shows its place in the order, with the next one in yellow.

### Changed
- The zealot counter is now a Zealot Tracker in the style of SkyHanni's trackers: kills, Summoning Eyes and kills since your last eye. Click Total / This Session while your inventory is open to switch. Only your own kills count, including Hyperion multi-kills.
- HUD elements no longer have a background by default. Right-click any HUD in `/sj gui` to turn its background on.
- In every settings section, dropdowns are now listed above the single toggles.
- The Enchanting tab has moved into Misc as the Experimental Table section. Your settings carry over.
- Settings now save as soon as you change them, not only when the settings screen closes.
- The "no custom skin" barrier is now the first slot in the helmet skin picker.
- In the helmet skin picker, animated heads only animate when you hover or select them.
- The README is much shorter.

### Removed
- The Alchemy tab and the Alchemy 50 Estimate HUD.
- The Hunting tab (Safari Critter Tracker and Hunting Box Value), for now.

### Fixed
- Animated helmet skins (Celestial Necron/Storm/Goldor, Golden Dragon Swap Plushies and every other animated head) couldn't be applied: selecting one cleared the skin instead. They now apply, wait for each frame to load instead of flashing Steve, and Cancel restores them properly. Skins that fail to load are retried.
- The zealot counter counted other players' kills.
- The item rarity style went back to Square after restarting the game.
- The Chronomatron solver didn't highlight anything.
- Calendar real-time dates didn't show in the calendar menus.

## 1.1 — 2026-09-25

### Added

**General**
- `/sj gui` can now move and resize every HUD element, not just a few. Elements snap to the screen edges and can't be dragged off screen.
- Right-click any HUD in `/sj gui` to turn its background on or off.
- `/sj keys` now uses a full port of the CommandKeys mod (keybinds, macros and profiles). Your old command keys are migrated automatically.
- `/sj custom` has been rebuilt as a port of Skyblocker's item customisation: custom names, dyes (including animated dyes), armor trims and helmet textures.
- `/sj recipe <item> [amount]` shows a SkyOcean-style craft helper. It shows the full recipe tree and what you still need, counting items in your storage. It also has `/sj recipe amount <n>` and `/sj recipe clear`.
- Item Rarity is now a dropdown with style and opacity options. It works in your inventory, in containers and on the hotbar.
- SkyBlock calendar time and a date calculator, ported from Skyblocker.
- Nicknames now show above players' heads and in the tab list for everyone using SkyJew. Hovering a nickname shows the player's real name. Offensive nicknames are blocked.
- SJ chat now shows each player's SkyBlock level in its level colour. Hovering a name shows the real name, and clicking starts a `/msg`.
- `/sjc <message>` sends a message to SJ chat. `/chat sj` switches your chat to SJ chat, and `/chat <anything else>` switches back.

**Pets**
- The pet HUD now shows overflow levels automatically from the tab list, like NopoMod. You no longer need to open the Pets menu. It also shows progress to the next level.
- Pet display background option.

**Combat**
- Arrow counter: your selected arrow and how many are left in your quiver.
- Zealot counter: zealot kills, kills since your last Summoning Eye and eyes dropped.
- Legion display: how many players are in Legion range.
- Cocoon alert for slayer bosses, slayer minibosses, elusive mobs and important bosses.
- Rare drops: copy drop messages and show an animation for valuable drops.

**Slayer**
- Slayer tracker: progress to spawning the boss, XP to your next level and session totals.
- Boss phase display showing your boss's nametag lines (Voidgloom hits, Inferno attunement and so on).

**Garden**
- Yaw, pitch and facing direction HUD (Garden only).
- Pest cooldown, blocks-per-second and special drop animation.
- Mouse lock can be set to only work in the Garden.

**Other skills**
- Fishing, mining (Crystal Hollows map, Mineshaft timer), foraging, alchemy, enchanting, runecrafting and hunting features.

**Dungeons** (ported from Skyblocker)
- Dungeon map with player heads and room names.
- Puzzle solvers: Tic Tac Toe, Three Weirdos, Creeper Beams, Water Board, Blaze, Boulder, Ice Fill, Silverfish, Trivia and Teleport Maze.
- Secret waypoints.
- Floor 7 terminal and device solvers.
- Splits, tick timers, mask timers and an M7 debuff alert.
- Highlight starred mobs: draws a box around ✯ mobs, visible through walls.
- Secret routes in Stella's format. Rooms without your own route use Stella's routes.
  - `/sj route start` records a route automatically: secrets, levers, etherwarps, superbooms, pearls and mined blocks. `/sj route stop` saves it.
  - `/sj route next` / `back` step through a route, and `/sj route clear` deletes your route for the current room.
  - All your routes are saved in one file. `/sj export` writes them to `routes-export.json` and copies them to your clipboard.

**Party**
- Party commands.

### Changed
- The notes screen (`/sj notes`) has been rebuilt with a proper multi-line editor and autosave.
- The cocoon alert no longer has a mob list text box. It alerts for important mobs only.
- The commission HUD now looks like Skyblocker's. Its background toggle is now saved.

### Removed
- The "you have been ratted" start-up screen and its sound.
- The slayer profit tracker, for now.

### Fixed
- HUDs saved off screen (such as the zealot counter) now appear on screen.
- The zealot counter never counted kills.
- The arrow counter didn't detect your arrows.
- Pet level was shown twice in the pet HUD.
- Item rarity backgrounds didn't show in the player inventory.
- Calendar time was wrong.
- `/sj search` still showed the first page's items on later pages.
- Image link previews in SJ chat stopped showing on hover.
- Other players' nicknames in the tab list.
- A crash on start-up when items were created too early.
- A crash from nicknames being applied repeatedly in the tab list.

## 1 — 2026-09-25

First release.

- Farming RNG tracker and HUD.
- Storage search (`/sj search`).
- Custom item tools (`/sj custom`).
- Command keys (`/sj keys`).
- Notes (`/sj notes`).
- Main `/sj` GUI.
- Version checker.
