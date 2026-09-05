package com.trevorschoeny.offrail.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.trevorschoeny.offrail.Offrail;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Offrail config: one toggle per feature, both default on, no master switch
 * (plan.md). Same static-field + JSON shape as the other house mods;
 * persisted to {@code config/offrail/config.json}.
 *
 * <p>Both toggles are read on the server (placement runs in the item's
 * use-on, pickup in the cart's tick). On a dedicated server the server's
 * file decides; the client's copy only drives the config screen.
 */
public final class OffrailConfig {

    private OffrailConfig() {}

    private static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Place a minecart on any non-liquid block, not just a rail.
    private static boolean railFreePlacement = true;
    // A cart picks up mobs even when it isn't moving.
    private static boolean stationaryPickup = true;

    private static boolean loaded = false;

    private static Path filePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("offrail")
                .resolve("config.json");
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path path = filePath();
        if (!Files.exists(path)) {
            Offrail.LOGGER.info("[config] no config at {}, using defaults", path);
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            railFreePlacement = readBool(root, "railFreePlacement", railFreePlacement);
            stationaryPickup = readBool(root, "stationaryPickup", stationaryPickup);
            Offrail.LOGGER.info("[config] loaded from {}", path);
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            Offrail.LOGGER.error("[config] failed to read {}, using defaults", path, e);
        }
    }

    private static boolean readBool(JsonObject root, String key, boolean fallback) {
        return root.has(key) && root.get(key).isJsonPrimitive()
                ? root.get(key).getAsBoolean() : fallback;
    }

    private static void save() {
        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", CURRENT_VERSION);
            root.addProperty("railFreePlacement", railFreePlacement);
            root.addProperty("stationaryPickup", stationaryPickup);
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException e) {
            Offrail.LOGGER.error("[config] failed to write {}, changes won't persist", path, e);
        }
    }

    public static boolean railFreePlacement() { return railFreePlacement; }
    public static void setRailFreePlacement(boolean v) { railFreePlacement = v; save(); }

    public static boolean stationaryPickup() { return stationaryPickup; }
    public static void setStationaryPickup(boolean v) { stationaryPickup = v; save(); }
}
