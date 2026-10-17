package com.trevorschoeny.offrail.checks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/** Checks for the place-and-pickup scene (test-gallery/scenes/offrail/place-and-pickup.json). */
public class PlacementChecks implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "offrail", "place-and-pickup")) {
            cartPlacesOnFloor(ctx, scene);
        }
    }

    /**
     * Station "offrail:place_on_floor": a minecart used on a plain floor, no rail, becomes a cart.
     * Vanilla only places one on a rail, so a cart here is Offrail at work. The scene puts the
     * minecarts in hotbar slot 0, which is the selected slot on arrival.
     */
    private static void cartPlacesOnFloor(ClientGameTestContext ctx, SceneWorld.Opened scene) {
        JsonObject station = scene.station("offrail:place_on_floor");
        scene.standAt(station, 60);                         // crosshair on the floor a couple of blocks ahead
        ctx.waitTicks(5);
        ctx.getInput().pressKey(options -> options.keyUse);
        ctx.waitTicks(10);

        // the station's whole floor area, from where the player stands to its back edge
        JsonArray o = station.getAsJsonArray("origin");
        int x = o.get(0).getAsInt(), y = o.get(1).getAsInt(), size = station.get("size").getAsInt();
        AABB area = new AABB(x - 1, y - 1, -8, x + size + 1, y + 3, size + 2);
        int carts = scene.world().getServer().computeOnServer(
                server -> server.overworld().getEntities(EntityTypeTest.forClass(AbstractMinecart.class), area, e -> true).size());
        ctx.takeScreenshot("offrail-place-on-floor");
        if (carts != 1) throw new AssertionError("using a minecart on the bare floor should place one cart; found " + carts);
    }
}
