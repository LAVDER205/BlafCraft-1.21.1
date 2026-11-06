package net.blafteam.blafcraft.social;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class FactionBook {
    private static final String MARKER = "blafcraft_faction_book";

    private FactionBook() {}

    @SubscribeEvent
    public static void create(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !event.getItemStack().is(Items.BOOK)) return;
        if (!SocialData.get(player.serverLevel().getServer()).isLeader(player.getUUID())) return;

        ItemStack input = event.getItemStack();
        ItemStack book = createBook();
        // Меняем одну обычную книгу. Остальные книги в стопке сохраняются.
        if (input.getCount() == 1) {
            player.setItemInHand(event.getHand(), book);
        } else {
            input.shrink(1);
            if (!player.getInventory().add(book)) player.drop(book, false);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void invite(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Player) || !isFactionBook(event.getItemStack())) return;
        if (event.getEntity() instanceof ServerPlayer leader && event.getTarget() instanceof ServerPlayer target) {
            SocialInteractions.invite(leader, target, SocialData.InviteKind.FACTION);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean isFactionBook(ItemStack stack) {
        return stack.is(Items.WRITTEN_BOOK) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(MARKER);
    }

    private static ItemStack createBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(MARKER, true);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        Component page = Component.literal("Книга фракции\n\nПКМ по игроку с книгой отправляет приглашение.\n\n")
                .append(SocialInteractions.button("Открыть состав", SocialInteractions.ACTION + "roster"));
        // Это обычный экран книги. Кнопка выводит актуальный состав, а не сохранённую копию.
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Книга фракции"),
                "BlafCraft", 0, List.of(Filterable.passThrough(page)), true));
        return book;
    }
}
