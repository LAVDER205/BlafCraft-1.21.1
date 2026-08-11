package net.blafteam.blafcraft.highlight;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HighlightManager {
    // Активные подсветки: игрок -> множество entityId
    private static final Map<UUID, Set<Integer>> playerHighlights = new HashMap<>();

    // Очередь отложенного выключения: игрок -> (entityId -> оставшиеся тики)
    private static final Map<UUID, Map<Integer, Integer>> pendingRemovals = new HashMap<>();

    // Очередь отложенного включения: игрок -> (entityId -> ScheduledHighlight)
    private static final Map<UUID, Map<Integer, ScheduledHighlight>> pendingHighlights = new HashMap<>();

    // Запись для хранения параметров отложенного включения
    private static class ScheduledHighlight {
        final float r, g, b;
        int ticksLeft;

        ScheduledHighlight(float r, float g, float b, int ticksLeft) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.ticksLeft = ticksLeft;
        }
    }

    // --- Мгновенное включение ---
    public static void highlight(ServerPlayer player, Entity entity, float r, float g, float b) {
        playerHighlights.computeIfAbsent(player.getUUID(), k -> new HashSet<>())
                .add(entity.getId());
        // Удаляем возможную запланированную задачу на выключение для этой сущности
        cancelPendingRemoval(player, entity);
        PacketDistributor.sendToPlayer(player,
                new HighlightEntityPacket(entity.getId(), true, r, g, b));
    }

    // --- Мгновенное выключение ---
    public static void unhighlight(ServerPlayer player, Entity entity) {
        Set<Integer> highlights = playerHighlights.get(player.getUUID());
        if (highlights != null) {
            highlights.remove(entity.getId());
            if (highlights.isEmpty()) {
                playerHighlights.remove(player.getUUID());
            }
        }
        // Удаляем возможные запланированные задачи для этой сущности
        cancelPendingRemoval(player, entity);
        cancelPendingHighlight(player, entity);
        PacketDistributor.sendToPlayer(player,
                new HighlightEntityPacket(entity.getId(), false, 0f, 0f, 0f));
    }

    // --- Проверка активности ---
    public static boolean isHighlighted(ServerPlayer player, Entity entity) {
        Set<Integer> highlights = playerHighlights.get(player.getUUID());
        return highlights != null && highlights.contains(entity.getId());
    }

    // --- Очистка при выходе игрока ---
    public static void onPlayerLoggedOut(ServerPlayer player) {
        playerHighlights.remove(player.getUUID());
        pendingRemovals.remove(player.getUUID());
        pendingHighlights.remove(player.getUUID());
    }

    // --- Планирование выключения ---
    public static void scheduleUnhighlightWithoutUpdate(ServerPlayer player, Entity entity, int delayTicks) {
        Map<Integer, Integer> map = pendingRemovals.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        if (map.containsKey(entity.getId())) {
            return; // уже запланировано – игнорируем
        }
        // Отменяем возможное запланированное включение для этой же сущности
        cancelPendingHighlight(player, entity);
        map.put(entity.getId(), delayTicks);
    }
    // --- Планирование выключения ---
    public static void scheduleUnhighlightWithUpdate(ServerPlayer player, Entity entity, int delayTicks) {
        Map<Integer, Integer> map = pendingRemovals.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        map.put(entity.getId(), delayTicks);
    }

    // --- Планирование включения ---
    public static void scheduleHighlightWithoutUpdate(ServerPlayer player, Entity entity, float r, float g, float b, int delayTicks) {
        Map<Integer, ScheduledHighlight> map = pendingHighlights.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        if (map.containsKey(entity.getId())) {
            return; // уже запланировано – игнорируем
        }
        cancelPendingRemoval(player, entity);
        map.put(entity.getId(), new ScheduledHighlight(r, g, b, delayTicks));
    }
    // --- Планирование включения ---
    public static void scheduleHighlightWithUpdate(ServerPlayer player, Entity entity, float r, float g, float b, int delayTicks) {
        Map<Integer, ScheduledHighlight> map = pendingHighlights.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        map.put(entity.getId(), new ScheduledHighlight(r, g, b, delayTicks));
    }

    // Вспомогательные методы для отмены задач
    private static void cancelPendingRemoval(ServerPlayer player, Entity entity) {
        Map<Integer, Integer> map = pendingRemovals.get(player.getUUID());
        if (map != null) {
            map.remove(entity.getId());
            if (map.isEmpty()) {
                pendingRemovals.remove(player.getUUID());
            }
        }
    }

    private static void cancelPendingHighlight(ServerPlayer player, Entity entity) {
        Map<Integer, ScheduledHighlight> map = pendingHighlights.get(player.getUUID());
        if (map != null) {
            map.remove(entity.getId());
            if (map.isEmpty()) {
                pendingHighlights.remove(player.getUUID());
            }
        }
    }

    // --- Обработка серверного тика (Post) ---
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // --- Обработка отложенных выключений ---
        List<Map.Entry<UUID, Integer>> removalsToProcess = new ArrayList<>();
        Iterator<Map.Entry<UUID, Map<Integer, Integer>>> removalIterator = pendingRemovals.entrySet().iterator();
        while (removalIterator.hasNext()) {
            Map.Entry<UUID, Map<Integer, Integer>> playerEntry = removalIterator.next();
            UUID playerId = playerEntry.getKey();
            Map<Integer, Integer> entityMap = playerEntry.getValue();

            Iterator<Map.Entry<Integer, Integer>> it = entityMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> e = it.next();
                int ticksLeft = e.getValue() - 1;
                if (ticksLeft <= 0) {
                    removalsToProcess.add(new AbstractMap.SimpleEntry<>(playerId, e.getKey()));
                    it.remove();
                } else {
                    e.setValue(ticksLeft);
                }
            }
            if (entityMap.isEmpty()) removalIterator.remove();
        }

        // Выполняем отложенные выключения вне цикла
        for (var entry : removalsToProcess) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                Entity entity = player.level().getEntity(entry.getValue());
                if (entity != null) {
                    unhighlight(player, entity); // здесь cancelPendingRemoval сработает уже без итератора
                } else {
                    // Сущность исчезла – просто убираем из активных подсветок
                    Set<Integer> highlights = playerHighlights.get(entry.getKey());
                    if (highlights != null) {
                        highlights.remove(entry.getValue());
                        if (highlights.isEmpty()) playerHighlights.remove(entry.getKey());
                    }
                }
            }
        }

        // --- Обработка отложенных включений (аналогично) ---
        List<Map.Entry<UUID, Map.Entry<Integer, ScheduledHighlight>>> highlightsToProcess = new ArrayList<>();
        Iterator<Map.Entry<UUID, Map<Integer, ScheduledHighlight>>> highlightIterator = pendingHighlights.entrySet().iterator();
        while (highlightIterator.hasNext()) {
            Map.Entry<UUID, Map<Integer, ScheduledHighlight>> playerEntry = highlightIterator.next();
            UUID playerId = playerEntry.getKey();
            Map<Integer, ScheduledHighlight> entityMap = playerEntry.getValue();

            Iterator<Map.Entry<Integer, ScheduledHighlight>> it = entityMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, ScheduledHighlight> e = it.next();
                ScheduledHighlight scheduled = e.getValue();
                scheduled.ticksLeft--;
                if (scheduled.ticksLeft <= 0) {
                    highlightsToProcess.add(new AbstractMap.SimpleEntry<>(playerId,
                            new AbstractMap.SimpleEntry<>(e.getKey(), scheduled)));
                    it.remove();
                }
            }
            if (entityMap.isEmpty()) highlightIterator.remove();
        }

        for (var entry : highlightsToProcess) {
            UUID playerId = entry.getKey();
            int entityId = entry.getValue().getKey();
            ScheduledHighlight scheduled = entry.getValue().getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                Entity entity = player.level().getEntity(entityId);
                if (entity != null) {
                    highlight(player, entity, scheduled.r, scheduled.g, scheduled.b);
                }
            }
        }
    }
}
