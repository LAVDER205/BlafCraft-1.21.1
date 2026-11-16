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
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class FrigbyConstructions {
    private static final String CANNON_SHOT = "blafcraft_frigby_cannon";
    private static final List<Rain> rains = new ArrayList<>();
    private static final List<Cannon> cannons = new ArrayList<>();

    private static final class Rain {
        final ServerLevel level;
        final UUID caster;
        final Vec3 center;
        long nextWave;
        int wavesLeft = FrigbyBalance.RAIN_WAVES;

        Rain(ServerLevel level, UUID caster, Vec3 center, long nextWave) {
            this.level = level;
            this.caster = caster;
            this.center = center;
            this.nextWave = nextWave;
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
        if (now < rain.nextWave) {
            if (rain.wavesLeft == FrigbyBalance.RAIN_WAVES && now % 5 == 0) warningCircle(rain.level, rain.center);
            return false;
        }
        spawnRainWave(rain, caster);
        rain.nextWave = now + FrigbyBalance.RAIN_WAVE_INTERVAL;
        return --rain.wavesLeft <= 0;
    }

    private static void warningCircle(ServerLevel level, Vec3 center) {
        for (Vec3 point : ConstructionAim.rainBoundary(level, center)) {
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnRainWave(Rain rain, ServerPlayer caster) {
        AbilitySounds.play(rain.level, rain.center, SoundEvents.ARROW_SHOOT, 0.7F, 1.3F);
        int wave = FrigbyBalance.RAIN_WAVES - rain.wavesLeft;
        for (int i = 0; i < FrigbyBalance.RAIN_ARROWS_PER_WAVE; i++) {
            // Стрела в центре каждой волны, остальные расходятся по двум кольцам.
            double angle = (i + wave / 3.0) * Math.TAU / (FrigbyBalance.RAIN_ARROWS_PER_WAVE - 1);
            double radius = i == 0 ? 0 : FrigbyBalance.RAIN_RADIUS * (0.5 + (i % 2) * 0.35);
            double x = rain.center.x + Math.cos(angle) * radius;
            double z = rain.center.z + Math.sin(angle) * radius;
            BlockPos column = BlockPos.containing(x, rain.center.y, z);
            if (!rain.level.hasChunkAt(column) || !rain.level.getWorldBorder().isWithinBounds(column)) continue;

            // Спавним над крышей, чтобы она действительно закрывала цели от стрел.
            int roof = rain.level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
            double y = Math.max(rain.center.y + 8, roof + 1);
            if (y > rain.center.y + 24 || y >= rain.level.getMaxBuildHeight()) continue;
            var arrow = EntityType.ARROW.create(rain.level);
            if (arrow == null) continue;
            arrow.setOwner(caster);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setPos(x, y, z);
            arrow.shoot(0, -1, 0, 2.0F, 0);
            rain.level.addFreshEntity(arrow);
        }
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
        shot.getPersistentData().putBoolean(CANNON_SHOT, true);
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
        Vec3 hit = event.getRayTraceResult().getLocation();
        // Взрыв с уроном и отбрасыванием, без огня и разрушения блоков.
        level.explode(shot, hit.x, hit.y, hit.z, FrigbyBalance.CANNON_POWER, false, Level.ExplosionInteraction.NONE);
        shot.discard();
    }

    @SubscribeEvent
    public static void expireCannonShot(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LargeFireball shot && !shot.level().isClientSide
                && shot.tickCount >= FrigbyBalance.CANNON_SHOT_TICKS && shot.getPersistentData().getBoolean(CANNON_SHOT)) {
            shot.discard();
        }
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
