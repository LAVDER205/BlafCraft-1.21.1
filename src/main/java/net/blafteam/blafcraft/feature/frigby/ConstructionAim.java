package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.keybinds.ActionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

/** Одна геометрия для приватного предпросмотра и серверной проверки места. */
public record ConstructionAim(ActionType action, BlockPos anchor, Direction facing, List<BlockPos> blocks) {
    public static boolean needsAim(ActionType action) {
        return action == ActionType.STONE_WALL || action == ActionType.ARROW_RAIN || action == ActionType.CANNON;
    }

    public static ConstructionAim fromLook(Player player, ActionType action) {
        if (!needsAim(action)) return null;
        double range = action == ActionType.ARROW_RAIN ? FrigbyBalance.RAIN_RANGE : FrigbyBalance.BUILD_RANGE;
        if (!(player.pick(range, 1, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos anchor = hit.getBlockPos().above();
        Direction facing = player.getDirection();
        if (action == ActionType.STONE_WALL) {
            return new ConstructionAim(action, anchor, facing, wallBlocks(anchor, facing));
        }
        return new ConstructionAim(action, anchor, facing, action == ActionType.CANNON ? List.of(anchor) : List.of());
    }

    static List<BlockPos> wallBlocks(BlockPos anchor, Direction facing) {
        Direction sideways = facing.getClockWise();
        List<BlockPos> blocks = new ArrayList<>(6);
        for (int column = -1; column <= 1; column++) {
            BlockPos base = anchor.relative(sideways, column);
            blocks.add(base);
            blocks.add(base.above());
        }
        return List.copyOf(blocks);
    }

    public boolean isClear(Player player) {
        Level level = player.level();
        if (!mayUse(level, player, anchor)) return false;
        if (action == ActionType.ARROW_RAIN) return true;
        for (BlockPos pos : blocks) {
            if (!mayUse(level, player, pos) || !level.isEmptyBlock(pos)) return false;
            if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), living -> living.isAlive() && !living.isSpectator()).isEmpty()) return false;
        }
        // У пушки должно быть свободно и у дула.
        if (action == ActionType.CANNON) {
            BlockPos muzzle = anchor.relative(facing);
            if (!mayUse(level, player, muzzle)
                    || !level.getBlockState(muzzle).getCollisionShape(level, muzzle).isEmpty()) return false;
        }
        return true;
    }

    private static boolean mayUse(Level level, Player player, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(player, pos);
    }

    static List<Vec3> rainBoundary(Level level, Vec3 center) {
        List<Vec3> points = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12;
            Vec3 point = center.add(Math.cos(angle) * FrigbyBalance.RAIN_RADIUS, 0,
                    Math.sin(angle) * FrigbyBalance.RAIN_RADIUS);
            if (!level.hasChunkAt(BlockPos.containing(point))) continue;
            // Край прицела следует за ближайшей поверхностью на неровной площадке.
            var ground = level.clip(new ClipContext(point.add(0, 2, 0), point.add(0, -3, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (ground.getType() == HitResult.Type.BLOCK) point = ground.getLocation();
            points.add(point.add(0, 0.12, 0));
        }
        return points;
    }

    public Vec3 groundCenter() {
        return Vec3.atBottomCenterOf(anchor);
    }
}
