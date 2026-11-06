package net.blafteam.blafcraft.network;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.keybinds.ActionPacket;
import net.blafteam.blafcraft.keybinds.ServerHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = BlafCraft.MODID)
public class AbilityNetworking {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        // Этот пакет нужен обеим сторонам, в том числе выделенному серверу без клиента Minecraft.
        event.registrar("2").playToServer(ActionPacket.TYPE, ActionPacket.STREAM_CODEC, ServerHandler::handle);
    }
}
