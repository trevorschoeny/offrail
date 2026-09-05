package com.trevorschoeny.offrail.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu entry point: opens the Offrail config screen from the mods list. */
public final class OffrailConfigModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OffrailConfigScreen::create;
    }
}
