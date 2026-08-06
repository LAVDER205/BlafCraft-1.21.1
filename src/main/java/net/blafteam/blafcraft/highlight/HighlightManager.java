package net.blafteam.blafcraft.highlight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class HighlightManager {
    private static final Map<UUID, Set<Integer>> playerHighlights = new HashMap<>();

    public static void highlight(ServerPlayer player, Entity entity, float r, float g, float b) {
        playerHighlights.computeIfAbsent(player.getUUID(), k -> new HashSet<>())
                .add(entity.getId());
        PacketDistributor.sendToPlayer(player,
                new HighlightEntityPacket(entity.getId(), true, r, g, b));
    }

    public static void unhighlight(ServerPlayer player, Entity entity) {
        Set<Integer> highlights = playerHighlights.get(player.getUUID());
        if (highlights != null) {
            highlights.remove(entity.getId());
            if (highlights.isEmpty()) {
                playerHighlights.remove(player.getUUID());
            }
        }
        PacketDistributor.sendToPlayer(player,
                new HighlightEntityPacket(entity.getId(), false, 0f, 0f, 0f));
    }

    public static boolean isHighlighted(ServerPlayer player, Entity entity) {
        Set<Integer> highlights = playerHighlights.get(player.getUUID());
        return highlights != null && highlights.contains(entity.getId());
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        playerHighlights.remove(player.getUUID());
    }
}
