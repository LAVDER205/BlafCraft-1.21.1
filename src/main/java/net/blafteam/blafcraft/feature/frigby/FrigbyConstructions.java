package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.block.ModBlocks;
import net.blafteam.blafcraft.block.custom.ConstructionBlock;
import net.blafteam.blafcraft.block.entity.ConstructionBlockEntity;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.sound.AbilitySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class FrigbyConstructions {
    private static final String CANNON_SHOT = "blafcraft_frigby_cannon";
    private static final String CANNON_ORIGIN = "blafcraft_frigby_cannon_origin";
    private static final String CANNON_FUSE = "blafcraft_frigby_cannon_fuse";
    private static final List<Rain> rains = new ArrayList<>();
    private static final List<Cannon> cannons = new ArrayList<>();

    private static final class Rain {
        final ServerLevel level;
        final UUID caster;
        final Vec3 center;
        final long startsAt;
        final List<BlockPos> columns;
        int shotCount;
        int nextColumn;

        Rain(ServerLevel level, UUID caster, Vec3 center, long startsAt) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.startsAt = startsAt;
            columns = rainColumns(BlockPos.containing(center));
            Collections.shuffle(columns, new Random(level.random.nextLong()));
        }

        BlockPos nextTarget() {
            // Периодически повторяем центр, между этими выстрелами обходим всю площадку.
            if (shotCount % 12 == 0) return BlockPos.containing(center);
            return columns.get(nextColumn++ % columns.size());
        }
    }

    private record Cannon(ServerLevel level, UUID caster, BlockPos pos, Direction facing,
                          ConstructionBlockEntity block, long firesAt) {}

    private FrigbyConstructions() {}

    public static boolean cast(ServerPlayer player, ActionType action) {
        ConstructionAim aim = ConstructionAim.fromLook(player, action);
        if (aim == null || !aim.isClear(player)) {
            player.displayClientMessage(Component.literal("Здесь нельзя создать конструкцию"), true);
            return false;
        }
        return switch (action) {
            case STONE_WALL -> wall(player, aim);
            case ARROW_RAIN -> rain(player, aim);
            case CANNON -> cannon(player, aim);
            default -> false;
        };
    }

    private static boolean wall(ServerPlayer player, ConstructionAim aim) {
        ServerLevel level = player.serverLevel();
        long expires = level.getGameTime() + FrigbyBalance.WALL_DURATION;
        List<BlockPos> placed = new ArrayList<>(6);
        for (BlockPos pos : aim.blocks()) {
            if (place(level, pos, ModBlocks.CREATION_WALL.get().defaultBlockState(), expires) == null) {
                for (BlockPos rollback : placed) level.removeBlock(rollback, false);
                return false;
            }
            placed.add(pos);
        }
        AbilitySounds.play(level, aim.groundCenter(), SoundEvents.STONE_PLACE, 0.9F, 0.8F);
        return true;
    }

    private static boolean rain(ServerPlayer player, ConstructionAim aim) {
        ServerLevel level = player.serverLevel();
        rains.add(new Rain(level, player.getUUID(), aim.groundCenter(),
                level.getGameTime() + FrigbyBalance.RAIN_WARNING_TICKS));
        warningCircle(level, aim.groundCenter());
        AbilitySounds.play(level, aim.groundCenter(), SoundEvents.CROSSBOW_LOADING_END.value(), 0.85F, 0.85F);
        return true;
    }

    private static boolean cannon(ServerPlayer player, ConstructionAim aim) {
        ServerLevel level = player.serverLevel();
        long fireAt = level.getGameTime() + FrigbyBalance.CANNON_WINDUP;
        BlockState cannon = ModBlocks.CREATION_CANNON.get().defaultBlockState().setValue(ConstructionBlock.FACING, aim.facing());
        var block = place(level, aim.anchor(), cannon, fireAt + 5);
        if (block == null) return false;
        cannons.add(new Cannon(level, player.getUUID(), aim.anchor(), aim.facing(), block, fireAt));
        AbilitySounds.play(level, aim.groundCenter(), SoundEvents.TNT_PRIMED, 0.75F, 1.25F);
        return true;
    }

    private static ConstructionBlockEntity place(ServerLevel level, BlockPos pos, BlockState state, long expires) {
        if (!level.isEmptyBlock(pos) || !level.setBlock(pos, state, Block.UPDATE_ALL)) return null;
        if (level.getBlockEntity(pos) instanceof ConstructionBlockEntity construction) {
            construction.expireAt(expires);
            return construction;
        }
        level.removeBlock(pos, false);
        return null;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        rains.removeIf(FrigbyConstructions::tickRain);
        cannons.removeIf(FrigbyConstructions::tickCannon);
    }

    private static boolean tickRain(Rain rain) {
        long now = rain.level.getGameTime();
        ServerPlayer caster = activeCaster(rain.level, rain.caster);
        if (caster == null || !rain.level.hasChunkAt(BlockPos.containing(rain.center))) return true;
        if (now % 5 == 0) warningCircle(rain.level, rain.center);
        if (now < rain.startsAt) return false;
        if (rain.shotCount % 6 == 0) AbilitySounds.play(rain.level, rain.center, SoundEvents.ARROW_SHOOT, 0.47F, 1.3F);
        spawnRainArrow(rain, caster, rain.nextTarget());
        return ++rain.shotCount >= FrigbyBalance.RAIN_DURATION_TICKS;
    }

    private static void warningCircle(ServerLevel level, Vec3 center) {
        for (Vec3 point : ConstructionAim.rainBoundary(level, center)) {
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    static List<BlockPos> rainColumns(BlockPos center) {
        int radius = FrigbyBalance.RAIN_RADIUS;
        List<BlockPos> columns = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) columns.add(center.offset(x, 0, z));
            }
        }
        return columns;
    }

    private static void spawnRainArrow(Rain rain, ServerPlayer caster, BlockPos column) {
        ServerLevel level = rain.level;
        if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) return;

        // Стрела начинается над крышей. Под слишком высокой крышей не создаём снаряд.
        int roof = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        double y = Math.max(rain.center.y + 8, roof + 1);
        if (y > rain.center.y + 24 || y >= level.getMaxBuildHeight()) return;
        var arrow = EntityType.ARROW.create(level);
        if (arrow == null) return;
        arrow.setOwner(caster);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setPos(column.getX() + 0.5, y, column.getZ() + 0.5);
        arrow.shoot(0, -1, 0, 2.0F, 0);
        level.addFreshEntity(arrow);
    }

    private static boolean tickCannon(Cannon cannon) {
        ServerLevel level = cannon.level();
        ServerPlayer caster = activeCaster(level, cannon.caster());
        if (caster == null || !level.hasChunkAt(cannon.pos())
                || level.getBlockEntity(cannon.pos()) != cannon.block()) {
            removeCannonBlock(cannon);
            return true;
        }
        Vec3 muzzle = Vec3.atCenterOf(cannon.pos()).add(Vec3.atLowerCornerOf(cannon.facing().getNormal()).scale(0.65));
        if (level.getGameTime() < cannon.firesAt()) {
            if (level.getGameTime() % 4 == 0) level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
            return false;
        }

        removeCannonBlock(cannon);
        BlockPos inFront = cannon.pos().relative(cannon.facing());
        if (!level.hasChunkAt(inFront) || !level.getBlockState(inFront).getCollisionShape(level, inFront).isEmpty()) return true;
        var shot = EntityType.FIREBALL.create(level);
        if (shot == null) return true;
        shot.setOwner(caster);
        var data = shot.getPersistentData();
        data.putBoolean(CANNON_SHOT, true);
        data.putDouble(CANNON_ORIGIN + "_x", muzzle.x);
        data.putDouble(CANNON_ORIGIN + "_y", muzzle.y);
        data.putDouble(CANNON_ORIGIN + "_z", muzzle.z);
        data.putLong(CANNON_FUSE, level.getGameTime() + FrigbyBalance.CANNON_MAX_FLIGHT_TICKS);
        shot.setPos(muzzle);
        shot.shoot(cannon.facing().getStepX(), 0, cannon.facing().getStepZ(), 1.5F, 0);
        if (level.addFreshEntity(shot)) AbilitySounds.play(level, muzzle, SoundEvents.BLAZE_SHOOT, 0.95F, 0.7F);
        return true;
    }

    private static void removeCannonBlock(Cannon cannon) {
        if (cannon.level().hasChunkAt(cannon.pos()) && cannon.level().getBlockEntity(cannon.pos()) == cannon.block()) {
            cannon.level().removeBlock(cannon.pos(), false);
        }
    }

    private static ServerPlayer activeCaster(ServerLevel level, UUID casterId) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(casterId);
        return player != null && player.isAlive() && player.serverLevel() == level && FrigbyController.isFrigby(player) ? player : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCannonImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof LargeFireball shot) || !(shot.level() instanceof ServerLevel level)
                || !shot.getPersistentData().getBoolean(CANNON_SHOT)) return;
        event.setCanceled(true);
        detonate(level, shot, capBlastPosition(shotOrigin(shot), event.getRayTraceResult().getLocation()));
    }

    @SubscribeEvent
    public static void checkCannonFuse(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LargeFireball shot) || shot.isRemoved()
                || !(shot.level() instanceof ServerLevel level) || !shot.getPersistentData().getBoolean(CANNON_SHOT)) return;
        Vec3 origin = shotOrigin(shot);
        Vec3 current = shot.position();
        if (origin.distanceToSqr(current) >= FrigbyBalance.CANNON_SHOT_RANGE * FrigbyBalance.CANNON_SHOT_RANGE
                || level.getGameTime() >= shot.getPersistentData().getLong(CANNON_FUSE)) {
            detonate(level, shot, capBlastPosition(origin, current));
        }
    }

    private static Vec3 shotOrigin(LargeFireball shot) {
        var data = shot.getPersistentData();
        if (!data.contains(CANNON_ORIGIN + "_x")) return shot.position(); // Снаряд остался с предыдущей сборки.
        return new Vec3(data.getDouble(CANNON_ORIGIN + "_x"), data.getDouble(CANNON_ORIGIN + "_y"),
                data.getDouble(CANNON_ORIGIN + "_z"));
    }

    static Vec3 capBlastPosition(Vec3 origin, Vec3 point) {
        // За тик снаряд может пересечь границу дальности. Взрыв остаётся на границе.
        Vec3 offset = point.subtract(origin);
        double range = FrigbyBalance.CANNON_SHOT_RANGE;
        return offset.lengthSqr() <= range * range ? point : origin.add(offset.normalize().scale(range));
    }

    private static void detonate(ServerLevel level, LargeFireball shot, Vec3 center) {
        if (shot.isRemoved()) return;
        // Урон и отбрасывание остаются ванильными. Крупная вспышка не требует разрушать блоки.
        level.explode(shot, center.x, center.y, center.z, FrigbyBalance.CANNON_POWER, false, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        shot.discard();
    }

    public static void cancelPending(UUID caster) {
        rains.removeIf(rain -> rain.caster.equals(caster));
        cannons.removeIf(cannon -> {
            if (!cannon.caster().equals(caster)) return false;
            removeCannonBlock(cannon);
            return true;
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        rains.clear();
        cannons.clear();
    }
}
