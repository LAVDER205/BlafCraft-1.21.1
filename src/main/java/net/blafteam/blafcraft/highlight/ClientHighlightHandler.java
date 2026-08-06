package net.blafteam.blafcraft.highlight;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public class ClientHighlightHandler {
    // Храним для каждой подсвеченной сущности её RGB (0..1)
    static final Map<Integer, float[]> highlightedEntities = new HashMap<>();

    public static void handle(HighlightEntityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            int id = packet.entityId();
            if (packet.highlight()) {
                highlightedEntities.put(id, new float[]{packet.r(), packet.g(), packet.b()});
            } else {
                highlightedEntities.remove(id);
            }
        });
    }

    public static float[] getHighlightColor(Entity entity) {
        return highlightedEntities.get(entity.getId());
    }

    public static boolean isHighlighted(Entity entity) {
        return highlightedEntities.containsKey(entity.getId());
    }

    public static void clear() {
        highlightedEntities.clear();
    }
}
