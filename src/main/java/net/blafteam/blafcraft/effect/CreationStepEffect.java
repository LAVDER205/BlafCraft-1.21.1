package net.blafteam.blafcraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Отмечает включённый созидающий шаг. Движение и расход опыта обрабатывает CreationStep. */
public class CreationStepEffect extends MobEffect {
    protected CreationStepEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
