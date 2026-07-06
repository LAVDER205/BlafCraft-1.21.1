package net.blafteam.blafcraft.mana;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientManaHandler {
    private static float clientMana = 100;
    private static int clientMaxMana = ManaConfig.MAX_MANA;

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
