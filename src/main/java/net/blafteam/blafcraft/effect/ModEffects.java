package net.blafteam.blafcraft.effect;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, BlafCraft.MODID);

    public static final Holder<MobEffect> SLIMEY_EFFECT = MOB_EFFECTS.register("slimey",
            () -> new SlimeyEffect(MobEffectCategory.NEUTRAL, 0x36ebab)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "slimey"), -0.25f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Holder<MobEffect> CREATION_STEP_EFFECT = MOB_EFFECTS.register("creation_step",
            () -> new CreationStepEffect(MobEffectCategory.NEUTRAL, 0xFFFFFF));

    public static final Holder<MobEffect> BLOODLUST_EFFECT = MOB_EFFECTS.register("bloodlust",
            () -> new BloodlustEffect(MobEffectCategory.NEUTRAL, 0x8B0000)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "bloodlust"), 0.5f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Holder<MobEffect> OVERDOSE_EFFECT = MOB_EFFECTS.register("overdose",
            () -> new OverdoseEffect(MobEffectCategory.NEUTRAL, 0x5BB56E));

    public static final Holder<MobEffect> BLEEDING_EFFECT = MOB_EFFECTS.register("bleeding",
            () -> new BleedingEffect(MobEffectCategory.HARMFUL, 0x8B0000));


    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
