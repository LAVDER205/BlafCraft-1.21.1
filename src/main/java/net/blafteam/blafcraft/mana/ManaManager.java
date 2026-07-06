package net.blafteam.blafcraft.mana;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ManaManager {
    // Хранилище: UUID игрока -> его данные маны
    private static final Map<UUID, ManaData> playerMana = new HashMap<>();

    // Базовые значения
    private static final int DEFAULT_MAX_MANA = ManaConfig.MAX_MANA;
    private static final float MANA_REGEN_PER_TICK = ManaConfig.MANA_REGEN;

    // Получить данные маны игрока (создать, если нет)
    private static ManaData getOrCreate(Player player) {
        return playerMana.computeIfAbsent(player.getUUID(),
                uuid -> new ManaData(DEFAULT_MAX_MANA, DEFAULT_MAX_MANA, MANA_REGEN_PER_TICK));
    }

    // Получить текущую ману
    public static float getMana(Player player) {
        return getOrCreate(player).currentMana;
    }

    // Установить ману напрямую (для синхронизации)
    public static void setMana(Player player, float value) {
        ManaData data = getOrCreate(player);
        data.currentMana = Math.min(value, data.maxMana);
    }

    // Проверить, достаточно ли маны, и если да — списать
    public static boolean consumeMana(Player player, float amount) { // check and decrease
        ManaData data = getOrCreate(player);
        if (data.currentMana >= amount) {
            data.currentMana -= amount;
            return true;
        }
        return false;
    }

    public static void setMaxMana(Player player, int newMaxMana) {
        ManaData data = getOrCreate(player);
        data.maxMana = newMaxMana;
        // Текущая мана не должна превышать новый максимум
        if (data.currentMana > newMaxMana) {
            data.currentMana = newMaxMana;
        }
    }

    public static int getMaxMana(Player player) {
        return getOrCreate(player).maxMana;
    }

    public static void setManaRegen(Player player, float newManaRegen) {
        ManaData data = getOrCreate(player);
        data.manaRegen = newManaRegen;
    }

    public static float getManaRegen(Player player) {
        return getOrCreate(player).manaRegen;
    }

    // Восстановление маны (вызывается каждый серверный тик)
    public static void tick(ServerPlayer player) {
        ManaData data = getOrCreate(player);
        if (data.currentMana < data.maxMana) {
            data.currentMana = Math.min(data.currentMana + data.manaRegen, data.maxMana);
        }
    }

    // Внутренний класс для хранения данных
    private static class ManaData {
        float currentMana;
        int maxMana;
        float manaRegen;

        ManaData(float currentMana, int maxMana, float manaRegen) {
            this.currentMana = currentMana;
            this.maxMana = maxMana;
            this.manaRegen = manaRegen;
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Получаем всех игроков на сервере
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            ManaManager.tick(player);
            // Отправляем обновлённую ману (можно делать не каждый тик, а раз в N тиков для оптимизации)
            PacketDistributor.sendToPlayer(player, new ManaSyncPayload(
                    ManaManager.getMana(player), DEFAULT_MAX_MANA));
        }
    }
}
