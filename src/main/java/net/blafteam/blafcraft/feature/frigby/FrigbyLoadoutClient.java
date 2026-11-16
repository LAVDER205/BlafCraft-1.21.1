package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = BlafCraft.MODID, value = Dist.CLIENT)
public final class FrigbyLoadoutClient {
    private static FrigbyLoadout loadout = FrigbyLoadout.DEFAULT;

    private FrigbyLoadoutClient() {}

    public static FrigbyLoadout get() {
        return loadout;
    }

    public static void handle(FrigbyLoadoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            FrigbyLoadout received = FrigbyLoadout.decode(payload.choices());
            if (received != null) loadout = received;
        });
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        loadout = FrigbyLoadout.DEFAULT;
    }
}
