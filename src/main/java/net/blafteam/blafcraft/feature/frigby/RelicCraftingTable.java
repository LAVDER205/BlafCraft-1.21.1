package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.item.ModItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class RelicCraftingTable {
    private RelicCraftingTable() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorkbenchUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || event.getHand() != InteractionHand.MAIN_HAND || event.getUseBlock().isFalse()
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.CRAFTING_TABLE)) return;

        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        var level = event.getLevel();
        var pos = event.getPos();
        if (event.getUseBlock().isDefault() && player.isSecondaryUseActive() && (!main.isEmpty() || !off.isEmpty())
                && !(main.doesSneakBypassUse(level, pos, player) && off.doesSneakBypassUse(level, pos, player))) return;

        // Сохраняем приоритет предметов, которые обрабатывают ПКМ раньше блока.
        if (!event.getUseItem().isFalse()) {
            InteractionResult itemResult = main.onItemUseFirst(new UseOnContext(player, event.getHand(), event.getHitVec()));
            if (itemResult != InteractionResult.PASS) {
                event.setCancellationResult(itemResult);
                event.setCanceled(true);
                return;
            }
        }

        // Обычное меню и экран верстака. На сервере добавляем игрока в контекст рецепта.
        ContainerLevelAccess access = ContainerLevelAccess.create(level, pos);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, owner) -> new WorkbenchMenu(id, inventory, access),
                Component.translatable("container.crafting")));
        player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        CriteriaTriggers.DEFAULT_BLOCK_USE.trigger(player, pos);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    private static final class WorkbenchMenu extends CraftingMenu {
        private final ServerPlayer owner;

        private WorkbenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
            owner = (ServerPlayer) inventory.player;
        }

        @Override
        public void slotsChanged(Container inventory) {
            Player previous = CommonHooks.getCraftingPlayer();
            CommonHooks.setCraftingPlayer(owner);
            try {
                super.slotsChanged(inventory);
            } finally {
                CommonHooks.setCraftingPlayer(previous);
            }
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            // Членство могло измениться после появления предмета в ячейке результата.
            if (getSlot(RESULT_SLOT).getItem().is(ModItems.RELIC.get()) && !RelicRecipe.canCraft(owner)) {
                getSlot(RESULT_SLOT).set(ItemStack.EMPTY);
                broadcastChanges();
            }
            super.clicked(slotId, button, clickType, player);
        }
    }
}
