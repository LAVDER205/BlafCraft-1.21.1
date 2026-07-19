package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.mana.ManaManager;
import net.blafteam.blafcraft.mana.ManaSyncPayload;
import net.blafteam.blafcraft.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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
                ServerLevel serverLevel = (ServerLevel) player.level();

                // Получаем или создаём запись для игрока
                Map<ActionType, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
                long lastUsed = playerCooldowns.getOrDefault(action, 0L);
                int cooldown = action.getCooldownTicks();
                int exp_price = action.getExp_price();
                int mana_price = action.getMana_price();

                // Если кулдаун не прошёл — просто выходим
                if (currentTime - lastUsed < cooldown || player.experienceLevel < exp_price || !ManaManager.consumeMana(player, mana_price)) {
                    return; // можно также отправить сообщение игроку о необходимости подождать
                }

                // Выполняем действие
                switch (action) {
                    case TEST -> {


                    }
                    case ARROW -> {
                        shootArrow(player);
                        debitAndCooldown(player, playerCooldowns, currentTime, action, exp_price, mana_price);

                        pendingActions.add(new PendingAction(player.getUUID(), ActionType.ARROW, 5));
                        pendingActions.add(new PendingAction(player.getUUID(), ActionType.ARROW, 10));
                    }

                    case FIREBALL -> {
                        shootLargeFireball(player);

                        // FIREBALL JUMP
                        BlockPos posBelow = player.blockPosition().below();
                        BlockPos posBelowBelow = player.blockPosition().below().below();
                        BlockState stateBelow = player.level().getBlockState(posBelow);
                        BlockState stateBelowBelow = player.level().getBlockState(posBelowBelow);
                        boolean hasSolidGroundBelow = !(stateBelow.getCollisionShape(player.level(), posBelow).isEmpty());
                        boolean hasSolidGroundBelowBelow = !(stateBelowBelow.getCollisionShape(player.level(), posBelowBelow).isEmpty());

                        if (player.getXRot() >= 60 && (hasSolidGroundBelow || hasSolidGroundBelowBelow)) {
                            player.addDeltaMovement(new Vec3(0, 1, 0));
                            player.connection.send(new ClientboundSetEntityMotionPacket(player)); // sync server and client
                        }
                        debitAndCooldown(player, playerCooldowns, currentTime, action, exp_price, mana_price);
                    }

                    case CREATION_STEP -> {
                        if (player.hasEffect(ModEffects.CREATION_STEP_EFFECT)) {
                            player.removeEffect(ModEffects.CREATION_STEP_EFFECT);
                        } else if (player.totalExperience >= 1){
                            player.addEffect(new MobEffectInstance(ModEffects.CREATION_STEP_EFFECT, -1, 0, false, false, false));
                            debitAndCooldown(player, playerCooldowns, currentTime, action, exp_price, mana_price);
                        }
                    }

                    case TELEPORT_DASH -> {
                        serverLevel.sendParticles(player, ModParticles.TELEPORT_PARTICLES.get(), false, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 10, 0.2, 0.5, 0.2, 5);

                        Vec3 look = player.getViewVector(1.0F);
                        double strength = 6.0;

                        player.teleportRelative(look.x * strength, 0, look.z * strength);

                        serverLevel.sendParticles(player, ModParticles.TELEPORT_PARTICLES.get(), false, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 10, 0.2, 0.5, 0.2, 5);

                        debitAndCooldown(player, playerCooldowns, currentTime, action, exp_price, mana_price);
                    }

                    case TELEPORT_GLANCE -> {
                        HitResult hitResult = player.pick(50.0, 1.0F, false);

                        if (hitResult.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                            BlockPos targetBlock = blockHitResult.getBlockPos();

                            BlockPos posAbove = targetBlock.above();
                            BlockState stateAbove = player.level().getBlockState(posAbove);
                            boolean hasSolidGroundAbove = stateAbove.getCollisionShape(player.level(), posAbove).isEmpty();

                            if (hasSolidGroundAbove) {
                                serverLevel.sendParticles(player, ModParticles.TELEPORT_PARTICLES.get(), false, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 10, 0.2, 0.5, 0.2, 5);
                                player.teleportTo(targetBlock.getX() + 0.5, targetBlock.getY() + 1, targetBlock.getZ() + 0.5);
                                serverLevel.sendParticles(player, ModParticles.TELEPORT_PARTICLES.get(), false, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 10, 0.2, 0.5, 0.2, 5);

                                debitAndCooldown(player, playerCooldowns, currentTime, action, exp_price, mana_price);
                            }
                        }
                    }

                    case BLOODLUST -> {
                        player.addEffect(new MobEffectInstance(ModEffects.BLOODLUST_EFFECT, 1200, 0));
                        debitAndCooldown(player, playerCooldowns, currentTime, action, exp_price, mana_price);
                    }
                }

//                int currentMaxMana = ManaManager.getMaxMana(player); // change max mana
//                ManaManager.setMaxMana(player, currentMaxMana + 50); // change max mana
//
//                float currentManaRegen = ManaManager.getMaxMana(player); // change mana regen
//                ManaManager.setManaRegen(player, currentManaRegen + 1); // change mana regen
            }
        });
    }

    private static void debitAndCooldown(ServerPlayer player, Map<ActionType, Long> playerCooldowns, long currentTime, ActionType action, int exp_price, int mana_price) {
        // Сохраняем время использования
        playerCooldowns.put(action, currentTime);

        player.setExperienceLevels(player.experienceLevel - exp_price); // цена exp

        PacketDistributor.sendToPlayer(player, new ManaSyncPayload( // синхронизация нового значения маны
                ManaManager.getMana(player), 100));

        ClientCooldowns.startCooldown(action);// кд

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

    private static void shootLargeFireball(ServerPlayer player) {
        LargeFireball largeFireball = EntityType.FIREBALL.create(player.level());
        if (largeFireball != null) {
            largeFireball.setOwner(player);
            largeFireball.setPos(player.getEyePosition());
            Vec3 look = player.getViewVector(1.0F);
            largeFireball.shoot(look.x, look.y, look.z, 1.5f, 0.0f);
            player.level().addFreshEntity(largeFireball);
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


