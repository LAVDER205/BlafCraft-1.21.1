package net.blafteam.blafcraft.social;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.UUID;

import static net.blafteam.blafcraft.social.SocialInteractions.ACTION;
import static net.blafteam.blafcraft.social.SocialInteractions.button;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class SocialCommands {
    private SocialCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("blafcraft")
                .then(adminCommands())
                .then(playerActions()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminCommands() {
        return Commands.literal("faction").requires(source -> source.hasPermission(2))
                .then(Commands.literal("create").then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(context -> {
                            var source = context.getSource();
                            boolean created = SocialData.get(source.getServer()).createFaction(
                                    StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "name"));
                            return result(source, created, "Фракция создана.",
                                    "Не удалось создать фракцию. Проверь уникальность ID и название.");
                        }))))
                .then(Commands.literal("delete").then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> result(context.getSource(),
                                SocialData.get(context.getSource().getServer()).deleteFaction(StringArgumentType.getString(context, "id")),
                                "Фракция удалена.", "Такой фракции нет."))))
                .then(Commands.literal("list").executes(context -> {
                    var source = context.getSource();
                    var data = SocialData.get(source.getServer());
                    for (String id : data.factionIds()) {
                        source.sendSuccess(() -> Component.literal(id + " — " + data.factionName(id)), false);
                    }
                    return 1;
                }))
                .then(adminRole("appoint", true))
                .then(adminRole("demote", false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminRole(String literal, boolean appoint) {
        return Commands.literal(literal).then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(context -> {
                    var source = context.getSource();
                    var profiles = GameProfileArgument.getGameProfiles(context, "player");
                    if (profiles.size() != 1) return result(source, false, "", "Укажи одного игрока.");
                    var profile = profiles.iterator().next();
                    var data = SocialData.get(source.getServer());
                    String id = StringArgumentType.getString(context, "id");
                    boolean changed = appoint ? data.join(id, profile.getId(), profile.getName(), true)
                            : data.demote(id, profile.getId());
                    return result(source, changed, appoint ? "Глава назначен." : "Глава снят.",
                            "Проверь фракцию и текущую роль игрока. Для перехода сначала нужно выйти из прежней фракции.");
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> playerActions() {
        // Эти действия вызывают кнопки книги и чата. Все проверки выполняются и при ручном вводе.
        return Commands.literal("social")
                .then(Commands.literal("roster").executes(context -> showRoster(context.getSource().getPlayerOrException())))
                .then(Commands.literal("leave_confirm").executes(context -> confirmLeave(context.getSource().getPlayerOrException())))
                .then(Commands.literal("leave").executes(context -> leave(context.getSource().getPlayerOrException())))
                .then(Commands.literal("kick_confirm").then(Commands.argument("target", UuidArgument.uuid())
                        .executes(context -> kick(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "target"), false))))
                .then(Commands.literal("kick").then(Commands.argument("target", UuidArgument.uuid())
                        .executes(context -> kick(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "target"), true))))
                .then(Commands.literal("friend_confirm").then(Commands.argument("target", UuidArgument.uuid())
                        .executes(context -> removeFriend(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "target"), false))))
                .then(Commands.literal("friend_remove").then(Commands.argument("target", UuidArgument.uuid())
                        .executes(context -> removeFriend(context.getSource().getPlayerOrException(), UuidArgument.getUuid(context, "target"), true))))
                .then(inviteResponse("faction_invite", SocialData.InviteKind.FACTION))
                .then(inviteResponse("friend_invite", SocialData.InviteKind.FRIEND));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inviteResponse(String literal, SocialData.InviteKind kind) {
        return Commands.literal(literal)
                .then(response("accept", kind, true))
                .then(response("decline", kind, false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> response(String literal, SocialData.InviteKind kind, boolean accept) {
        return Commands.literal(literal).then(Commands.argument("token", UuidArgument.uuid())
                .executes(context -> SocialInteractions.respond(context.getSource().getPlayerOrException(), kind,
                        UuidArgument.getUuid(context, "token"), accept)));
    }

    static int showRoster(ServerPlayer player) {
        var server = player.serverLevel().getServer();
        var data = SocialData.get(server);
        UUID id = player.getUUID();
        String faction = data.factionOf(id);
        if (faction == null) return message(player, false, "Ты не состоишь во фракции.");
        if (!data.isLeader(id)) {
            player.sendSystemMessage(Component.literal("Фракция «" + data.factionName(faction) + "» ")
                    .append(button("Выйти", ACTION + "leave_confirm")));
            return 1;
        }
        var members = data.members(faction);
        player.sendSystemMessage(Component.literal("Фракция «" + data.factionName(faction) + "» (" + members.size() + ")"));
        members.stream().sorted(Comparator.comparing(SocialData.Member::leader).reversed()
                .thenComparing(SocialData.Member::name, String.CASE_INSENSITIVE_ORDER)).forEach(member -> {
                    String presence = server.getPlayerList().getPlayer(member.id()) == null ? " (офлайн)" : "";
                    var line = Component.literal(member.name() + presence + (member.leader() ? " — глава" : " "));
                    if (!member.leader()) line.append(button("Исключить", ACTION + "kick_confirm " + member.id()));
                    if (!member.id().equals(id) && server.getPlayerList().getPlayer(member.id()) != null) {
                        line.append(" ").append(button("Дуэль", "/duel challenge " + member.name()));
                    }
                    player.sendSystemMessage(line);
                });
        return 1;
    }

    private static int confirmLeave(ServerPlayer player) {
        var data = SocialData.get(player.serverLevel().getServer());
        if (data.factionOf(player.getUUID()) == null || data.isLeader(player.getUUID())) {
            return message(player, false, "Выход недоступен. Главе сначала нужно снять полномочия у админа.");
        }
        player.sendSystemMessage(Component.literal("Покинуть свою фракцию? ")
                .append(button("Подтвердить", ACTION + "leave")));
        return 1;
    }

    private static int leave(ServerPlayer player) {
        var data = SocialData.get(player.serverLevel().getServer());
        String faction = data.factionOf(player.getUUID());
        boolean removed = faction != null && data.removeMember(faction, player.getUUID());
        return message(player, removed, removed ? "Ты покинул фракцию." : "Выход сейчас недоступен.");
    }

    private static int kick(ServerPlayer leader, UUID target, boolean confirmed) {
        var server = leader.serverLevel().getServer();
        var data = SocialData.get(server);
        String faction = data.factionOf(leader.getUUID());
        if (!data.isLeader(leader.getUUID()) || !faction.equals(data.factionOf(target)) || data.isLeader(target)) {
            return message(leader, false, "Исключить этого игрока нельзя.");
        }
        if (!confirmed) {
            String name = data.members(faction).stream().filter(member -> member.id().equals(target))
                    .findFirst().orElseThrow().name();
            leader.sendSystemMessage(Component.literal("Исключить " + name + "? ")
                    .append(button("Подтвердить", ACTION + "kick " + target)));
            return 1;
        }
        data.removeMember(faction, target);
        ServerPlayer online = server.getPlayerList().getPlayer(target);
        if (online != null) online.sendSystemMessage(Component.literal("Тебя исключили из фракции «" + data.factionName(faction) + "»."));
        return message(leader, true, "Игрок исключён.");
    }

    private static int removeFriend(ServerPlayer player, UUID target, boolean confirmed) {
        var server = player.serverLevel().getServer();
        var data = SocialData.get(server);
        if (!data.areFriends(player.getUUID(), target)) return message(player, false, "Игрока уже нет в списке.");
        if (!confirmed) {
            String name = data.friendsOf(player.getUUID()).stream().filter(friend -> friend.id().equals(target))
                    .findFirst().orElseThrow().name();
            player.sendSystemMessage(Component.literal("Удалить " + name + " из друзей? ")
                    .append(button("Подтвердить", ACTION + "friend_remove " + target)));
            return 1;
        }
        data.removeFriendship(player.getUUID(), target);
        ServerPlayer online = server.getPlayerList().getPlayer(target);
        if (online != null) online.sendSystemMessage(Component.literal(SocialInteractions.name(player) + " прекратил дружбу."));
        return message(player, true, "Дружба прекращена.");
    }

    private static int message(ServerPlayer player, boolean success, String text) {
        player.sendSystemMessage(Component.literal(text));
        return success ? 1 : 0;
    }

    private static int result(CommandSourceStack source, boolean success, String ok, String error) {
        if (success) source.sendSuccess(() -> Component.literal(ok), true);
        else source.sendFailure(Component.literal(error));
        return success ? 1 : 0;
    }
}
