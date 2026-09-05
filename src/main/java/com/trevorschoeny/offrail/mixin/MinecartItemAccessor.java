package com.trevorschoeny.offrail.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.MinecartItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the cart type a {@link MinecartItem} spawns; vanilla keeps it private with no getter. */
@Mixin(MinecartItem.class)
public interface MinecartItemAccessor {
    @Accessor("type")
    EntityType<? extends AbstractMinecart> offrail$type();
}
