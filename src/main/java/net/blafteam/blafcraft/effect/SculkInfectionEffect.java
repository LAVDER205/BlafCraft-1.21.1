package net.blafteam.blafcraft.effect;

import net.blafteam.blafcraft.highlight.HighlightManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SculkInfectionEffect extends MobEffect {
    protected SculkInfectionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof ServerPlayer serverPlayer) {
            double x = livingEntity.getX();
            double y = livingEntity.getY();
            double z = livingEntity.getZ();
            Level level = livingEntity.level();
            double radius_of_list = 100.0;
            double radius_of_visibility = 3.6;
            AABB area = new AABB(x - radius_of_list, y - radius_of_list, z - radius_of_list,
                    x + radius_of_list, y + radius_of_list, z + radius_of_list);
            List<LivingEntity> entitiesInRange = level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.position().distanceToSqr(x, y, z) <= radius_of_list * radius_of_list);

            for (LivingEntity entity : entitiesInRange) {
                if (!serverPlayer.equals(entity)) {
                    double distanceSqr = serverPlayer.distanceToSqr(entity);
                    if (HighlightManager.isHighlighted(serverPlayer, entity) && distanceSqr > radius_of_visibility * radius_of_visibility) {
                        HighlightManager.scheduleUnhighlight(serverPlayer, entity, 20);
                    } else if (!HighlightManager.isHighlighted(serverPlayer, entity) && distanceSqr <= radius_of_visibility * radius_of_visibility) {
                        HighlightManager.highlight(serverPlayer, entity, 1, 1, 1);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
