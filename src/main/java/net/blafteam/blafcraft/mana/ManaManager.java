package net.blafteam.blafcraft.mana;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = BlafCraft.MODID)
public class ManaManager {
    private static ManaData getOrCreate(Player player) {
        return player.getData(ModAttachments.MANA.get());
    }

    public static float getMana(Player player) { return getOrCreate(player).currentMana(); }
    public static int getMaxMana(Player player) { return getOrCreate(player).maxMana(); }
    public static float getManaRegen(Player player) { return getOrCreate(player).manaRegen(); }

    public static void setMana(Player player, float value) {
        ManaData data = getOrCreate(player);
        player.setData(ModAttachments.MANA.get(), new ManaData(
                Math.min(value, data.maxMana()), data.maxMana(), data.manaRegen()));
    }

    public static boolean consumeMana(Player player, float amount) {
        if (getMana(player) < amount) return false;
        setMana(player, getMana(player) - amount);
        return true;
    }

    public static void setMaxMana(Player player, int newMaxMana) {
        ManaData data = getOrCreate(player);
        player.setData(ModAttachments.MANA.get(), new ManaData(
                Math.min(data.currentMana(), newMaxMana), newMaxMana, data.manaRegen()));
    }

    public static void setManaRegen(Player player, float newManaRegen) {
        ManaData data = getOrCreate(player);
        player.setData(ModAttachments.MANA.get(), new ManaData(
                data.currentMana(), data.maxMana(), newManaRegen));
    }

    public static void tick(ServerPlayer player) {
        ManaData data = getOrCreate(player);
        if (data.currentMana() < data.maxMana()) setMana(player, data.currentMana() + data.manaRegen());
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(getMana(player), getMaxMana(player)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player);
            sync(player);
        }
    }
}
