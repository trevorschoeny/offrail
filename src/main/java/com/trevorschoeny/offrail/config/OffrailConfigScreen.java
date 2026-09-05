package com.trevorschoeny.offrail.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** YACL screen: one tab, one toggle per feature. */
public final class OffrailConfigScreen {

    private OffrailConfigScreen() {}

    public static Screen create(Screen parent) {
        Option<Boolean> placement = booleanOption(
                "Rail-free placement",
                "Right-click any block that isn't water or lava with a minecart to set it "
                        + "down there, centered and facing the way you're looking. Rails still "
                        + "work the vanilla way. On a dedicated server this is the server's setting.",
                true, OffrailConfig::railFreePlacement, OffrailConfig::setRailFreePlacement);
        Option<Boolean> pickup = booleanOption(
                "Stationary pickup",
                "A parked minecart picks up a mob that walks into it, instead of only while "
                        + "it's moving. On a dedicated server this is the server's setting.",
                true, OffrailConfig::stationaryPickup, OffrailConfig::setStationaryPickup);

        ConfigCategory features = ConfigCategory.createBuilder()
                .name(Component.literal("Features"))
                .group(OptionGroup.createBuilder()
                        .name(Component.literal("Offrail"))
                        .description(OptionDescription.of(Component.literal(
                                "Minecarts made less brittle. Each feature has its own switch.")))
                        .option(placement)
                        .option(pickup)
                        .build())
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Offrail"))
                .category(features)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> booleanOption(String name, String description, boolean defaultValue,
                                                 Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(BooleanControllerBuilder::create)
                .build();
    }
}
