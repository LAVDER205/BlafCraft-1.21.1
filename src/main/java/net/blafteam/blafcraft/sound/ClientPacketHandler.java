package net.blafteam.blafcraft.sound;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPacketHandler { // клиентский обработчик пакетов
    public static void handleLoopingSound(LoopingSoundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.start()) {
                ClientLoopingSoundManager.start(payload.sound(), payload.volume(), payload.pitch());
            } else {
                ClientLoopingSoundManager.stop(payload.sound());
            }
        });
    }
}
