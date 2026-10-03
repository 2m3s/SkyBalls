package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsChat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lets party members run !warp, !allinvite, !pt, !promote and Odin's !f1-!f7, !m1-!m7 and !t1-!t5 while you are
 * party leader, and answers !fps, !ping and !tps (Odin's ChatCommands, https://github.com/odtheking/Odin, BSD-3-Clause)
 * whoever is leader. Leadership is tracked from Hypixel's party messages. SBO's party commands
 * (https://github.com/SkyblockOverhaul/SBO, Apache-2.0) add !demote, !carrot, !time and the Diana ones (!chim, !since,
 * !profit, ...; see {@link com.epic60869.skyballs.features.combat.DianaPartyCommands}).
 */
public final class PartyCommands {
    private static final Pattern PARTY_CHAT = Pattern.compile("^Party > (?:\\[[^]]+] )?(?<name>\\w+)[^:]*: !(?<command>\\w+)(?: +(?<arg>\\S+))?(?<more> +\\S.*?)?\\s*$");
    private static final Pattern INVITED = Pattern.compile("^(?:\\[[^]]+] )?(?<me>\\w+) invited (?:\\[[^]]+] )?\\w+ to the party!");
    private static final Pattern TRANSFERRED = Pattern.compile("^The party was transferred to (?:\\[[^]]+] )?(?<name>\\w+)");
    private static final Pattern JOINED_OTHER = Pattern.compile("^You have joined (?:\\[[^]]+] )?(?<name>\\w+)'s? party!");
    private static final Pattern LEADER_LIST = Pattern.compile("^Party Leader: (?:\\[[^]]+] )?(?<name>\\w+)");

    private static final Pattern INSTANCE = Pattern.compile("([fmt])([1-7])");
    private static final String[] FLOORS = {"ENTRANCE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN"};
    private static final String[] KUUDRA = {"NORMAL", "HOT", "BURNING", "FIERY", "INFERNAL"};

    /** Hypixel says "Woah slow down" to chat sent this soon after your last message. */
    private static final long SEND_GAP_MS = 1_200L;
    /** Like Odin, answer a moment after the ! message, not in the same tick. */
    private static final long REPLY_DELAY_MS = 250L;

    /** SBO's !carrot answers. */
    private static final List<String> CARROT = List.of("As I see it, Carrot", "It is Carrot", "It is decidedly Carrot",
        "Most likely Carrot", "Outlook Carrot", "Signs point to Carrot", "Without a Carrot", "Yes - Carrot", "Carrot - definitely",
        "You may rely on Carrot", "Ask Carrot later", "Carrot predict now", "Concentrate and ask Carrot ",
        "Don't count on it - Carrot 2024", "My reply is Carrot", "My sources say Carrot", "Outlook not so Carrot", "Very Carrot");

    private record Pending(String command, long at) {}

    private static String leader;
    private static long lastCommand;
    private static long lastSent;
    /** Commands waiting to be sent, one at a time so Hypixel doesn't say "Woah slow down". */
    private static final Deque<Pending> pending = new ArrayDeque<>();

    private PartyCommands() {}

    public static void init() {
        SkyBallsChat.onChat(PartyCommands::onChat);
        ClientSendMessageEvents.CHAT.register(message -> lastSent = System.currentTimeMillis());
        ClientSendMessageEvents.COMMAND.register(command -> lastSent = System.currentTimeMillis());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Pending next = pending.peek();
            long now = System.currentTimeMillis();
            if (next == null || now < next.at() || now < lastSent + SEND_GAP_MS) return;
            pending.poll();
            if (mc.getConnection() != null) mc.getConnection().sendCommand(next.command());
        });
    }

    private static String me() {
        return Minecraft.getInstance().getUser().getName();
    }

    private static void onChat(SkyBallsChat.Message message) {
        String text = message.text();
        Matcher m;
        if ((m = INVITED.matcher(text)).find() && m.group("me").equalsIgnoreCase(me())) leader = me();
        else if ((m = TRANSFERRED.matcher(text)).find()) leader = m.group("name");
        else if ((m = JOINED_OTHER.matcher(text)).find()) leader = m.group("name");
        else if ((m = LEADER_LIST.matcher(text)).find()) leader = m.group("name");
        else if (text.equals("You left the party.") || text.endsWith("has disbanded the party!")
            || text.startsWith("You have been kicked from the party") || text.equals("The party was disbanded because all invites expired and the party was empty.")) {
            leader = null;
        }

        SkyBallsConfig c = SkyBallsConfig.current();
        if (c == null) return;
        FeatureConfigs.PartyCommands config = c.misc.partyCommands;
        if (!config.enabled || !(m = PARTY_CHAT.matcher(text)).find()) return;
        String sender = m.group("name");
        String command = m.group("command").toLowerCase(Locale.ROOT);
        String arg = m.group("arg");
        // Like SBO, a command with more words after it than it takes isn't one.
        if (m.group("more") != null) return;

        if (config.diana) {
            // A short list: a party message over 256 characters gets you disconnected.
            String diana = command.equals("help") ? "Diana: !chim !inq !king !manti !sphinx !relic !stick !core !wool !mobs !burrows !profit !since <drop/mob>"
                : com.epic60869.skyballs.features.combat.DianaPartyCommands.reply(command, arg, me());
            if (diana != null) {
                run("pc " + diana);
                return;
            }
        }

        // Anyone in the party (you too) can ask these, like Odin's chat commands.
        String reply = switch (command) {
            case "fps" -> config.fps ? "Current FPS: " + ServerInfo.fps() : null;
            case "ping" -> {
                Integer ping = ServerInfo.ping();
                yield config.ping ? "Current Ping: " + (ping == null ? "?" : ping) + "ms" : null;
            }
            case "tps" -> {
                Double tps = ServerInfo.tps();
                yield config.tps ? "Current TPS: " + (tps == null ? "?" : String.format(Locale.ROOT, "%.1f", tps)) : null;
            }
            case "carrot", "c" -> config.carrot ? CARROT.get(ThreadLocalRandom.current().nextInt(CARROT.size())) : null;
            case "time" -> config.time ? LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : null;
            default -> null;
        };
        if (reply != null) {
            run("pc " + reply);
            return;
        }

        if (leader == null || !leader.equalsIgnoreCase(me()) || sender.equalsIgnoreCase(me())) return;
        String toRun = switch (command) {
            case "warp" -> config.warp ? "party warp" : null;
            case "allinvite", "allinv" -> config.allInvite ? "party settings allinvite" : null;
            case "pt", "transfer", "ptme" -> config.transfer ? "party transfer " + sender : null;
            case "promote" -> config.promote ? "party promote " + (arg == null ? sender : arg) : null;
            case "demote" -> config.promote ? "party demote " + (arg == null ? sender : arg) : null;
            default -> instance(config, command);
        };
        if (toRun != null) run(toRun);
    }

    /** !f1-!f7, !m1-!m7 and !t1-!t5: Hypixel's /joininstance for that floor or tier (Odin's queue commands). */
    private static String instance(FeatureConfigs.PartyCommands config, String command) {
        Matcher m = INSTANCE.matcher(command);
        if (!m.matches()) return null;
        int n = Integer.parseInt(m.group(2));
        return switch (m.group(1)) {
            case "f" -> config.floors ? "joininstance CATACOMBS_FLOOR_" + FLOORS[n] : null;
            case "m" -> config.masterFloors ? "joininstance MASTER_CATACOMBS_FLOOR_" + FLOORS[n] : null;
            default -> config.kuudra && n <= 5 ? "joininstance KUUDRA_" + KUUDRA[n - 1] : null;
        };
    }

    private static void run(String command) {
        // Small cooldown so a spammed command is only run once.
        if (System.currentTimeMillis() - lastCommand <= 1500) return;
        long now = System.currentTimeMillis();
        lastCommand = now;
        // Sent from the tick, once it's been long enough since the last message you sent.
        Minecraft.getInstance().execute(() -> pending.add(new Pending(command, now + REPLY_DELAY_MS)));
    }

    /** Says something in party chat, queued behind anything else waiting to be sent (Diana drop announcements). */
    public static void partyChat(String message) {
        long now = System.currentTimeMillis();
        Minecraft.getInstance().execute(() -> pending.add(new Pending("pc " + message, now)));
    }
}
