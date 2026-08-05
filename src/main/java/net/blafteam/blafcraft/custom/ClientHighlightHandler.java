package net.blafteam.blafcraft.custom;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientHighlightHandler {
    static final Set<Integer> highlightedEntityIds = new HashSet<>();

    public static void handle(HighlightEntityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.highlight()) {
                highlightedEntityIds.add(packet.entityId());
            } else {
                highlightedEntityIds.remove(packet.entityId());
            }
        });
    }

    public static boolean isHighlighted(Entity entity) {
        return highlightedEntityIds.contains(entity.getId());
    }

    public static void clear() {
        highlightedEntityIds.clear();
    }
}
