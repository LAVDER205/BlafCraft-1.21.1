package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class FrigbyActivity {
    private FrigbyActivity() {}

    public static void syncLoadout(ServerPlayer player) {
        if (FrigbyController.isFrigby(player)) {
            PacketDistributor.sendToPlayer(player, new FrigbyLoadoutPayload(FrigbyLoadout.get(player).encode()));
        }
    }

    public static int remaining(ServerPlayer player) {
        long now = player.serverLevel().getServer().overworld().getGameTime();
        return (int) Math.clamp(player.getData(ModAttachments.FRIGBY_EDIT_AT.get()) - now,
                0, FrigbyBalance.LOADOUT_LOCK_TICKS);
    }

    public static void mark(ServerPlayer player) {
        if (!FrigbyController.isFrigby(player)) return;
        long now = player.serverLevel().getServer().overworld().getGameTime();
        player.setData(ModAttachments.FRIGBY_EDIT_AT.get(), now + FrigbyBalance.LOADOUT_LOCK_TICKS);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        if (event.getEntity() instanceof ServerPlayer target) mark(target);
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) mark(attacker);
    }
}
