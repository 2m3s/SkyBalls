package com.epic60869.skyballs.features.fishing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Fishing Festival personal bests (Feesh's PersonalBestData), in config/skyballs/fishing.json. */
final class FishingRecords {
    static int sharks;
    static int greatWhites;

    private FishingRecords() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyballs").resolve("fishing.json");
    }

    static void load() {
        try {
            Path file = file();
            if (!Files.exists(file)) return;
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            sharks = json.has("festivalSharks") ? json.get("festivalSharks").getAsInt() : 0;
            greatWhites = json.has("festivalGreatWhites") ? json.get("festivalGreatWhites").getAsInt() : 0;
        } catch (Exception e) {
            System.err.println("[SkyBalls] Couldn't read fishing records: " + e.getMessage());
        }
    }

    static void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("festivalSharks", sharks);
            json.addProperty("festivalGreatWhites", greatWhites);
            Files.createDirectories(file().getParent());
            Files.writeString(file(), json.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Couldn't save fishing records: " + e.getMessage());
        }
    }
}
