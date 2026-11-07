package net.blafteam.blafcraft.keybinds;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumMap;
import java.util.Map;

public class ClientCooldowns {
    // Отсчёт для HUD. Решение о повторном применении всегда принимает сервер.
    private static final Map<ActionType, Integer> remaining = new EnumMap<>(ActionType.class);

    public static void handle(CooldownSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.ticks() > 0) {
                remaining.put(payload.action(), payload.ticks());
            }
        });
    }

    public static int getRemaining(ActionType action) {
        return remaining.getOrDefault(action, 0);
    }

    public static void tick() {
        remaining.replaceAll((action, ticks) -> ticks - 1);
        remaining.values().removeIf(ticks -> ticks <= 0);
    }

    public static void clear() {
        remaining.clear();
    }
}
