package com.trevorschoeny.offrail;

import com.trevorschoeny.offrail.config.OffrailConfig;
import com.trevorschoeny.offrail.placement.RailFreePlacement;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point, runs on both physical sides. Loads the config so the
 * toggles are known before the first minecart is placed or ticked, and
 * registers the one non-mixin hook: stacking a cart on a cart goes through
 * Fabric's entity-use event, since a click on an entity never reaches the
 * item's use-on-block path.
 */
public class Offrail implements ModInitializer {

    public static final String MOD_ID = "offrail";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        OffrailConfig.load();
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                OffrailConfig.railFreePlacement()
                        ? RailFreePlacement.stackOnCart(player, level, entity, player.getItemInHand(hand))
                        : InteractionResult.PASS);
        LOGGER.info("[offrail] init: rail-free placement {}, stationary pickup {}",
                OffrailConfig.railFreePlacement() ? "on" : "off",
                OffrailConfig.stationaryPickup() ? "on" : "off");
    }
}
