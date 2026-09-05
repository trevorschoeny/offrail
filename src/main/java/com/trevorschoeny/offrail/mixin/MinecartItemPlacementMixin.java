package com.trevorschoeny.offrail.mixin;

import com.trevorschoeny.offrail.config.OffrailConfig;
import com.trevorschoeny.offrail.placement.RailFreePlacement;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rail-free placement hook. Vanilla's first line in {@code useOn} is
 * "not a rail, FAIL". This runs just before it: if the clicked block isn't a
 * rail, Offrail places the cart itself and the vanilla body never runs. Rails
 * fall through untouched, so vanilla keeps its rail-shape and slope handling.
 *
 * <p>Site choice (plan.md asked for the trade-off to be weighed): hooking
 * {@code MinecartItem} keeps the blast radius to the one item class and
 * leaves the entity spawn helper and the block-interaction path alone, so
 * other mods hooking either of those still see vanilla behaviour.
 */
@Mixin(MinecartItem.class)
public abstract class MinecartItemPlacementMixin {

    @Shadow @Final private EntityType<? extends AbstractMinecart> type;

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void offrail$placeOffRail(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!OffrailConfig.railFreePlacement()) return;
        if (ctx.getLevel().getBlockState(ctx.getClickedPos()).is(BlockTags.RAILS)) return;
        cir.setReturnValue(RailFreePlacement.placeOnBlock(ctx, this.type));
    }
}
