package com.trevorschoeny.offrail;

import com.trevorschoeny.offrail.config.OffrailConfig;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point, runs on both physical sides. Both features are mixins
 * and need no registration; the only job here is loading the config so the
 * toggles are known before the first minecart is placed or ticked.
 */
public class Offrail implements ModInitializer {

    public static final String MOD_ID = "offrail";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        OffrailConfig.load();
        LOGGER.info("[offrail] init: rail-free placement {}, stationary pickup {}",
                OffrailConfig.railFreePlacement() ? "on" : "off",
                OffrailConfig.stationaryPickup() ? "on" : "off");
    }
}
