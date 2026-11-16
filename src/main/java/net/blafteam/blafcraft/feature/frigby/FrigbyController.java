package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.character.CharacterAssignments;
import net.blafteam.blafcraft.character.CharacterKit;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.sound.AbilitySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class FrigbyController {
    private static final String PROTECTED_SHOOTER = "blafcraft_frigby_protected_shooter";

    private record PendingArrow(UUID playerId, ServerLevel level, long fireAt) {}

    private static final List<PendingArrow> pendingArrows = new ArrayList<>();

    private FrigbyController() {}

    public static boolean isFrigby(ServerPlayer player) {
        return CharacterAssignments.get(player) == CharacterKit.FRIGBY;
    }

    public static boolean cast(ServerPlayer player, ActionType action) {
        if (!isFrigby(player)) return false;
        return switch (action) {
            case ARROW -> castArrowVolley(player);
            case FIREBALL -> castFireball(player);
            case CREATION_STEP -> toggleCreationStep(player);
            default -> false;
        };
    }

    private static boolean castArrowVolley(ServerPlayer player) {
        if (!shootArrow(player)) return false;
        // Весь залп оплачивается сразу. Следующие стрелы летят по текущему взгляду игрока.
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        pendingArrows.add(new PendingArrow(player.getUUID(), level, now + FrigbyBalance.ARROW_INTERVAL_TICKS));
        pendingArrows.add(new PendingArrow(player.getUUID(), level, now + 2L * FrigbyBalance.ARROW_INTERVAL_TICKS));
        return true;
    }

    private static boolean toggleCreationStep(ServerPlayer player) {
        if (player.hasEffect(ModEffects.CREATION_STEP_EFFECT)) {
            if (!player.removeEffect(ModEffects.CREATION_STEP_EFFECT)) return false;
            AbilitySounds.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.55F, 0.7F);
            return true;
        }
        // Включение бесплатно. Опыт списывается по мере использования опоры.
        if (!player.addEffect(new MobEffectInstance(ModEffects.CREATION_STEP_EFFECT, -1, 0, false, false, false))) {
            return false;
        }
        AbilitySounds.play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.65F, 1.35F);
        return true;
    }

    private static boolean castFireball(ServerPlayer player) {
        LargeFireball fireball = EntityType.FIREBALL.create(player.level());
        if (fireball == null) return false;
        fireball.setOwner(player);
        fireball.setPos(player.getEyePosition());
        Vec3 look = player.getViewVector(1.0F);
        fireball.shoot(look.x, look.y, look.z, 1.5f, 0.0f);

        BlockPos below = player.blockPosition().below();
        BlockPos twoBelow = below.below();
        BlockState stateBelow = player.level().getBlockState(below);
        BlockState stateTwoBelow = player.level().getBlockState(twoBelow);
        boolean boosting = player.getXRot() >= 60 && (!stateBelow.getCollisionShape(player.level(), below).isEmpty()
                || !stateTwoBelow.getCollisionShape(player.level(), twoBelow).isEmpty());
        if (boosting) fireball.getPersistentData().putUUID(PROTECTED_SHOOTER, player.getUUID());
        if (!player.level().addFreshEntity(fireball)) return false;

        AbilitySounds.play(player, SoundEvents.BLAZE_SHOOT, 0.65F, 1.1F);
        if (boosting) {
            AbilitySounds.play(player, SoundEvents.WIND_CHARGE_THROW, 0.6F, 1.25F);
            player.addDeltaMovement(new Vec3(0, 1, 0));
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        return true;
    }

    private static boolean shootArrow(ServerPlayer player) {
        Arrow arrow = EntityType.ARROW.create(player.level());
        if (arrow == null) return false;
        arrow.setOwner(player);
        arrow.getPersistentData().putUUID(PROTECTED_SHOOTER, player.getUUID());
        arrow.setPos(player.getEyePosition());
        Vec3 look = player.getViewVector(1.0F);
        arrow.shoot(look.x, look.y, look.z, 3.0f, 0.0f);
        if (!player.level().addFreshEntity(arrow)) return false;
        AbilitySounds.play(player, SoundEvents.ARROW_SHOOT, 0.5F, 1.25F);
        return true;
    }

    @SubscribeEvent
    public static void protectShooter(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var projectile = event.getSource().getDirectEntity();
        if (!(projectile instanceof Arrow) && !(projectile instanceof LargeFireball)) return;

        // Защита действует только для этого снаряда и только на выпустившего его игрока.
        // Стрела может отскочить от щита или цели, уже получившей другой удар.
        var data = projectile.getPersistentData();
        if (data.hasUUID(PROTECTED_SHOOTER) && data.getUUID(PROTECTED_SHOOTER).equals(player.getUUID())) {
            event.setCanceled(true);
            if (projectile instanceof Arrow) projectile.discard();
        }
    }

    @SubscribeEvent
    public static void firePendingArrows(ServerTickEvent.Post event) {
        var iterator = pendingArrows.iterator();
        while (iterator.hasNext()) {
            PendingArrow shot = iterator.next();
            if (shot.level().getGameTime() < shot.fireAt()) continue;

            iterator.remove();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(shot.playerId());
            if (player != null && player.isAlive() && player.serverLevel() == shot.level() && isFrigby(player)) {
                shootArrow(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancelPendingArrows(event.getEntity().getUUID());
    }

    public static void cancelPendingArrows(UUID playerId) {
        // После завершения дуэли оставшиеся стрелы не должны вылететь.
        pendingArrows.removeIf(shot -> shot.playerId().equals(playerId));
        FrigbyConstructions.cancelPending(playerId);
    }
}
