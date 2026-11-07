package net.blafteam.blafcraft.social;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.duel.DuelService;
import net.blafteam.blafcraft.friend_system.FriendUpdatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Comparator;
import java.util.UUID;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class SocialInteractions {
    static final String ACTION = "/blafcraft social ";

    private SocialInteractions() {}

    public static void handleFriendShortcut(FriendUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
            if (!packet.add()) {
                showFriends(player);
                return;
            }
            if (player.serverLevel().getEntity(packet.targetUUID()) instanceof ServerPlayer target
                    && player.distanceToSqr(target) <= 36 && player.hasLineOfSight(target)) {
                invite(player, target, SocialData.InviteKind.FRIEND);
            }
        });
    }

    public static void invite(ServerPlayer sender, ServerPlayer target, SocialData.InviteKind kind) {
        if (sender == target || sender.isSpectator() || target.isSpectator()) return;
        SocialData data = SocialData.get(sender.serverLevel().getServer());
        UUID senderId = sender.getUUID();
        UUID targetId = target.getUUID();
        String faction = null;
        if (kind == SocialData.InviteKind.FACTION) {
            if (!data.isLeader(senderId)) {
                sender.sendSystemMessage(Component.literal("Приглашать может глава фракции."));
                return;
            }
            if (data.factionOf(targetId) != null) {
                sender.sendSystemMessage(Component.literal("Игроку сначала нужно выйти из своей фракции."));
                return;
            }
            faction = data.factionOf(senderId);
        } else if (data.areFriends(senderId, targetId)) {
            sender.sendSystemMessage(Component.literal("Вы уже друзья."));
            return;
        }

        SocialData.Invite invite = data.invite(kind, targetId, senderId, name(sender), faction,
                sender.serverLevel().getServer().getTickCount());
        if (invite == null) {
            sender.sendSystemMessage(Component.literal("У игрока уже есть активное приглашение этого типа."));
            return;
        }

        String command = ACTION + (kind == SocialData.InviteKind.FACTION ? "faction_invite " : "friend_invite ");
        String message = kind == SocialData.InviteKind.FACTION
                ? name(sender) + " приглашает тебя во фракцию «" + data.factionName(faction) + "»."
                : name(sender) + " предлагает дружбу.";
        target.sendSystemMessage(Component.literal(message + " ")
                .append(button("Принять", command + "accept " + invite.token()))
                .append(" ").append(button("Отклонить", command + "decline " + invite.token())));
        sender.sendSystemMessage(Component.literal("Приглашение отправлено игроку " + name(target) + "."));
    }

    static int respond(ServerPlayer player, SocialData.InviteKind kind, UUID token, boolean accept) {
        MinecraftServer server = player.serverLevel().getServer();
        SocialData data = SocialData.get(server);
        SocialData.Invite invite = data.takeInvite(kind, player.getUUID(), token, server.getTickCount());
        if (invite == null) {
            player.sendSystemMessage(Component.literal("Это приглашение больше не действует."));
            return 0;
        }
        if (!accept) {
            player.sendSystemMessage(Component.literal("Приглашение отклонено."));
            return 1;
        }
        if (kind == SocialData.InviteKind.FACTION) {
            // Между приглашением и принятием состав и полномочия могли измениться.
            if (!data.isLeader(invite.sender()) || !invite.faction().equals(data.factionOf(invite.sender()))
                    || data.factionOf(player.getUUID()) != null
                    || !data.join(invite.faction(), player.getUUID(), name(player), false)) {
                player.sendSystemMessage(Component.literal("Вступление сейчас невозможно."));
                return 0;
            }
            player.sendSystemMessage(Component.literal("Ты вступил во фракцию «" + data.factionName(invite.faction()) + "»."));
        } else {
            data.addFriendship(invite.sender(), invite.senderName(), player.getUUID(), name(player));
            player.sendSystemMessage(Component.literal("Теперь вы друзья с " + invite.senderName() + "."));
        }
        ServerPlayer sender = server.getPlayerList().getPlayer(invite.sender());
        if (sender != null) sender.sendSystemMessage(Component.literal(name(player) + " принял приглашение."));
        return 1;
    }

    public static void showFriends(ServerPlayer player) {
        SocialData data = SocialData.get(player.serverLevel().getServer());
        var friends = data.friendsOf(player.getUUID());
        friends.sort(Comparator.comparing(SocialData.Friend::name, String.CASE_INSENSITIVE_ORDER));
        player.sendSystemMessage(Component.literal("Друзья (" + friends.size() + ") ")
                .append(button("Моя фракция", ACTION + "roster")));
        for (SocialData.Friend friend : friends) {
            player.sendSystemMessage(Component.literal(friend.name() + " ")
                    .append(button("Удалить", ACTION + "friend_confirm " + friend.id()))
                    .append(" ").append(button("Дуэль", "/duel challenge " + friend.name())));
        }
    }

    static MutableComponent button(String label, String command) {
        return Component.literal("[" + label + "]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SocialData.get(player.serverLevel().getServer()).rememberName(player.getUUID(), name(player));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        // Останавливаем и побочные эффекты удара, которые другие обработчики навешивают до урона.
        if (event.getEntity() instanceof ServerPlayer attacker && event.getTarget() instanceof ServerPlayer target
                && areFriends(attacker, target)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer target
                && event.getSource().getEntity() instanceof ServerPlayer attacker
                && areFriends(attacker, target)) event.setCanceled(true);
    }

    private static boolean areFriends(ServerPlayer attacker, ServerPlayer target) {
        var server = attacker.serverLevel().getServer();
        return SocialData.get(server).areFriends(attacker.getUUID(), target.getUUID())
                && !DuelService.opponents(server, attacker.getUUID(), target.getUUID());
    }
}
