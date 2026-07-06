package net.blafteam.blafcraft.event;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.mana.ClientManaHandler;
import net.blafteam.blafcraft.mana.ManaSyncPayload;
import net.blafteam.blafcraft.sound.ClientPacketHandler;
import net.blafteam.blafcraft.sound.LoopingSoundPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModBusEvents { // регистрация пакета на клиенте

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Регистрируем пакет, идущий от сервера к клиенту
        registrar.playToClient(
                LoopingSoundPayload.TYPE,
                LoopingSoundPayload.STREAM_CODEC,
                ClientPacketHandler::handleLoopingSound
        );

        registrar.playToClient(
                ManaSyncPayload.TYPE,
                ManaSyncPayload.STREAM_CODEC,
                ClientManaHandler::handle
        );
    }
}
