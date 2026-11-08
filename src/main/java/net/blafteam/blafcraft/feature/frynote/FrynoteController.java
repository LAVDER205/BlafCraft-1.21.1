package net.blafteam.blafcraft.feature.frynote;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.character.CharacterAssignments;
import net.blafteam.blafcraft.character.CharacterKit;
import net.blafteam.blafcraft.component.ModAttachments;
import net.blafteam.blafcraft.effect.ModEffects;
import net.blafteam.blafcraft.keybinds.ActionType;
import net.blafteam.blafcraft.mana.ManaManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class FrynoteController {
    private FrynoteController() {}

    public static boolean isFrynote(ServerPlayer player) {
        return CharacterAssignments.get(player) == CharacterKit.FRYNOTE;
    }

    public static boolean handles(ActionType action) {
        return action == ActionType.TELEPORT_DASH || action == ActionType.TELEPORT_GLANCE
                || action == ActionType.TELEPORT_ANCHOR;
    }

    public static FrynoteData data(ServerPlayer player) {
        return player.getData(ModAttachments.FRYNOTE.get());
    }

    public static void cast(ServerPlayer player, ActionType action) {
        if (!player.isAlive() || player.isSpectator() || player.isSleeping() || player.isPassenger()
                || player.hasEffect(ModEffects.STUN_EFFECT)) return;
        FrynoteData state = data(player);
        boolean success = switch (action) {
            case TELEPORT_DASH -> dash(player, state);
            case TELEPORT_GLANCE -> glance(player, state);
            case TELEPORT_ANCHOR -> anchor(player, state);
            default -> false;
        };
        if (!success) player.displayClientMessage(Component.literal("Перенос недоступен: проверь ману, заряды и место назначения."), true);
        ManaManager.sync(player);
        syncHud(player);
    }

    private static boolean dash(ServerPlayer player, FrynoteData state) {
        if (state.charges() == 0 || ManaManager.getMana(player) < FrynoteBalance.DASH_MANA) return false;
        Vec3 destination = SafeTeleport.dashDestination(player);
        Vec3 origin = player.position();
        if (!SafeTeleport.move(player, destination)) return false;
        ManaManager.consumeMana(player, FrynoteBalance.DASH_MANA);
        state.spendCharge();
        finishTeleport(player, state, origin);
        return true;
    }

    private static boolean glance(ServerPlayer player, FrynoteData state) {
        if (state.glanceCooldown() > 0 || ManaManager.getMana(player) < FrynoteBalance.GLANCE_MANA) return false;
        Vec3 destination = SafeTeleport.glanceDestination(player);
        Vec3 origin = player.position();
        if (!SafeTeleport.move(player, destination)) return false;
        ManaManager.consumeMana(player, FrynoteBalance.GLANCE_MANA);
        state.startGlanceCooldown();
        finishTeleport(player, state, origin);
        return true;
    }

    private static boolean anchor(ServerPlayer player, FrynoteData state) {
        if (state.anchor() == null) {
            if (state.anchorCooldown() > 0 || ManaManager.getMana(player) < FrynoteBalance.ANCHOR_SET_MANA
                    || !SafeTeleport.isSafe(player, player.position(), false)) return false;
            state.setAnchor(player.position(), player.serverLevel().dimension());
            ManaManager.consumeMana(player, FrynoteBalance.ANCHOR_SET_MANA);
            particles(player, player.position());
            return true;
        }
        Vec3 destination = state.anchor();
        if (!player.serverLevel().dimension().equals(state.anchorDimension())
                || player.position().distanceToSqr(destination) > FrynoteBalance.ANCHOR_DISTANCE * FrynoteBalance.ANCHOR_DISTANCE
                || ManaManager.getMana(player) < FrynoteBalance.ANCHOR_RETURN_MANA
                || !SafeTeleport.isSafe(player, destination, false)) return false;
        Vec3 origin = player.position();
        if (!SafeTeleport.move(player, destination)) return false;
        ManaManager.consumeMana(player, FrynoteBalance.ANCHOR_RETURN_MANA);
        state.cancelAnchor();
        finishTeleport(player, state, origin);
        return true;
    }

    private static void finishTeleport(ServerPlayer player, FrynoteData state, Vec3 origin) {
        state.markTeleport();
        particles(player, origin);
        particles(player, player.position());
        player.serverLevel().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.2F);
    }

    private static void particles(ServerPlayer player, Vec3 position) {
        player.serverLevel().sendParticles(ParticleTypes.PORTAL, position.x, position.y + 1, position.z,
                15, 0.2, 0.5, 0.2, 0.05);
    }

    public static void tick(ServerPlayer player) {
        FrynoteData state = data(player);
        state.tick();
        ManaManager.setMana(player, ManaManager.getMana(player) + state.regenPerTick());
        if (player.tickCount % 5 == 0) syncHud(player);
    }

    public static void syncHud(ServerPlayer player) {
        boolean enabled = isFrynote(player);
        FrynoteData state = data(player);
        PacketDistributor.sendToPlayer(player, new FrynoteHudPayload(enabled, state.charges(), state.recharge(),
                state.glanceCooldown(), state.anchorTicks(), state.anchorCooldown()));
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getNewDamage() > 0)) return;
        if (event.getEntity() instanceof ServerPlayer victim && isFrynote(victim)) data(victim).markCombat();
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || !isFrynote(attacker)
                || attacker == event.getEntity()) return;
        FrynoteData state = data(attacker);
        state.markCombat();
        // Бонус за ближний бой: снаряды и безвредные удары его не дают.
        // Пассивные животные не служат источником бонусной маны.
        if (event.getSource().getDirectEntity() != attacker
                || !(event.getEntity() instanceof ServerPlayer || event.getEntity() instanceof Enemy)) return;
        Vec3 offset = attacker.position().subtract(event.getEntity().position()).multiply(1, 0, 1);
        Vec3 direction = offset.normalize();
        if (state.recordMeleeHit(event.getEntity().getUUID(), direction)) {
            ManaManager.setMana(attacker, ManaManager.getMana(attacker) + FrynoteBalance.BONUS_MANA);
            ManaManager.sync(attacker);
        }
    }

    private static void cancelAnchor(ServerPlayer player) {
        if (player.hasData(ModAttachments.FRYNOTE.get())) data(player).cancelAnchor();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancelAnchor(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancelAnchor(player);
            if (isFrynote(player)) syncHud(player);
        }
    }
}
