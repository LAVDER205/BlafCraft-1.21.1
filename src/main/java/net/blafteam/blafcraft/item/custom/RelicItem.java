package net.blafteam.blafcraft.item.custom;

import net.blafteam.blafcraft.feature.frigby.FrigbyBalance;
import net.blafteam.blafcraft.feature.frigby.FrigbyController;
import net.blafteam.blafcraft.feature.frigby.RelicCharge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class RelicItem extends Item {
    private static final int USE_DURATION = 72_000;

    public RelicItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (!FrigbyController.isFrigby(serverPlayer) || player.isSpectator()) {
                player.displayClientMessage(Component.literal("Использовать реликвию может только Фригби"), true);
                return InteractionResultHolder.fail(stack);
            }
            if (RelicCharge.points(serverPlayer) == 0) {
                player.displayClientMessage(Component.literal("Реликвия пока не накопила опыт"), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!FrigbyController.isFrigby(player) || player.isSpectator()) {
            player.stopUsingItem();
            return;
        }

        int elapsed = USE_DURATION - remainingUseDuration;
        int transferTicks = elapsed - FrigbyBalance.RELIC_STARTUP_TICKS;
        if (transferTicks < FrigbyBalance.RELIC_TRANSFER_INTERVAL
                || transferTicks % FrigbyBalance.RELIC_TRANSFER_INTERVAL != 0) return;

        // Списываем по очку. После отпускания кнопки остаток остаётся у игрока.
        RelicCharge.transferPoint(player);
        if (RelicCharge.points(player) == 0) player.stopUsingItem();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
