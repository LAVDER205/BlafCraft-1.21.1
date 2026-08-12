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
            () -> new SlimeyEffect(MobEffectCategory.BENEFICIAL, 0x36ebab)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "slimey"), -0.25f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Holder<MobEffect> CREATION_STEP_EFFECT = MOB_EFFECTS.register("creation_step",
            () -> new CreationStepEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF));

    public static final Holder<MobEffect> BLOODLUST_EFFECT = MOB_EFFECTS.register("bloodlust",
            () -> new BloodlustEffect(MobEffectCategory.BENEFICIAL, 0x8B0000)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "bloodlust"), 0.5f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Holder<MobEffect> OVERDOSE_EFFECT = MOB_EFFECTS.register("overdose",
            () -> new OverdoseEffect(MobEffectCategory.NEUTRAL, 0x35B554));

    public static final Holder<MobEffect> BLEEDING_EFFECT = MOB_EFFECTS.register("bleeding",
            () -> new BleedingEffect(MobEffectCategory.HARMFUL, 0x8B0000));

    public static final Holder<MobEffect> POTION_SICKNESS_EFFECT = MOB_EFFECTS.register("potion_sickness",
            () -> new PotionSicknessEffect(MobEffectCategory.HARMFUL, 0x819987)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "potion_sickness"), -0.1f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Holder<MobEffect> FIERY_TOUCH_EFFECT = MOB_EFFECTS.register("fiery_touch",
            () -> new FieryTouchEffect(MobEffectCategory.BENEFICIAL, 0xED4A2B));

    public static final Holder<MobEffect> TIME_BOMB_EFFECT = MOB_EFFECTS.register("time_bomb",
            () -> new TimeBombEffect(MobEffectCategory.HARMFUL, 0x000000));

    public static final Holder<MobEffect> SCULK_INFECTION_EFFECT = MOB_EFFECTS.register("sculk_infection",
            () -> new SculkInfectionEffect(MobEffectCategory.NEUTRAL, 0x1A1266)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "sculk_infection"), 0.2f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.MAX_HEALTH ,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "sculk_infection"), 0.2f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE,
                            ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "sculk_infection"), 0.2f,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Holder<MobEffect> FREE_FLIGHT_EFFECT = MOB_EFFECTS.register("free_flight",
            () -> new FreeFlightEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF));

    public static final Holder<MobEffect> SCULK_MARK_EFFECT = MOB_EFFECTS.register("sculk_mark",
            () -> new SculkMarkEffect(MobEffectCategory.HARMFUL, 0x1A1266));

    public static final Holder<MobEffect> QUICK_ATTACK_EFFECT = MOB_EFFECTS.register("quick_attack",
            () -> new QuickAttackEffect(MobEffectCategory.BENEFICIAL, 0xEBB586)
                    .addAttributeModifier(Attributes.ATTACK_SPEED,
                    ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "quick_attack"), 0.2f,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Holder<MobEffect> MORPH_EFFECT = MOB_EFFECTS.register("morph",
            () -> new MorphEffect(MobEffectCategory.NEUTRAL, 0x1A1266).addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "morph"), -0.45f,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));


    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
