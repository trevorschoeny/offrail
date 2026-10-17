package com.trevorschoeny.offrail.checks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens a Test Gallery scene's check world in a client gametest: the same stations Trev walks,
 * built by the same gen.py, so a check never rebuilds a fixture in Java.
 *
 * ponytail: lives in Offrail while it is the only mod with checks. Moves to a shared
 * library in the test-gallery repo once a second mod needs it.
 */
final class SceneWorld {
    private SceneWorld() {}

    /** An open check world and the station index gen.py wrote beside it. */
    record Opened(TestSingleplayerContext world, JsonArray stations) implements AutoCloseable {
        /** The station whose "check" is this id; fails the test if the scene has none. */
        JsonObject station(String checkId) {
            for (JsonElement e : stations) {
                JsonObject st = e.getAsJsonObject();
                if (!st.get("check").isJsonNull() && st.get("check").getAsString().equals(checkId)) return st;
            }
            throw new AssertionError("no station in the scene has \"check\": \"" + checkId + "\"");
        }

        /** Teleports the player to where Trev would stand for this station, looking at it, pitched down. */
        void standAt(JsonObject station, float pitch) {
            JsonArray s = station.getAsJsonArray("stand");
            world.getServer().runCommand("tp @p " + s.get(0) + " " + s.get(1) + " " + s.get(2) + " 0 " + pitch);
        }

        @Override public void close() { world.close(); }
    }

    /**
     * Fabric's API can only open a world it created, so: create one, close it, replace its folder
     * with gen.py's check world, and open it again. Every call builds afresh, because a scene
     * world builds its stations only once, on first load.
     */
    static Opened open(ClientGameTestContext ctx, String mod, String scene) {
        Path gallery = Path.of(System.getProperty("testgallery.dir"));
        TestWorldSave save;
        try (TestSingleplayerContext fresh = ctx.worldBuilder().create()) {
            save = fresh.getWorldSave();
        }
        Path dir = save.getSaveDirectory();
        run(gallery, "python3", "gen.py", "--checks", "scenes/" + mod + "/" + scene + ".json", dir.toString());

        JsonArray stations;
        try {
            stations = JsonParser.parseString(Files.readString(dir.resolve("stations.json"))).getAsJsonArray();
        } catch (Exception e) {
            throw new AssertionError("gen.py wrote no readable stations.json in " + dir, e);
        }

        TestSingleplayerContext world = save.open();
        // gallery:join runs on the player's first tick: teleport to the first plate, clear the
        // inventory, give the scene's. Anything set up before it would be wiped, so wait for the
        // teleport. Every plate's stand point shares one z, a few blocks north of spawn.
        double standZ = stations.get(0).getAsJsonObject().getAsJsonArray("stand").get(2).getAsDouble();
        ctx.waitFor(mc -> mc.player != null && Math.abs(mc.player.getZ() - standZ) < 0.01);
        world.getConnection().waitForChunksRender();
        return new Opened(world, stations);
    }

    private static void run(Path cwd, String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).directory(cwd.toFile()).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            if (p.waitFor() != 0) throw new AssertionError(String.join(" ", cmd) + " failed:\n" + out);
        } catch (java.io.IOException | InterruptedException e) {
            throw new AssertionError("could not run " + String.join(" ", cmd), e);
        }
    }
}
