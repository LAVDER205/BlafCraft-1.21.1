package net.blafteam.blafcraft.morph;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientMorphData {
    private static ResourceLocation morphId;

    public static void handleMorphSync(MorphSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            morphId = payload.morphId();
        });
    }

    public static ResourceLocation getMorphId() {
        return morphId;
    }

    public static boolean hasMorph() {
        return morphId != null;
    }
}
