package net.blafteam.blafcraft.effect;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SculkInfectionFog {

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MobEffectInstance instance = mc.player.getEffect(ModEffects.SCULK_INFECTION_EFFECT);
        if (instance == null) return;

        int amplifier = instance.getAmplifier();
        float far = 3.0F;

        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(far * 0.5F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MobEffectInstance instance = mc.player.getEffect(ModEffects.SCULK_INFECTION_EFFECT);
        if (instance == null) return;

        int amplifier = instance.getAmplifier();

        // Задаём цвет (RGB от 0 до 1)
        event.setRed(0.0f);
        event.setGreen(0.f);
        event.setBlue(0.0f);
    }
}
