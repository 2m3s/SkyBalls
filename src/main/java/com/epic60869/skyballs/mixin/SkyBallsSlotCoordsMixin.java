// Ported from Firmament (https://github.com/FirmamentMC/Firmament), mixins/customgui/OriginalSlotCoords.java.
// SPDX-FileCopyrightText: Linnea Gräf <nea@nea.moe>, Firmament Contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.features.misc.storage.CustomGui;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Remembers where a slot was before a custom gui moved it, to put it back. Slot.x/y are made mutable in skyballs.classtweaker. */
@Mixin(Slot.class)
public class SkyBallsSlotCoordsMixin implements CustomGui.RememberingSlot {
    @Shadow public int x;
    @Shadow public int y;
    @Unique private int skyballs$originalX;
    @Unique private int skyballs$originalY;

    @Override
    public void skyballs$rememberCoords() {
        this.skyballs$originalX = this.x;
        this.skyballs$originalY = this.y;
    }

    @Override
    public void skyballs$restoreCoords() {
        this.x = this.skyballs$originalX;
        this.y = this.skyballs$originalY;
    }
}
