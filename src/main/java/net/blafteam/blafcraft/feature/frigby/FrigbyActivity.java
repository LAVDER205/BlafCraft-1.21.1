package net.blafteam.blafcraft.feature.frigby;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModAttachments;
import net.blafteam.blafcraft.keybinds.ServerHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class FrigbyActivity {
    public record QueuedLoadout(FrigbyLoadout choices, long readyAt) {
        public static final Codec<QueuedLoadout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FrigbyLoadout.CODEC.fieldOf("choices").forGetter(QueuedLoadout::choices),
                Codec.LONG.fieldOf("ready_at").forGetter(QueuedLoadout::readyAt)
        ).apply(instance, QueuedLoadout::new));
    }

    private FrigbyActivity() {}

    public static void syncLoadout(ServerPlayer player) {
        if (!FrigbyController.isFrigby(player)) return;
        QueuedLoadout queued = queued(player);
        PacketDistributor.sendToPlayer(player, new FrigbyLoadoutPayload(FrigbyLoadout.get(player).encode(),
                queued == null ? "" : queued.choices().encode()));
    }

    public static void requestLoadout(ServerPlayer player, FrigbyLoadout requested) {
        var pending = ModAttachments.FRIGBY_QUEUED_LOADOUT.get();
        if (requested == null || requested.equals(FrigbyLoadout.get(player))) {
            if (player.hasData(pending)) {
                player.removeData(pending);
                tell(player, "Смена набора отменена");
            }
            syncLoadout(player);
            return;
        }

        int wait = remainingWait(player, requested);
        if (wait == 0) {
            applyLoadout(player, requested);
        } else {
            player.setData(pending, new QueuedLoadout(requested, worldTime(player) + wait));
            syncLoadout(player);
            tell(player, "Смена набора запланирована");
        }
    }

    private static void applyLoadout(ServerPlayer player, FrigbyLoadout requested) {
        player.removeData(ModAttachments.FRIGBY_QUEUED_LOADOUT.get());
        requested.apply(player);
        syncLoadout(player);
        tell(player, "Новый набор созидания активен");
    }

    private static QueuedLoadout queued(ServerPlayer player) {
        var type = ModAttachments.FRIGBY_QUEUED_LOADOUT.get();
        return player.hasData(type) ? player.getData(type) : null;
    }

    private static int remainingWait(ServerPlayer player, FrigbyLoadout requested) {
        int wait = (int) Math.clamp(player.getData(ModAttachments.FRIGBY_EDIT_AT.get()) - worldTime(player),
                0, FrigbyBalance.LOADOUT_LOCK_TICKS);
        // Смотрим только на конструкции, которые меняют слот, добавляются или убираются.
        for (var action : FrigbyLoadout.get(player).affectedBy(requested)) {
            wait = Math.max(wait, ServerHandler.remainingCooldown(player, action));
        }
        return wait;
    }

    private static long worldTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    private static void tell(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    public static void mark(ServerPlayer player) {
        if (!FrigbyController.isFrigby(player)) return;
        player.setData(ModAttachments.FRIGBY_EDIT_AT.get(), worldTime(player) + FrigbyBalance.LOADOUT_LOCK_TICKS);
        QueuedLoadout queued = queued(player);
        if (queued == null) return;
        // Новый бой или применение может отодвинуть уже запланированную смену.
        long newReadyAt = worldTime(player) + remainingWait(player, queued.choices());
        if (newReadyAt > queued.readyAt()) {
            player.setData(ModAttachments.FRIGBY_QUEUED_LOADOUT.get(), new QueuedLoadout(queued.choices(), newReadyAt));
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        QueuedLoadout queued = queued(player);
        if (queued == null || !FrigbyController.isFrigby(player) || worldTime(player) < queued.readyAt()) return;
        if (remainingWait(player, queued.choices()) == 0) applyLoadout(player, queued.choices());
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;
        if (event.getEntity() instanceof ServerPlayer target) mark(target);
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) mark(attacker);
    }
}
