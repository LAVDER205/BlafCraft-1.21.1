package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModAttachments;
import net.blafteam.blafcraft.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class RelicCharge {
    // Дробные части очка копятся в данных игрока и переживают выход из мира.
    private static final int UNITS_PER_POINT = 100;
    private static final int CAPACITY = FrigbyBalance.RELIC_CAPACITY * UNITS_PER_POINT;

    private RelicCharge() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || !FrigbyController.isFrigby(player)) return;
        // Пока игрок удерживает реликвию, она только отдаёт заряд.
        if (player.isUsingItem() && player.getUseItem().is(ModItems.RELIC.get())) return;

        int previous = units(player);
        if (previous >= CAPACITY) return;
        int next = Math.min(CAPACITY, previous + chargePerTick(player.experienceLevel));
        player.setData(ModAttachments.RELIC_CHARGE.get(), next);
        if (previous / UNITS_PER_POINT != next / UNITS_PER_POINT) sync(player);
    }

    static int chargePerTick(int experienceLevel) {
        return Math.max(FrigbyBalance.RELIC_MIN_CHARGE_RATE,
                FrigbyBalance.RELIC_BASE_CHARGE_RATE - experienceLevel / FrigbyBalance.RELIC_LEVELS_PER_SLOWDOWN);
    }

    public static int points(ServerPlayer player) {
        return units(player) / UNITS_PER_POINT;
    }

    public static boolean transferPoint(ServerPlayer player) {
        int stored = units(player);
        if (stored < UNITS_PER_POINT) return false;
        player.setData(ModAttachments.RELIC_CHARGE.get(), stored - UNITS_PER_POINT);
        player.giveExperiencePoints(1);
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        int charge = FrigbyController.isFrigby(player) ? points(player) : -1;
        PacketDistributor.sendToPlayer(player, new RelicChargePayload(charge));
    }

    private static int units(ServerPlayer player) {
        return Math.clamp(player.getData(ModAttachments.RELIC_CHARGE.get()), 0, CAPACITY);
    }
}
