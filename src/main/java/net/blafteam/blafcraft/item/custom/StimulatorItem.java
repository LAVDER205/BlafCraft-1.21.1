package net.blafteam.blafcraft.item.custom;

import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.potion.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

public class StimulatorItem extends Item {
    public StimulatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {

            if (stack.is(ModItems.STIMULATOR)) {
                ItemStack offhandStack = player.getOffhandItem();
                if (offhandStack.getItem() == Items.POTION) {
                    PotionContents contents = offhandStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                    Item resultItem = null;

                    if (contents.is(Potions.SWIFTNESS)) {
                        resultItem = ModItems.STIMULATOR_SPEED.get();
                    } else if (contents.is(Potions.STRENGTH)) {
                        resultItem = ModItems.STIMULATOR_STRENGTH.get();
                    } else if (contents.is(Potions.REGENERATION)) {
                        resultItem = ModItems.STIMULATOR_REGENERATION.get();
                    } else if (contents.is(Potions.LEAPING)) {
                        resultItem = ModItems.STIMULATOR_JUMP.get();
                    } else if (contents.is(ModPotions.RESISTANCE_POTION)) {
                        resultItem = ModItems.STIMULATOR_RESISTANCE.get();
                    }

                    if (resultItem != null) {
                        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));

                        return InteractionResultHolder.consume(new ItemStack(resultItem, 1));
                    }
                }
            } else if (!player.hasEffect(ModEffects.OVERDOSE_EFFECT) && !player.hasEffect(ModEffects.POTION_SICKNESS_EFFECT)) {
                if (stack.is(ModItems.STIMULATOR_SPEED)) {
                    player.setItemInHand(hand, new ItemStack(ModItems.STIMULATOR.get()));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 4));
                    player.addEffect(new MobEffectInstance(ModEffects.OVERDOSE_EFFECT, 6000, 0));
                } else if (stack.is(ModItems.STIMULATOR_STRENGTH)) {
                    player.setItemInHand(hand, new ItemStack(ModItems.STIMULATOR.get()));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 4));
                    player.addEffect(new MobEffectInstance(ModEffects.OVERDOSE_EFFECT, 6000, 0));
                } else if (stack.is(ModItems.STIMULATOR_REGENERATION)) {
                    player.setItemInHand(hand, new ItemStack(ModItems.STIMULATOR.get()));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6000, 4));
                    player.addEffect(new MobEffectInstance(ModEffects.OVERDOSE_EFFECT, 6000, 0));
                } else if (stack.is(ModItems.STIMULATOR_JUMP)) {
                    player.setItemInHand(hand, new ItemStack(ModItems.STIMULATOR.get()));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 6000, 4));
                    player.addEffect(new MobEffectInstance(ModEffects.OVERDOSE_EFFECT, 6000, 0));
                } else if (stack.is(ModItems.STIMULATOR_RESISTANCE)) {
                    player.setItemInHand(hand, new ItemStack(ModItems.STIMULATOR.get()));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 4));
                    player.addEffect(new MobEffectInstance(ModEffects.OVERDOSE_EFFECT, 6000, 0));
                }
            }

        }
        return InteractionResultHolder.pass(stack);
    }
}