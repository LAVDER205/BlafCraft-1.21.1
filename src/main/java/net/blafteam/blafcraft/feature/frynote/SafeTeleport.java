package net.blafteam.blafcraft.feature.frynote;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SafeTeleport {
    private SafeTeleport() {}

    public static boolean isSafe(ServerPlayer player, Vec3 destination, boolean needsSupport) {
        var level = player.serverLevel();
        AABB bounds = player.getBoundingBox().move(destination.subtract(player.position()));
        if (bounds.minY < level.getMinBuildHeight() || bounds.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(bounds)
                || !hasLoadedChunks(level, bounds)
                || !level.noCollision(player, bounds)) return false;
        // Проверяем весь объём игрока и блок под ногами, а не только одну клетку.
        if (level.getBlockStates(bounds.inflate(0, 0.05, 0)).anyMatch(state ->
                state.getFluidState().is(FluidTags.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                        || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW))) return false;
        BlockPos below = BlockPos.containing(destination.add(0, -0.05, 0));
        var support = level.getBlockState(below);
        if (support.is(Blocks.MAGMA_BLOCK) || support.is(Blocks.CAMPFIRE) || support.is(Blocks.SOUL_CAMPFIRE)) return false;
        return !needsSupport || !support.getCollisionShape(level, below).isEmpty();
    }

    private static boolean hasLoadedChunks(ServerLevel level, AABB bounds) {
        int minX = BlockPos.containing(bounds.minX, 0, bounds.minZ).getX() >> 4;
        int minZ = BlockPos.containing(bounds.minX, 0, bounds.minZ).getZ() >> 4;
        int maxX = BlockPos.containing(bounds.maxX, 0, bounds.maxZ).getX() >> 4;
        int maxZ = BlockPos.containing(bounds.maxX, 0, bounds.maxZ).getZ() >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    public static Vec3 dashDestination(ServerPlayer player) {
        Vec3 origin = player.position();
        Vec3 look = dashDirection(player.getLookAngle(), player.getYRot(), player.onGround());
        // Можно пересечь стену, но конечный объём обязан быть свободным.
        // Если полные 5 блоков недоступны, ищем более близкую точку до 1 блока.
        for (double distance = FrynoteBalance.DASH_DISTANCE; distance >= 1; distance -= 0.25) {
            Vec3 point = origin.add(look.scale(distance));
            if (isSafe(player, point, false)) return point;
        }
        return null;
    }

    static Vec3 dashDirection(Vec3 look, float yaw, boolean onGround) {
        // На земле взгляд вниз означает движение вдоль поверхности, иначе все
        // проверяемые точки окажутся под полом. В воздухе сохраняем вертикальный рывок.
        if (onGround && look.y < 0) {
            Vec3 horizontal = new Vec3(look.x, 0, look.z);
            return horizontal.lengthSqr() > 0.0001 ? horizontal.normalize() : Vec3.directionFromRotation(0, yaw);
        }
        return look;
    }

    public static Vec3 glanceDestination(ServerPlayer player) {
        HitResult result = player.pick(FrynoteBalance.GLANCE_DISTANCE, 1, false);
        if (!(result instanceof BlockHitResult hit) || result.getType() != HitResult.Type.BLOCK) return null;
        BlockPos[] candidates = {hit.getBlockPos().relative(hit.getDirection()), hit.getBlockPos().above()};
        for (BlockPos candidate : candidates) {
            Vec3 point = Vec3.atBottomCenterOf(candidate);
            if (point.distanceToSqr(player.position()) <= FrynoteBalance.GLANCE_DISTANCE * FrynoteBalance.GLANCE_DISTANCE
                    && isSafe(player, point, true)) return point;
        }
        return null;
    }

    public static boolean move(ServerPlayer player, Vec3 destination) {
        if (destination == null || destination.distanceToSqr(player.position()) < 0.01) return false;
        // Абсолютные углы сохраняют текущий взгляд и при возврате к якорю.
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        if (player.position().distanceToSqr(destination) > 0.01) return false;
        player.fallDistance = 0;
        return true;
    }
}
