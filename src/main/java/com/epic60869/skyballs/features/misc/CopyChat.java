package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsStaff;
import com.epic60869.skyballs.mixin.SkyBallsChatComponentAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Copy Chat, from NoFrills' Chat Tweaks: with chat open, press the Copy Message Key (any key or mouse button, right
 * click by default) over a message to copy the whole message. A short preview of what was copied shows in chat. SkyBalls rank prefixes like "[OWNER] " are left out of what's copied.
 */
public final class CopyChat {
    private CopyChat() {}

    private static SkyBallsConfig.CopyChat config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.chat.copyChat;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ChatScreen)) return;
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                SkyBallsConfig.CopyChat c = config();
                if (c == null || !c.enabled || c.copyMessageKey != -100 + event.button()) return true;
                return !copy(event.x(), event.y());
            });
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                SkyBallsConfig.CopyChat c = config();
                if (c == null || !c.enabled || c.copyMessageKey < 0 || c.copyMessageKey == GLFW.GLFW_KEY_UNKNOWN || event.key() != c.copyMessageKey) return true;
                Minecraft mc = Minecraft.getInstance();
                double x = mc.mouseHandler.getScaledXPos(mc.getWindow());
                double y = mc.mouseHandler.getScaledYPos(mc.getWindow());
                // Over a message: copy it and keep the key out of the chat box. Anywhere else the key types as normal.
                return !copy(x, y);
            });
        });
    }

    /** Copies the chat message under the mouse; false if there's none there. */
    private static boolean copy(double mouseX, double mouseY) {
        String text = hovered(mouseX, mouseY);
        if (text.isEmpty()) return false;
        text = stripRanks(text);
        SkyBallsConfig.CopyChat c = config();
        if (c != null && c.trim) text = text.trim();
        Minecraft mc = Minecraft.getInstance();
        mc.keyboardHandler.setClipboard(text);
        if (c != null && c.preview) {
            int length = c.previewLength;
            String shown = length > 0 && text.length() > length ? text.substring(0, length) + "..." : text;
            Component message = Component.literal("[SB] ").withStyle(ChatFormatting.DARK_GREEN)
                .append(Component.literal("Message copied" + (length == 0 ? "." : ": ")).withStyle(ChatFormatting.GREEN));
            if (length != 0) {
                message = message.copy().append(Component.literal("\"").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(shown).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\"").withStyle(ChatFormatting.GREEN));
            }
            mc.gui.hud.getChat().addClientSystemMessage(message);
        }
        return true;
    }

    /** SkyBalls rank prefixes ("[OWNER] ") aren't part of what the player wrote. */
    private static String stripRanks(String text) {
        String out = text;
        for (SkyBallsStaff.Rank rank : SkyBallsStaff.allRanks()) out = out.replace("[" + rank.label() + "] ", "");
        return out;
    }

    /** The plain text of the chat message under the mouse, like NoFrills' getHoveredMsg. */
    private static String hovered(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.hud.getChat();
        SkyBallsChatComponentAccessor access = (SkyBallsChatComponentAccessor) chat;
        List<GuiMessage.Line> all = access.skyballs$trimmedMessages();
        double chatScale = mc.options.chatScale().get();
        int entryHeight = (int) (9.0 * (mc.options.chatLineSpacing().get() + 1.0));
        int chatHeight = ChatComponent.getHeight(mc.options.chatHeightFocused().get());
        int start = access.skyballs$chatScrollbarPos();
        int perPage = chatHeight / Math.max(1, entryHeight);
        int shown = Math.min(perPage, all.size() - start);
        if (shown <= 0) return "";
        // Same maths as vanilla's ChatComponent (screenToChatX/Y, getMessageLineIndexAt), in doubles, so the line
        // found is exactly the one the mouse is over. Rounding each line's edges to whole pixels could pick the
        // line above or below near an edge.
        double chatX = mouseX / chatScale - 4.0;
        double chatY = (mc.getWindow().getGuiScaledHeight() - mouseY - 40.0) / (chatScale * entryHeight);
        if (chatX < -4.0 || chatX > Mth.floor(ChatComponent.getWidth(mc.options.chatWidth().get()) / chatScale)) return "";
        if (chatY < 0.0 || chatY >= shown) return "";
        int index = Mth.floor(chatY);
        List<GuiMessage.Line> visible = all.subList(start, start + shown);
        StringBuilder out = new StringBuilder();
        for (GuiMessage.Line line : fullMessage(visible, index)) out.append(plain(line.content()));
        return out.toString();
    }

    private static List<GuiMessage.Line> fullMessage(List<GuiMessage.Line> visible, int index) {
        List<GuiMessage.Line> lines = new ArrayList<>();
        for (int i = index + 1; i < visible.size(); i++) {
            GuiMessage.Line line = visible.get(i);
            if (line.endOfEntry()) break;
            lines.addFirst(line);
        }
        for (int i = index; i >= 0; i--) {
            GuiMessage.Line line = visible.get(i);
            lines.add(line);
            if (line.endOfEntry()) break;
        }
        return lines;
    }

    private static String plain(FormattedCharSequence sequence) {
        StringBuilder out = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            out.appendCodePoint(codePoint);
            return true;
        });
        return out.toString();
    }
}
