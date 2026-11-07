package net.blafteam.blafcraft.duel;

import net.blafteam.blafcraft.keybinds.ServerHandler;
import net.blafteam.blafcraft.social.SocialData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.UUID;

public final class DuelService {
    // Командой можно вызвать союзника в пределах 32 блоков в том же измерении.
    private static final double MAX_START_DISTANCE = 32;
    private DuelService() {}

    public static DuelState state(MinecraftServer server) {
        return SocialData.get(server).duels();
    }

    public static boolean opponents(MinecraftServer server, UUID first, UUID second) {
        return state(server).opponents(first, second);
    }

    private static boolean eligible(ServerPlayer first, ServerPlayer second) {
        if (first == second || !first.isAlive() || !second.isAlive()
                || first.isSpectator() || second.isSpectator() || first.isCreative() || second.isCreative()
                || first.serverLevel() != second.serverLevel()
                || first.distanceToSqr(second) > MAX_START_DISTANCE * MAX_START_DISTANCE) return false;
        SocialData social = SocialData.get(first.serverLevel().getServer());
        return social.areFriends(first.getUUID(), second.getUUID()) || social.sameFaction(first.getUUID(), second.getUUID());
    }

    public static int challenge(ServerPlayer sender, ServerPlayer target) {
        if (!eligible(sender, target)) return tell(sender,
                "Для дуэли нужен живой друг или соклановец поблизости в режиме выживания.");
        var state = state(sender.serverLevel().getServer());
        var invite = state.invite(sender.getUUID(), target.getUUID(), sender.serverLevel().getServer().getTickCount());
        if (invite == null) return tell(sender, "Игрок уже участвует в дуэли, восстанавливается или получил другой вызов.");
        target.sendSystemMessage(Component.literal(name(sender) + " вызывает тебя на дуэль. ")
                .append(button("Принять", "/duel accept " + invite.token()))
                .append(" ").append(button("Отклонить", "/duel decline " + invite.token())));
        sender.sendSystemMessage(Component.literal("Вызов отправлен на "
                + DuelState.INVITATION_DURATION_TICKS / 20 + " секунд."));
        return 1;
    }

    public static int respond(ServerPlayer target, UUID token, boolean accept) {
        MinecraftServer server = target.serverLevel().getServer();
        DuelState state = state(server);
        var invite = state.take(target.getUUID(), token, server.getTickCount());
        if (invite == null) return tell(target, "Этот вызов уже не действует.");
        ServerPlayer sender = server.getPlayerList().getPlayer(invite.sender());
        if (!accept) {
            if (sender != null) tell(sender, name(target) + " отклонил вызов.");
            return tell(target, "Вызов отклонён.");
        }
        // Дружба, состав фракции и расстояние могли измениться после отправки.
        if (sender == null || !eligible(sender, target)) return tell(target, "Начать дуэль сейчас нельзя.");
        var match = state.start(sender.getUUID(), target.getUUID(), server.getTickCount());
        if (match == null) return tell(target, "Один из игроков уже участвует в другом поединке.");
        rememberEffects(match, sender);
        rememberEffects(match, target);
        showMenu(sender);
        showMenu(target);
        return 1;
    }

    private static void rememberEffects(DuelState.Match match, ServerPlayer player) {
        match.rememberEffects(player.getUUID(), player.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .map(MobEffectInstance::new).toList());
    }

    public static int showMenu(ServerPlayer player) {
        var server = player.serverLevel().getServer();
        var match = state(server).match(player.getUUID());
        if (match == null) return tell(player, "Дуэли нет. Наведись на друга или соклановца и нажми клавишу дуэли.");
        var opponent = server.getPlayerList().getPlayer(match.opponent(player.getUUID()));
        String opponentName = opponent == null ? "игрок" : name(opponent);
        player.sendSystemMessage(Component.literal("Дуэль с " + opponentName + ". ")
                .append(button("Предложить завершение", "/duel stop " + match.id())));
        return 1;
    }

    public static int stop(ServerPlayer player, UUID session) {
        var server = player.serverLevel().getServer();
        var state = state(server);
        var match = state.match(player.getUUID());
        if (match == null || !match.id().equals(session)) return tell(player, "Эта дуэль уже завершена.");
        if (match.votedToStop(player.getUUID())) return tell(player, "Ожидаем согласия второго участника.");
        if (state.voteToStop(player.getUUID(), session)) {
            finish(player, "Дуэль завершена по взаимному согласию.");
        } else {
            tell(player, "Предложение отправлено. Поединок продолжается до согласия соперника.");
            ServerPlayer opponent = server.getPlayerList().getPlayer(match.opponent(player.getUUID()));
            if (opponent != null) opponent.sendSystemMessage(Component.literal(name(player) + " предлагает закончить дуэль. ")
                    .append(button("Согласиться", "/duel stop " + session)));
        }
        return 1;
    }

    public static void finish(ServerPlayer participant, String reason) {
        MinecraftServer server = participant.serverLevel().getServer();
        var match = state(server).finish(participant.getUUID(), server.getTickCount());
        if (match == null) return;
        cleanUp(match.first().equals(participant.getUUID()) ? participant : server.getPlayerList().getPlayer(match.first()),
                match, server.getTickCount());
        cleanUp(match.second().equals(participant.getUUID()) ? participant : server.getPlayerList().getPlayer(match.second()),
                match, server.getTickCount());
        for (UUID id : new UUID[]{match.first(), match.second()}) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) player.sendSystemMessage(Component.literal(reason));
        }
    }

    private static void cleanUp(ServerPlayer player, DuelState.Match match, int now) {
        if (player == null) return;
        ServerHandler.cancelPendingActions(player.getUUID());
        player.clearFire();
        // Убираем вредные состояния боя, чтобы кровотечение не убило сразу после поражения.
        // Состояния, которые были до дуэли, восстанавливаем с учётом прошедшего времени.
        for (var effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
        int elapsed = now - match.startedAt();
        for (var original : match.originalEffects(player.getUUID())) {
            boolean infinite = original.isInfiniteDuration();
            int duration = infinite ? -1 : original.getDuration() - elapsed;
            if (infinite || duration > 0) player.addEffect(new MobEffectInstance(original.getEffect(), duration,
                    original.getAmplifier(), original.isAmbient(), original.isVisible(), original.showIcon()));
        }
    }

    public static MutableComponent button(String text, String command) {
        return Component.literal("[" + text + "]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
                .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static String name(ServerPlayer player) { return player.getGameProfile().getName(); }

    private static int tell(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text));
        return 1;
    }
}
