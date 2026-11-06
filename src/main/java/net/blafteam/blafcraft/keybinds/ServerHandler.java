package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.character.CharacterAssignments;
import net.blafteam.blafcraft.friend_system.FriendManager;
import net.blafteam.blafcraft.friend_system.FriendUpdatePacket;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.mana.ManaManager;
import net.blafteam.blafcraft.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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

@EventBusSubscriber(modid = BlafCraft.MODID)
public class ServerHandler {
    public static final Set<UUID> playersWantToFly = new HashSet<>();

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

    public static void handleFriendUpdate(FriendUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                Entity target = level.getEntity(packet.targetUUID());
                if (target instanceof ServerPlayer friend) {
                    if (packet.add()) {
                        FriendManager.addFriend(player, friend);
                    } else {
                        FriendManager.removeFriend(player, friend);
                    }
                }
            }
        });
    }

    public static void handle(ActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive()) return;
            ActionType action = CharacterAssignments.get(player).actionForSlot(packet.slot());
            if (action == null) return;

            long now = player.level().getGameTime();
            Map<ActionType, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
            Long lastUsed = playerCooldowns.get(action);
            if (lastUsed != null && now - lastUsed < action.getCooldownTicks()) return;

            int experienceCost = action.getExp_price();
            int manaCost = action.getMana_price();
            // Проверка ресурсов ничего не списывает. Весь запуск идёт в серверном потоке.
            if (player.experienceLevel < experienceCost || ManaManager.getMana(player) < manaCost) return;
            if (!executeAbility(player, action)) return;

            // Успешное действие оплачивается один раз. Отложенные стрелы входят в этот же залп.
            if (experienceCost > 0) player.setExperienceLevels(player.experienceLevel - experienceCost);
            if (manaCost > 0) ManaManager.consumeMana(player, manaCost);
            playerCooldowns.put(action, now);
            ManaManager.sync(player);
            ClientCooldowns.startCooldown(action);
        });
    }

    /**
     * Добавляя способность, возвращай true только после выполнения.
     * false означает отказ без списания ресурсов и запуска кулдауна.
     */
    private static boolean executeAbility(ServerPlayer player, ActionType action) {
        return switch (action) {
            case TEST -> false;
            case ARROW -> {
                if (!shootArrow(player)) yield false;
                pendingActions.add(new PendingAction(player.getUUID(), action, 5));
                pendingActions.add(new PendingAction(player.getUUID(), action, 10));
                yield true;
            }
            case FIREBALL -> castFireball(player);
            case CREATION_STEP -> {
                if (player.hasEffect(ModEffects.CREATION_STEP_EFFECT)) {
                    yield player.removeEffect(ModEffects.CREATION_STEP_EFFECT);
                }
                yield player.totalExperience > 0 && player.addEffect(
                        new MobEffectInstance(ModEffects.CREATION_STEP_EFFECT, -1, 0, false, false, false));
            }
            case TELEPORT_DASH -> {
                Vec3 look = player.getViewVector(1.0F);
                Vec3 origin = player.position();
                player.teleportRelative(look.x * 6.0, 0, look.z * 6.0);
                yield finishTeleport(player, origin);
            }
            case TELEPORT_GLANCE -> castTeleportGlance(player);
            case BLOODLUST -> player.addEffect(new MobEffectInstance(ModEffects.BLOODLUST_EFFECT, 1200, 0));
            case TIME_BOMB -> player.addEffect(new MobEffectInstance(ModEffects.TIME_BOMB_EFFECT, 200, 1, false, true, true));
            case FLIGHT -> {
                if (player.onGround() || player.isFallFlying()) yield false;
                Vec3 look = player.getViewVector(1.0F);
                player.addDeltaMovement(look.scale(1.5));
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                player.addEffect(new MobEffectInstance(ModEffects.FREE_FLIGHT_EFFECT, -1, 0, false, false, false));
                yield true;
            }
            case SCULK_MARK -> player.addEffect(new MobEffectInstance(ModEffects.SCULK_MARK_EFFECT, 200, 1, false, false, true));
            case MORPH -> player.addEffect(new MobEffectInstance(ModEffects.MORPH_EFFECT, 1200, 0, false, false, true));
            case OVERDOSE_ABILITY -> castOverdose(player);
        };
    }

    private static boolean castFireball(ServerPlayer player) {
        LargeFireball fireball = EntityType.FIREBALL.create(player.level());
        if (fireball == null) return false;
        fireball.setOwner(player);
        fireball.setPos(player.getEyePosition());
        Vec3 look = player.getViewVector(1.0F);
        fireball.shoot(look.x, look.y, look.z, 1.5f, 0.0f);
        if (!player.level().addFreshEntity(fireball)) return false;

        BlockPos below = player.blockPosition().below();
        BlockPos twoBelow = below.below();
        BlockState stateBelow = player.level().getBlockState(below);
        BlockState stateTwoBelow = player.level().getBlockState(twoBelow);
        if (player.getXRot() >= 60 && (!stateBelow.getCollisionShape(player.level(), below).isEmpty()
                || !stateTwoBelow.getCollisionShape(player.level(), twoBelow).isEmpty())) {
            player.addDeltaMovement(new Vec3(0, 1, 0));
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        return true;
    }

    private static boolean castTeleportGlance(ServerPlayer player) {
        if (!(player.pick(50.0, 1.0F, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos destination = hit.getBlockPos().above();
        if (!player.level().getBlockState(destination).getCollisionShape(player.level(), destination).isEmpty()) return false;

        Vec3 origin = player.position();
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        return finishTeleport(player, origin);
    }

    private static boolean finishTeleport(ServerPlayer player, Vec3 origin) {
        if (player.position().equals(origin)) return false;
        sendTeleportParticles(player, origin);
        sendTeleportParticles(player, player.position());
        return true;
    }

    private static void sendTeleportParticles(ServerPlayer player, Vec3 position) {
        player.serverLevel().sendParticles(player, ModParticles.TELEPORT_PARTICLES.get(), false,
                position.x, position.y + player.getBbHeight() / 2.0, position.z, 10, 0.2, 0.5, 0.2, 5);
    }

    private static boolean castOverdose(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.OVERDOSE_EFFECT)) return false;

        var speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        var strength = player.getEffect(MobEffects.DAMAGE_BOOST);
        var regeneration = player.getEffect(MobEffects.REGENERATION);
        var jump = player.getEffect(MobEffects.JUMP);
        var resistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (speed != null && speed.getAmplifier() >= 4) {
            Vec3 look = player.getViewVector(1.0F);
            Vec3 dash = new Vec3(look.x, 0, look.z).normalize().scale(1.6);
            player.setDeltaMovement(dash.x, player.getDeltaMovement().y, dash.z);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            player.addEffect(new MobEffectInstance(ModEffects.RAM_EFFECT, -1, 0, false, false, true));
            return true;
        }
        // Для силы и прыжка активное действие ещё не реализовано. Сохраняем приоритет стимуляторов.
        if (strength != null && strength.getAmplifier() >= 4) return false;
        if (regeneration != null && regeneration.getAmplifier() >= 4) {
            return player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 10, 4, false, false, true));
        }
        if (jump != null && jump.getAmplifier() >= 4) return false;
        if (resistance != null && resistance.getAmplifier() >= 4) {
            return player.addEffect(new MobEffectInstance(ModEffects.IMMORTAL_EFFECT, 5, 0, false, false, true));
        }
        return false;
    }

    private static boolean shootArrow(ServerPlayer player) {
        Arrow arrow = EntityType.ARROW.create(player.level());
        if (arrow == null) return false;
        arrow.setOwner(player);
        arrow.setPos(player.getEyePosition());
        Vec3 look = player.getViewVector(1.0F);
        arrow.shoot(look.x, look.y, look.z, 3.0f, 0.0f);
        return player.level().addFreshEntity(arrow);
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
