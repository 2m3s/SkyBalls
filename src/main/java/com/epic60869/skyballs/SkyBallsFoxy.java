package com.epic60869.skyballs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

public final class SkyBallsFoxy {
    private static final Random RANDOM = new Random();
    private static int tickCounter;

    private SkyBallsFoxy() {}

    /**
     * In a dungeon or Kuudra run. Also true while the area isn't known yet on SkyBlock (world loading into an
     * instance), so it never fires in the moment before the location is read.
     */
    private static boolean inInstance() {
        if (com.epic60869.skyballs.features.core.SkyBallsLocation.inDungeon()) return true;
        String area = com.epic60869.skyballs.features.core.SkyBallsLocation.area();
        String location = com.epic60869.skyballs.features.core.SkyBallsLocation.location();
        if (area.toLowerCase(java.util.Locale.ROOT).contains("kuudra") || location.toLowerCase(java.util.Locale.ROOT).contains("kuudra's hollow")) return true;
        return com.epic60869.skyballs.features.core.SkyBallsLocation.onSkyblock() && area.isEmpty();
    }

    public static void tick(Minecraft mc) {
        tickCounter++;

        // One independent 1/100,000 roll every second (10x rarer than the linked datapack's 1/10,000).
        if (tickCounter < 20) return;
        tickCounter = 0;

        if (SkyBallsFoxyScare.isPlaying()) return;
        // Never in dungeons or Kuudra, where a surprise scare would ruin a run.
        if (inInstance()) return;

        if (RANDOM.nextInt(100_000) == 0) {
            mc.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.GHAST_SCREAM, 1.5F, 0.72F)
            );
            SkyBallsFoxyScare.start();
        }
    }
}
