package com.trevorschoeny.offrail.placement;

import com.trevorschoeny.offrail.mixin.MinecartItemAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Rail-free placement (plan.md): put a minecart down on any block that isn't
 * a liquid, or on top of another cart. The cart lands centered, at the height
 * of the click (so a slab or carpet gets the cart on its surface, not floating
 * half a block up), with its long axis along the player's look direction
 * snapped to a cardinal.
 *
 * <p>Mirrors vanilla {@code MinecartItem.useOn} step for step (spawn reason,
 * game event, stack consumption); only the "must be a rail" gate and the
 * position differ. Rails stay vanilla's.
 */
public final class RailFreePlacement {

    private RailFreePlacement() {}

    // Vanilla's on-rail resting height above the block floor; reused so a cart
    // set on a bare block sits at the same height as one on a flat rail.
    private static final double REST_OFFSET = 0.0625;

    /**
     * Block placement: the cart goes in the block space on the clicked side of
     * the clicked block. The caller has already established that block is not
     * a rail.
     *
     * @return {@link InteractionResult#SUCCESS} if the cart was placed (or would
     *         be, on the client), {@link InteractionResult#FAIL} if the spot is
     *         a liquid or blocked.
     */
    public static InteractionResult placeOnBlock(UseOnContext ctx, EntityType<? extends AbstractMinecart> type) {
        Level level = ctx.getLevel();
        BlockPos target = ctx.getClickedPos().relative(ctx.getClickedFace());
        BlockState targetState = level.getBlockState(target);

        // Liquids are the one excluded surface (plan.md). Clicking through water
        // hits the seabed, so the check has to be on the landing space, not the
        // clicked block.
        if (!targetState.getFluidState().isEmpty()) {
            return InteractionResult.FAIL;
        }

        // Top face: rest on the surface where the click landed (slabs, carpet,
        // snow layers). Any other face: the floor of the target block space;
        // gravity does the rest.
        double y = ctx.getClickedFace() == Direction.UP
                ? ctx.getClickLocation().y + REST_OFFSET
                : target.getY() + REST_OFFSET;
        Vec3 pos = new Vec3(target.getX() + 0.5, y, target.getZ() + 0.5);

        return spawn(level, pos, target, type, ctx.getItemInHand(), ctx.getPlayer());
    }

    /**
     * Stacking: shift-right-click an existing cart while holding a minecart
     * item puts the new cart on top of it. Vanilla never reaches {@code useOn}
     * here because the raycast stops at the entity, so this is its own path
     * (Fabric's UseEntityCallback, registered in the mod initializer).
     *
     * @return {@link InteractionResult#PASS} when this click isn't a stack
     *         (not sneaking, not holding a cart), so vanilla handles it.
     */
    public static InteractionResult stackOnCart(Player player, Level level, Entity target, ItemStack held) {
        if (!(target instanceof AbstractMinecart below)) return InteractionResult.PASS;
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(held.getItem() instanceof MinecartItem item)) return InteractionResult.PASS;

        Vec3 pos = new Vec3(below.getX(), below.getBoundingBox().maxY + REST_OFFSET, below.getZ());
        return spawn(level, pos, below.blockPosition(), ((MinecartItemAccessor) item).offrail$type(), held, player);
    }

    /** Shared tail of both paths: create, orient, collision-check, add, consume. */
    private static InteractionResult spawn(Level level, Vec3 pos, BlockPos eventPos,
                                           EntityType<? extends AbstractMinecart> type,
                                           ItemStack stack, Player player) {
        AbstractMinecart cart = AbstractMinecart.createMinecart(
                level, pos.x, pos.y, pos.z, type, EntitySpawnReason.DISPENSER, stack, player);
        if (cart == null) {
            return InteractionResult.FAIL;
        }

        // Long axis along the player's look direction, snapped to N/S/E/W. A
        // cart's yRot is perpendicular to its axis (yRot 0 is an east-west
        // cart), hence the +90. Both rotation fields so the first rendered
        // frame doesn't lerp from zero.
        Direction facing = player != null ? player.getDirection() : Direction.NORTH;
        cart.setYRot(facing.toYRot() + 90.0F);
        cart.yRotO = cart.getYRot();

        // Blocks only: stacking a cart on top of another cart is allowed
        // (plan.md), so entity collisions are deliberately not checked.
        if (!level.noBlockCollision(cart, cart.getBoundingBox())) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, eventPos,
                    GameEvent.Context.of(player, level.getBlockState(eventPos.below())));
        }
        // Same as vanilla: the server-side count drops; creative mode restores it.
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
