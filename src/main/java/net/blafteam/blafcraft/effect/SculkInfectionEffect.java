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
import java.util.Objects;

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
            double radius_of_mark = 50.0;
            double radius_of_visibility = 3.6 * (1 + amplifier);
            AABB area = new AABB(x - radius_of_mark, y - radius_of_mark, z - radius_of_mark,
                    x + radius_of_mark, y + radius_of_mark, z + radius_of_mark);
            List<LivingEntity> entitiesInRange = level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.position().distanceToSqr(x, y, z) <= radius_of_mark * radius_of_mark);

            for (LivingEntity entity : entitiesInRange) {
                if (!serverPlayer.equals(entity)) {
                    double distanceSqr = serverPlayer.distanceToSqr(entity);
                    if (!HighlightManager.isHighlighted(serverPlayer, entity) && entity.hasEffect(ModEffects.SCULK_MARK_EFFECT) && Objects.requireNonNull(entity.getEffect(ModEffects.SCULK_MARK_EFFECT)).getAmplifier() == 0) { // normal sculk mark
                        HighlightManager.highlight(serverPlayer, entity, 1, 0, 0);
                    } else if (HighlightManager.isHighlighted(serverPlayer, entity) && distanceSqr <= radius_of_visibility * radius_of_visibility && !entity.hasEffect(ModEffects.SCULK_MARK_EFFECT)) { // if entity have red highlight active but no mark
                        HighlightManager.unhighlight(serverPlayer, entity);
                        HighlightManager.highlight(serverPlayer, entity, 1, 1, 1);
                    } else if (!HighlightManager.isHighlighted(serverPlayer, entity) && distanceSqr <= radius_of_visibility * radius_of_visibility) { // if not highlighted but in radius of visibility
                        HighlightManager.highlight(serverPlayer, entity, 1, 1, 1);
                        HighlightManager.scheduleUnhighlightWithUpdate(serverPlayer, entity, 20);
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
