package net.blafteam.blafcraft.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RamEffect extends MobEffect {
    private static final double MIN_SPEED_SQ = 0.0001; // 0.01 blocks/tick
    private static final double RAM_DAMAGE = 4.0;
    private static final double KNOCKBACK_STRENGTH = 0.8;

    protected RamEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return false;


        Vec3 motion = player.getDeltaMovement();
        double horizontalSpeedSq = motion.x * motion.x + motion.z * motion.z;
        if (horizontalSpeedSq < MIN_SPEED_SQ) {
            player.removeEffect(ModEffects.RAM_EFFECT);
            return false;
        }


        AABB hitbox = player.getBoundingBox().inflate(0.5);
        Set<UUID> hitThisTick = new HashSet<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, hitbox, e -> e != player)) {
            if (!hitThisTick.contains(target.getUUID())) {
                target.hurt(player.damageSources().playerAttack(player), (float) RAM_DAMAGE);

                Vec3 direction = motion.normalize();
                target.knockback(KNOCKBACK_STRENGTH, -direction.x, -direction.z);
                hitThisTick.add(target.getUUID());
            }
        }

        return true;
    }
}
