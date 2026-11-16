package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.character.CharacterAssignments;
import net.blafteam.blafcraft.character.CharacterKit;
import net.blafteam.blafcraft.feature.frigby.FrigbyActivity;
import net.blafteam.blafcraft.feature.frigby.FrigbyLoadout;
import net.blafteam.blafcraft.feature.frigby.FrigbyConstructions;
import net.blafteam.blafcraft.feature.frigby.ExperiencePoints;
import net.blafteam.blafcraft.feature.frigby.FrigbyController;
import net.blafteam.blafcraft.feature.frigby.KingCall;
import net.blafteam.blafcraft.feature.frynote.FrynoteController;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.mana.ManaManager;
import net.blafteam.blafcraft.particle.ModParticles;
import net.blafteam.blafcraft.sound.AbilitySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

@EventBusSubscriber(modid = BlafCraft.MODID)
public class ServerHandler {
    public static final Set<UUID> playersWantToFly = new HashSet<>();

    // Храним для каждого игрока (UUID) и для каждого типа действия время последнего использования
    private static final Map<UUID, Map<ActionType, Long>> cooldowns = new HashMap<>();

    public static void handle(ActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
            CharacterKit kit = CharacterAssignments.get(player);
            int slot = packet.slot();
            ActionType action = kit == CharacterKit.FRIGBY && slot >= 0 && slot < 3
                    ? FrigbyLoadout.get(player).forSlot(slot) : kit.actionForSlot(slot);
            if (action == null || !action.name().equals(packet.expectedAction())) return;
            // Заряды и якорь имеют собственное состояние. Их правила находятся в модуле персонажа.
            if (FrynoteController.isFrynote(player) && FrynoteController.handles(action)) {
                FrynoteController.cast(player, action);
                return;
            }

            // У Зова сохраняется перезарядка между входами в мир.
            if (action == ActionType.KING_CALL) {
                KingCall.cast(player);
                return;
            }

            long now = player.level().getGameTime();
            Map<ActionType, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
            Long lastUsed = playerCooldowns.get(action);
            if (lastUsed != null && now - lastUsed < action.getCooldownTicks()) return;

            int experienceCost = action.getExperiencePointCost();
            int manaCost = action.getMana_price();
            // Проверка ресурсов ничего не списывает. Весь запуск идёт в серверном потоке.
            if (!ExperiencePoints.canAfford(player, experienceCost)) {
                player.displayClientMessage(Component.literal("Нужно опыта: " + experienceCost), true);
                return;
            }
            if (ManaManager.getMana(player) < manaCost) return;
            if (!executeAbility(player, action)) return;
            playAbilitySound(player, action);

            // Успешное действие оплачивается один раз. Отложенные стрелы входят в этот же залп.
            if (experienceCost > 0) player.giveExperiencePoints(-experienceCost);
            if (manaCost > 0) ManaManager.consumeMana(player, manaCost);
            playerCooldowns.put(action, now);
            if (kit == CharacterKit.FRIGBY) FrigbyActivity.mark(player);
            ManaManager.sync(player);
            // Сетевой клиент запускает отсчёт только после подтверждения сервера.
            int cooldownTicks = action.getCooldownTicks();
            if (cooldownTicks > 0) {
                PacketDistributor.sendToPlayer(player, new CooldownSyncPayload(action, cooldownTicks));
            }
        });
    }

    public static int remainingCooldown(ServerPlayer player, ActionType action) {
        Map<ActionType, Long> playerCooldowns = cooldowns.get(player.getUUID());
        if (playerCooldowns == null) return 0;
        Long lastUsed = playerCooldowns.get(action);
        if (lastUsed == null) return 0;
        long elapsed = player.level().getGameTime() - lastUsed;
        return (int) Math.clamp(action.getCooldownTicks() - elapsed, 0, action.getCooldownTicks());
    }

    /**
     * Добавляя способность, возвращай true только после выполнения.
     * false означает отказ без списания ресурсов и запуска кулдауна.
     */
    private static boolean executeAbility(ServerPlayer player, ActionType action) {
        return switch (action) {
            case TELEPORT_ANCHOR, KING_CALL -> false;
            case TEST -> false;
            case ARROW, FIREBALL, CREATION_STEP -> FrigbyController.cast(player, action);
            case STONE_WALL, ARROW_RAIN, CANNON -> FrigbyConstructions.cast(player, action);
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

    // Эти способности выполняются здесь. У Фригби и Фрайнота звуки привязаны к этапам их способностей.
    private static void playAbilitySound(ServerPlayer player, ActionType action) {
        switch (action) {
            case TELEPORT_DASH, TELEPORT_GLANCE -> AbilitySounds.play(player, SoundEvents.ENDERMAN_TELEPORT, 0.55F, 1.2F);
            case BLOODLUST -> AbilitySounds.play(player, SoundEvents.WARDEN_SONIC_CHARGE, 0.45F, 0.85F);
            case TIME_BOMB -> AbilitySounds.play(player, SoundEvents.FIRECHARGE_USE, 0.6F, 0.85F);
            case FLIGHT -> AbilitySounds.play(player, SoundEvents.PHANTOM_FLAP, 0.75F, 1.15F);
            case SCULK_MARK -> AbilitySounds.play(player, SoundEvents.SCULK_BLOCK_CHARGE, 0.7F, 1.0F);
            case MORPH -> AbilitySounds.play(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6F, 0.95F);
            case OVERDOSE_ABILITY -> AbilitySounds.play(player, SoundEvents.PLAYER_BREATH, 0.8F, 1.1F);
            default -> {}
        }
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

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cooldowns.remove(event.getEntity().getUUID());
    }
}
