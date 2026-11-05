package net.blafteam.blafcraft.event;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModAttachments;
import net.blafteam.blafcraft.morph.MorphSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BlafCraft.MODID)
public class PlayerDataEvents {
    private static void syncMorph(ServerPlayer player) {
        if (player.hasData(ModAttachments.ENTITY.get())) {
            var morph = player.getData(ModAttachments.ENTITY.get());
            if (morph != null) PacketDistributor.sendToPlayer(player, new MorphSyncPayload(morph));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMorph(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMorph(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncMorph(player);
    }
}
