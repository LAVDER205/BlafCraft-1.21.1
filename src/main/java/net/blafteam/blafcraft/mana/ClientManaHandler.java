package net.blafteam.blafcraft.mana;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientManaHandler {
    private static float clientMana;
    private static int clientMaxMana;

    public static void clear() {
        clientMana = 0;
        clientMaxMana = 0;
    }

    public static void handle(ManaSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientMana = payload.currentMana();
            clientMaxMana = payload.maxMana();
        });
    }

    public static float getMana() {
        return clientMana;
    }


    public static int getMaxMana() {
        return clientMaxMana;
    }
}
