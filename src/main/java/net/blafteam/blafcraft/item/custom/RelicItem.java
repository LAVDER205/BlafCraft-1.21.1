package net.blafteam.blafcraft.item.custom;

import net.blafteam.blafcraft.component.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RelicItem extends Item {
    public RelicItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && player.getDisplayName().getString().equals("Dev")) {
            if (!stack.has(ModDataComponents.COUNTER)) stack.set(ModDataComponents.COUNTER, 0);
            else {
                stack.set(ModDataComponents.COUNTER, stack.get(ModDataComponents.COUNTER) + 1);
                if (stack.get(ModDataComponents.COUNTER) >= 20) {
                    stack.set(ModDataComponents.COUNTER, 0);
                    int expAmount = (int) (5 * (-0.000716846 * player.totalExperience + 1));
                    if (expAmount > 0) player.giveExperiencePoints(expAmount);
                }
            }
        }
    }
}
