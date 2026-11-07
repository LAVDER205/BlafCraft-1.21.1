package net.blafteam.blafcraft.duel;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class DuelEvents {
    private static final String SESSION = "blafcraft_duel_session";
    private static final String OPPONENT = "blafcraft_duel_opponent";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (recovering(player) || event.getTarget() instanceof ServerPlayer target && recovering(target))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        if (recovering(target) || event.getSource().getEntity() instanceof ServerPlayer attacker && recovering(attacker)) {
            event.setCanceled(true);
            return;
        }
        // Снаряд старого поединка не может попасть в прежнего противника после окончания,
        // даже если оба игрока успели начать новую дуэль.
        if (event.getSource().getDirectEntity() instanceof Projectile projectile) {
            var tag = projectile.getPersistentData();
            if (tag.hasUUID(SESSION) && tag.hasUUID(OPPONENT) && target.getUUID().equals(tag.getUUID(OPPONENT))) {
                var match = DuelService.state(target.serverLevel().getServer()).match(target.getUUID());
                if (match == null || !match.id().equals(tag.getUUID(SESSION))) event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHealthChanged(LivingDamageEvent.Post event) {
        // Здесь уже учтены броня и поглощение. Смерть ещё не обработана Minecraft.
        if (event.getEntity() instanceof ServerPlayer player && player.getHealth() <= 0) protectLoser(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        // Резервный путь для источников смерти, которые обходят обычный расчёт урона.
        if (event.getEntity() instanceof ServerPlayer player) {
            if (DuelService.state(player.serverLevel().getServer()).match(player.getUUID()) != null) {
                event.setCanceled(true);
                protectLoser(player);
            } else if (recovering(player)) {
                event.setCanceled(true);
                player.setHealth(1.0F);
            }
        }
    }

    private static void protectLoser(ServerPlayer player) {
        if (DuelService.state(player.serverLevel().getServer()).match(player.getUUID()) == null) return;
        player.setHealth(1.0F); // Одно очко здоровья — половина сердца.
        player.setAbsorptionAmount(0);
        DuelService.finish(player, player.getGameProfile().getName() + " проиграл дуэль. Поединок завершён.");
        player.setHealth(1.0F);
    }

    private static boolean recovering(ServerPlayer player) {
        var server = player.serverLevel().getServer();
        return DuelService.state(server).recovering(player.getUUID(), server.getTickCount());
    }

    @SubscribeEvent
    public static void onProjectileAdded(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile)
                || !(projectile.getOwner() instanceof ServerPlayer owner)) return;
        var match = DuelService.state(owner.serverLevel().getServer()).match(owner.getUUID());
        if (match == null) return;
        projectile.getPersistentData().putUUID(SESSION, match.id());
        projectile.getPersistentData().putUUID(OPPONENT, match.opponent(owner.getUUID()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DuelService.finish(player, "Дуэль завершена из-за выхода участника.");
            DuelService.state(player.serverLevel().getServer()).clearInvitations(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DuelService.finish(player, "Дуэль завершена из-за смены измерения.");
            DuelService.state(player.serverLevel().getServer()).clearInvitations(player.getUUID());
        }
    }
}
