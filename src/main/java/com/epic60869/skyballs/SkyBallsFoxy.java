package com.epic60869.skyballs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

public final class SkyBallsFoxy {
    private static final Random RANDOM = new Random();

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

    /** The menu to open once the scare is over (it's drawn on the HUD, so a menu would cover it). */
    private static Runnable afterScare;

    public static void tick(Minecraft mc) {
        if (afterScare != null && !SkyBallsFoxyScare.isPlaying()) {
            Runnable open = afterScare;
            afterScare = null;
            open.run();
        }
    }

    /**
     * Opening the SkyBalls menu (/sb): a 1 in 1,000 chance of the scare first, never in dungeons
     * or Kuudra. {@code open} opens the menu, straight away or once the scare is over.
     */
    public static void openMenu(Minecraft mc, Runnable open) {
        if (SkyBallsFoxyScare.isPlaying() || inInstance() || RANDOM.nextInt(1_000) != 0) {
            open.run();
            return;
        }
        // Close whatever is open so the scare shows.
        mc.gui.setScreen(null);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GHAST_SCREAM, 1.5F, 0.72F));
        SkyBallsFoxyScare.start();
        afterScare = open;
    }
}
