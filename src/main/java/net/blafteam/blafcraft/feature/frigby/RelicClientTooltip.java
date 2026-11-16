package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.item.ModItems;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = BlafCraft.MODID, value = Dist.CLIENT)
public final class RelicClientTooltip {
    private static int points = -1;

    private RelicClientTooltip() {}

    public static void handle(RelicChargePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> points = payload.points());
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ModItems.RELIC.get())) return;
        if (points >= 0) {
            event.getToolTip().add(Component.literal("Запас опыта: " + points + "/" + FrigbyBalance.RELIC_CAPACITY));
            event.getToolTip().add(Component.literal("Удерживай ПКМ, чтобы забрать опыт"));
        } else {
            event.getToolTip().add(Component.literal("Заряд реликвии может забрать только Фригби"));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        points = -1;
    }
}
