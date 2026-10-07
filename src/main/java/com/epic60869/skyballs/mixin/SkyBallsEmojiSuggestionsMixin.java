package com.epic60869.skyballs.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Adapts NopoMod's emoji autocomplete behaviour: when the current chat token
 * starts with ':', SkyBalls supplies the emoji shortcode suggestions instead of
 * vanilla command suggestions.
 */
@Mixin(CommandSuggestions.class)
public abstract class SkyBallsEmojiSuggestionsMixin {
    @Unique
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Shadow
    @Final
    EditBox input;

    @Shadow
    @Nullable
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("TAIL"), cancellable = true)
    private void skyballs$emojiSuggestions(CallbackInfo ci) {
        if (skyballs$mentionSuggestions()) {
            ci.cancel();
            return;
        }
        if (!com.epic60869.skyballs.features.misc.ItemEmojis.autocompleteEnabled()) return;

        String text = input.getValue();
        int cursor = input.getCursorPosition();
        if (cursor <= 0 || cursor > text.length()) return;

        String uptoCursor = text.substring(0, cursor);
        int whitespace = lastWhitespaceEnd(uptoCursor);
        String token = uptoCursor.substring(whitespace);

        if (!token.startsWith(":")) return;
        if (token.indexOf(':', 1) >= 0) return;

        // A short list (up to 50), not every emoji: typing ':' listed thousands and lagged.
        SuggestionsBuilder builder = new SuggestionsBuilder(uptoCursor, whitespace);
        for (String emoji : com.epic60869.skyballs.features.misc.ItemEmojis.suggestions(token)) builder.suggest(emoji);
        pendingSuggestions = builder.buildFuture();
        pendingSuggestions.thenRun(() -> {
            if (pendingSuggestions != null && pendingSuggestions.isDone()) {
                showSuggestions(false);
            }
        });
        ci.cancel();
    }

    /** Room for the emoji's picture in front of its name. */
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "showSuggestions", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int skyballs$emojiPreviewWidth(net.minecraft.client.gui.Font font, String text, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Integer> original) {
        int width = original.call(font, text);
        return com.epic60869.skyballs.features.misc.ItemEmojis.isEmojiSuggestion(text) ? width + com.epic60869.skyballs.features.misc.ItemEmojis.PREVIEW_WIDTH : width;
    }

    /** "@na" completes the names of SkyBalls players (in /sbc, or anywhere while in the SkyBalls channel). */
    @Unique
    private boolean skyballs$mentionSuggestions() {
        if (!com.epic60869.skyballs.features.sbc.Sbc.config().chat.mentionCompletion) return false;
        String text = input.getValue();
        boolean sbc = text.startsWith("/sbc ") || text.startsWith("/sb chat ") || text.startsWith("/skyballs chat ")
            || (com.epic60869.skyballs.SkyBallsGlobalChat.isInSkyBallsChannel() && !text.startsWith("/"));
        if (!sbc) return false;
        int cursor = input.getCursorPosition();
        if (cursor <= 0 || cursor > text.length()) return false;
        String uptoCursor = text.substring(0, cursor);
        int whitespace = lastWhitespaceEnd(uptoCursor);
        String token = uptoCursor.substring(whitespace);
        if (!token.startsWith("@")) return false;
        String typed = token.substring(1).toLowerCase(java.util.Locale.ROOT);
        SuggestionsBuilder builder = new SuggestionsBuilder(uptoCursor, whitespace);
        for (String name : com.epic60869.skyballs.features.sbc.SbcChat.mentionNames()) {
            if (name.toLowerCase(java.util.Locale.ROOT).startsWith(typed)) builder.suggest("@" + name);
        }
        pendingSuggestions = builder.buildFuture();
        pendingSuggestions.thenRun(() -> {
            if (pendingSuggestions != null && pendingSuggestions.isDone()) showSuggestions(false);
        });
        return true;
    }

    @Unique
    private int lastWhitespaceEnd(String text) {
        int end = 0;
        var matcher = WHITESPACE.matcher(text);
        while (matcher.find()) end = matcher.end();
        return end;
    }
}
