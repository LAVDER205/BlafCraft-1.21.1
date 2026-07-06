package net.blafteam.blafcraft.potion;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(BuiltInRegistries.POTION, BlafCraft.MODID);

    public static final Holder<Potion> SLIMEY_POTION = POTIONS.register("slimey_potion",
            () -> new Potion(new MobEffectInstance(ModEffects.SLIMEY_EFFECT, 3600, 0)));

    public static final Holder<Potion> FIERY_TOUCH_POTION = POTIONS.register("fiery_touch_potion",
            () -> new Potion(new MobEffectInstance(ModEffects.FIERY_TOUCH_EFFECT, 3600, 0)));
    public static final Holder<Potion> STRONG_FIERY_TOUCH_POTION = POTIONS.register("strong_fiery_touch_potion",
            () -> new Potion(new MobEffectInstance(ModEffects.FIERY_TOUCH_EFFECT, 1800, 1)));
    public static final Holder<Potion> LONG_FIERY_TOUCH_POTION = POTIONS.register("long_fiery_touch_potion",
            () -> new Potion(new MobEffectInstance(ModEffects.FIERY_TOUCH_EFFECT, 9600, 0)));

    public static final Holder<Potion> RESISTANCE_POTION = POTIONS.register("resistance_potion",
            () -> new Potion(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 0)));
    public static final Holder<Potion> STRONG_RESISTANCE_POTION = POTIONS.register("strong_resistance_potion",
            () -> new Potion(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1800, 1)));
    public static final Holder<Potion> LONG_RESISTANCE_TOUCH_POTION = POTIONS.register("long_resistance_potion",
            () -> new Potion(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 9600, 0)));

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
