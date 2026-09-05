package com.trevorschoeny.offrail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.trevorschoeny.offrail.config.OffrailConfig;

import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stationary pickup. Vanilla's {@code pushAndPickupEntities} only enters the
 * pickup branch when {@code deltaMovement.horizontalDistanceSqr() >= 0.01};
 * a slower or parked cart only pushes. This wraps that one call and reports
 * the cart as moving, so the pickup branch runs at any speed. The branch's
 * other conditions (rideable cart, empty, mob not a player/golem/cart, mob
 * not already riding) are vanilla's and untouched.
 *
 * <p>Survey (plan.md asked): this is the only velocity gate on pickup.
 * {@code NewMinecartBehavior} (the "minecart improvements" experiment) has no
 * such gate at all, so under that flag stationary pickup already happens and
 * this mixin has nothing to do. The mob AI side has no cart awareness.
 */
@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorPickupMixin {

    @WrapOperation(
            method = "pushAndPickupEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;horizontalDistanceSqr()D"))
    private double offrail$ignoreSpeedForPickup(Vec3 movement, Operation<Double> original) {
        // Anything >= 0.01 passes the gate; MAX_VALUE reads as "moving, always".
        return OffrailConfig.stationaryPickup() ? Double.MAX_VALUE : original.call(movement);
    }
}
