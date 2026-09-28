package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.ItemCooldowns;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item cooldowns: the cooldown bar/shade over the item's slot, drawn with its count and durability. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class SkyBallsItemCooldownMixin {
    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void skyballs$cooldown(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        ItemCooldowns.drawOverlay((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }
}
