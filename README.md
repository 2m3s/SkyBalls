# SkyBalls Mod

An all-in-one Fabric mod for Hypixel SkyBlock on Minecraft 26.2. Commands are not case-sensitive.

- `/sb` opens the settings; `/sb gui` moves and resizes every HUD.
- What changed in each version: [CHANGELOG.md](CHANGELOG.md).

## Features

- **General:** storage search, notes, calculator, craft helper, item customisation, command keys, item rarity backgrounds, item price tooltips, museum and accessory tooltips, SkyBlock calendar, nicknames, SJ chat and a current-chat display.
- **Combat and slayer:** arrow counter, zealot tracker, Legion display, cocoon alert, rare drops and slayer boss phases.
- **Skills:** farming RNG, Garden HUDs, commissions, Crystal Hollows map, Mineshaft timer, and fishing, foraging and runecrafting features.
- **Pets:** pet HUD with overflow levels from the tab list.
- **Dungeons:** map, puzzle solvers, secret waypoints, F7 terminals and devices, timers, starred mob boxes and Stella secret routes.
- **Experimental Table:** Chronomatron, Superpairs and Ultrasequencer solvers.
- **Party:** `!warp`, `!allinvite`, `!pt` and `!promote` for your party members while you are leader.

## Commands

`/sb` · `gui` · `search` · `notes` · `recipe <item> [amount]` · `calc <sum>` · `custom` · `keys` · `nick [name]` · `waypoints` · `crystalwaypoints` · `log` · `chat [message]` · `route start|stop|next|back|clear|list|cancel|reload|folder` · `export` · `debug`

`/sbc [message]` sends to SJ chat. `/chat sj` switches to SJ chat. `/skyballs` works the same as `/sb`. `/sb help` lists every command.

## SkyBalls Online

These talk to the SkyBalls server (`tastyfish.org/mod-api`). The mod logs in with your Minecraft account on every connection. If the server is down it keeps retrying in the background and shows "SBC offline" where it matters. Settings are under **SkyBalls Online** in `/sb`. The server can turn any of these off with a feature flag, and the mod then tells you why.

- **Chat:** click `[SB]` or `↩` to reply to a message (replies show "↪ Name: text" above them). Messages that mention you are highlighted and ping (colour, sound and volume are configurable). Tab after `@` completes online players' names. With chat open, hover a message for the reaction bar. `/sb react <id> <emoji>` also reacts.
- **Items:** `/sb share [message]`, the *Share Item* key (unbound by default; it works while hovering an item in a GUI), or `[item]` in a message. Shared items show as a hoverable `[Item Name]`; click one to see it.
- **Ignore:** `/sb ignore <player>` · `/sb ignore discord <name>` · `/sb ignore list` · `/sb unignore <player>`.
- **Players:** `/sb who` (a list screen, or chat if *Who List Screen* is off) · `/sb friend add|remove|accept|deny <player>` · `/sb friends` (list, requests and privacy settings: who sees your location, friend requests, online notifications). Your area, server and AFK status are shared unless *Share Your Status* is off.
- **Leaderboards:** `/sb leaderboard` lists the running event boards and the Slayer PBs board · `/sb leaderboard <board>` · `/sb leaderboard slayer [boss]` (everyone's fastest kill per slayer boss and tier; your personal bests are shared unless *Share Slayer PBs* under Slayers → Personal Best is off) · the *Slayer PB Leaderboard* HUD (Slayers → PB Leaderboard HUD) shows the board for your current slayer boss with a live timer against your PB and the #1 time.
- **Cosmetics:** badges and supporter symbols before names, and capes. Pick yours with `/sb cosmetics`. Nickname styles now include Gradient and Chroma.
- **Settings sync:** `/sb settings upload|download|list|delete [slot]`, or the buttons in *Settings Cloud Sync*. Downloading keeps your old settings in `skyballs-mod.json.bak`.
- **Server messages:** update notices (required updates show on every join and on the title screen), announcements and the message of the day.
- **Crash reports:** SkyBalls errors are sent to the SkyBalls team (stack trace, versions, OS, name and UUID). Turn off *Crash Reports* to stop this.

## SkyBlock helpers

- **Item Cooldowns** (Misc): ability cooldowns on the item's slot, read from its lore, plus an optional HUD.
- **Event Calendar** (Misc): `/sb calendar`, an optional HUD, and reminders before the events you choose (with minutes-before and a Jacob's crop filter).
- **Pest Highlight** (Farming, Garden only): outlines pests, with an optional line and beam to the nearest one and a pests/plots HUD.

## Credits

Ports code from Firmament (storage overlay), Skyblocker, CommandKeys, SkyOcean, SkyHanni, NopoMod, Stella, Odin and NoammAddons. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Built with Java 25 and Gradle 9.5+; GitHub Actions builds every push.

## License

SkyBalls is licensed under the GNU General Public License v3.0 or later (see [LICENSE](LICENSE)), as it includes code ported from Firmament.
