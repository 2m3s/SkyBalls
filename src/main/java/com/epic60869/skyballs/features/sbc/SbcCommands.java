package com.epic60869.skyballs.features.sbc;

import com.epic60869.skyballs.custom.util.Compat;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** /sb help and the /sb commands of the SkyBalls Online features. */
public final class SbcCommands {
    private record Help(String usage, String description) {}

    private record Section(String title, List<Help> entries) {}

    private static final List<Section> HELP = List.of(
        new Section("General", List.of(
            new Help("/sb", "Open the SkyBalls settings"),
            new Help("/sb help", "This list"),
            new Help("/sb gui", "Move and resize the HUDs"),
            new Help("/sb toggle <setting>", "Turn a setting on or off"),
            new Help("/sb disableall", "Turn every feature off"),
            new Help("/sb notes", "Your notes"),
            new Help("/sb search", "Search your storage (or press O)"),
            new Help("/sb calc <sum>", "Calculator"),
            new Help("/sb recipe <item>", "Show an item's recipe"),
            new Help("/sb keys", "Command keys"),
            new Help("/sb custom", "Item and armor customization"),
            new Help("/sb discord", "The SkyBalls Discord"),
            new Help("/sb bugreport | suggest | feedback", "Send a report to the SkyBalls team"))),
        new Section("SkyBalls Chat", List.of(
            new Help("/sbc [message]", "Send to SkyBalls chat (no message: switch to the SkyBalls channel)"),
            new Help("/sb chat leave", "Leave the SkyBalls channel"),
            new Help("/sb reply <id>", "Reply to a message (or click ↩ after it)"),
            new Help("/sb share", "Share the item you're holding (or the Share Item key; [item] in a message)"),
            new Help("/sb react <id> <emoji>", "React to a message (or hover it with chat open)"),
            new Help("/sb ignore <player> | discord <name> | list", "Stop seeing someone's messages"),
            new Help("/sb unignore <player> | discord <name>", "See their messages again"),
            new Help("/sb nick", "Your nickname"))),
        new Section("Players", List.of(
            new Help("/sb who", "Who's online with SkyBalls"),
            new Help("/sb friend add|remove|accept|deny <player>", "Manage SkyBalls friends"),
            new Help("/sb friend list", "Your friends (or /sb friends)"),
            new Help("/sb cosmetics", "Pick your badge and cape"))),
        new Section("Leaderboards", List.of(
            new Help("/sb leaderboard", "SkyBalls leaderboards"))),
        new Section("SkyBlock", List.of(
            new Help("/sb calendar", "Upcoming SkyBlock events"),
            new Help("/sb portfolio", "Your portfolio"),
            new Help("/sb trackcollection", "Track a collection"),
            new Help("/sb itemnotify", "Item notifications"),
            new Help("/sb helditem", "Held item editor"),
            new Help("/sb waypoints", "Waypoints"))),
        new Section("Settings Sync", List.of(
            new Help("/sb settings upload [slot]", "Save your settings to the SkyBalls server"),
            new Help("/sb settings download [slot]", "Load saved settings"),
            new Help("/sb settings list", "Your saved slots"),
            new Help("/sb settings delete <slot>", "Delete a slot"))));

    private SbcCommands() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) dispatcher.register(tree(root));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> tree(String root) {
        return ClientCommands.literal(root)
            .then(ClientCommands.literal("help").executes(c -> help()))
            .then(ClientCommands.literal("cosmetics").executes(c -> run(SbcCosmetics::openScreen)))
            .then(ClientCommands.literal("settings")
                .executes(c -> {
                    SbcSettingsSync.list();
                    return 1;
                })
                .then(ClientCommands.literal("upload")
                    .executes(c -> run(() -> SbcSettingsSync.upload(null)))
                    .then(ClientCommands.argument("slot", StringArgumentType.word())
                        .executes(c -> run(() -> SbcSettingsSync.upload(StringArgumentType.getString(c, "slot"))))))
                .then(ClientCommands.literal("download")
                    .executes(c -> run(() -> SbcSettingsSync.download(null)))
                    .then(ClientCommands.argument("slot", StringArgumentType.word())
                        .executes(c -> run(() -> SbcSettingsSync.download(StringArgumentType.getString(c, "slot"))))))
                .then(ClientCommands.literal("list").executes(c -> run(SbcSettingsSync::list)))
                .then(ClientCommands.literal("delete")
                    .then(ClientCommands.argument("slot", StringArgumentType.word())
                        .executes(c -> run(() -> SbcSettingsSync.delete(StringArgumentType.getString(c, "slot")))))));
    }

    /** Runs a command's action, reporting (instead of throwing) anything that goes wrong. */
    static int run(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            SbcCrashReports.report(e, "command");
            Sbc.error("Something went wrong: " + e.getMessage());
        }
        return 1;
    }

    private static int help() {
        MutableComponent out = Component.literal("SkyBalls commands").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal(" (click one to type it)").withStyle(s -> s.withColor(ChatFormatting.GRAY).withBold(false)));
        for (Section section : HELP) {
            out.append(Component.literal("\n" + section.title()).withStyle(s -> s.withColor(ChatFormatting.YELLOW).withBold(false)));
            for (Help help : section.entries()) {
                String typed = help.usage().replaceFirst("\\s*[<\\[|].*$", "").trim() + " ";
                out.append(Component.literal("\n  " + help.usage()).withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(false)
                        .withClickEvent(new ClickEvent.SuggestCommand(typed))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(help.description())))))
                    .append(Component.literal(" - " + help.description()).withStyle(s -> s.withColor(ChatFormatting.GRAY).withBold(false)));
            }
        }
        Sbc.say(out);
        return 1;
    }
}
