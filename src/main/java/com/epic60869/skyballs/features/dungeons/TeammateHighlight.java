package com.epic60869.skyballs.features.dungeons;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.core.EntityGlow;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.epic60869.skyballs.sb.skyblock.dungeon.DungeonClass;
import com.epic60869.skyballs.sb.skyblock.dungeon.secrets.DungeonPlayerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Glowing outlines on your dungeon teammates in their class's Leap Menu colour. Ghosts (no class) aren't outlined. */
public final class TeammateHighlight {
    private TeammateHighlight() {}

    public static void init() {
        EntityGlow.register(entity -> {
            if (!(entity instanceof Player player) || player == Minecraft.getInstance().player) return -1;
            SkyBallsConfig config = SkyBallsConfig.current();
            if (config == null || !config.dungeons.mobs.teammates || !SkyBallsLocation.inDungeon()) return -1;
            DungeonClass dungeonClass = DungeonPlayerManager.getClassFromPlayer(player);
            return dungeonClass == DungeonClass.UNKNOWN ? -1 : LeapMenu.classColour(dungeonClass) & 0xFFFFFF;
        });
    }
}
