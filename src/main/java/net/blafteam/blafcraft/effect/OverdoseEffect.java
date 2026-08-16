package net.blafteam.blafcraft.effect;

import net.blafteam.blafcraft.sound.ClientLoopingSoundManager;
import net.blafteam.blafcraft.sound.LoopingSoundPayload;
import net.blafteam.blafcraft.sound.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class OverdoseEffect extends MobEffect {

    protected OverdoseEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player, new LoopingSoundPayload(ModSounds.OVERDOSE.get(), 1.4f, 1.0f, true));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
