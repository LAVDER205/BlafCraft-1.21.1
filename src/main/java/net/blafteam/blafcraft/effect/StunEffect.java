package net.blafteam.blafcraft.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class StunEffect extends MobEffect {
    protected StunEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        if (entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false, false));
        }
    }
}
