package net.blafteam.blafcraft.effect;

import net.blafteam.blafcraft.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BleedingEffect extends MobEffect {
    protected BleedingEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        float damage = 1.0F + amplifier * 0.5F;
        entity.hurt(entity.damageSources().magic(), damage);
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.BLOOD_PARTICLES.get(),
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                    10, 0.2, 0.5, 0.2, 0.3);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 20 >> amplifier;
        if (interval > 0) {
            return duration % interval == 0;
        }
        return true;
    }
}
