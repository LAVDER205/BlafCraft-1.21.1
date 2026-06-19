package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


@EventBusSubscriber(modid = BlafCraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerHandler {

    private static class PendingAction {
        final UUID playerId; // игрок
        final ActionType action; // действие
        int ticksLeft; // кд

        PendingAction(UUID playerId, ActionType action, int ticksLeft) {
            this.playerId = playerId;
            this.action = action;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final Queue<PendingAction> pendingActions = new ConcurrentLinkedQueue<>();

    // Храним для каждого игрока (UUID) и для каждого типа действия время последнего использования
    private static final Map<UUID, Map<ActionType, Long>> cooldowns = new HashMap<>();

    public static void handle(ActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UUID playerId = player.getUUID();
                ActionType action = packet.action();
                long currentTime = player.level().getGameTime(); // серверное время в тиках

                // Получаем или создаём запись для игрока
                Map<ActionType, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
                long lastUsed = playerCooldowns.getOrDefault(action, 0L);
                int cooldown = action.getCooldownTicks();

                // Если кулдаун не прошёл — просто выходим
                if (currentTime - lastUsed < cooldown) {
                    return; // можно также отправить сообщение игроку о необходимости подождать
                }

                // Сохраняем время использования
                playerCooldowns.put(action, currentTime);

                // Выполняем действие
                switch (action) {
                    case SPEED -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                    case JUMP -> player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1));

                    case ARROW -> {
                        // Первый выстрел немедленно
                        shootArrow(player);

                        // Планируем второй выстрел через n тиков
                        pendingActions.add(new PendingAction(player.getUUID(), ActionType.ARROW, 5));
                        pendingActions.add(new PendingAction(player.getUUID(), ActionType.ARROW, 10));
                    }

                    case SNOWBALL -> {
                       shootSnowball(player);
                    }
                }
            }
        });
    }

    private static void shootArrow(ServerPlayer player) {
        Arrow arrow = EntityType.ARROW.create(player.level());
        if (arrow == null) return;
        arrow.setOwner(player);
        arrow.setPos(player.getEyePosition());
        Vec3 look = player.getViewVector(1.0F);
        arrow.shoot(look.x, look.y, look.z, 3.0f, 0.0f);
        player.level().addFreshEntity(arrow);
    }

    private static void shootSnowball(ServerPlayer player) {
        Snowball snowball = EntityType.SNOWBALL.create(player.level());
        if (snowball != null) {
            snowball.setOwner(player);
            snowball.setPos(player.getEyePosition());
            Vec3 look = player.getViewVector(1.0F);
            snowball.shoot(look.x, look.y, look.z, 1.5f, 0.0f);
            player.level().addFreshEntity(snowball);
        }
    }


    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Получаем сервер (можно через ServerLifecycleHooks.getCurrentServer())
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Обрабатываем все запланированные действия
        Iterator<ServerHandler.PendingAction> iterator = ServerHandler.pendingActions.iterator();
        while (iterator.hasNext()) {
            ServerHandler.PendingAction pa = iterator.next();
            pa.ticksLeft--;

            if (pa.ticksLeft <= 0) {
                // Время вышло, выполняем действие
                ServerPlayer player = server.getPlayerList().getPlayer(pa.playerId);
                if (player != null) {
                    switch (pa.action) {
                        case ARROW -> ServerHandler.shootArrow(player);
                        // Можно добавить другие действия с задержкой
                        default -> {} // на случай других действий
                    }
                }
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) { // сброс кд при выходе
        cooldowns.remove(event.getEntity().getUUID());
    }
}


