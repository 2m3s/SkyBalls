package com.epic60869.skyballs.features.sbc;

import net.fabricmc.loader.api.FabricLoader;

/** The installed SkyBalls and Minecraft versions, as sent to the mod server. */
public final class SbcInfo {
    private SbcInfo() {}

    public static String modVersion() {
        return FabricLoader.getInstance().getModContainer("skyballs")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    public static String mcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
    }
}
