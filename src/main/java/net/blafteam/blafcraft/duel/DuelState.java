package net.blafteam.blafcraft.duel;

import net.minecraft.world.effect.MobEffectInstance;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Короткоживущие данные одного мира. При перезапуске приглашения и дуэли завершаются. */
public final class DuelState {
    // 20 тиков — одна секунда. Эти два значения можно менять для всех дуэлей.
    public static final int INVITATION_DURATION_TICKS = 20 * 60;
    public static final int RECOVERY_DURATION_TICKS = 20 * 3;
    public record Invitation(UUID token, UUID sender, UUID recipient, int expiresAt) {}

    public static final class Match {
        private final UUID id = UUID.randomUUID();
        private final UUID first;
        private final UUID second;
        private final Set<UUID> stopVotes = new HashSet<>();
        private final Map<UUID, List<MobEffectInstance>> originalEffects = new HashMap<>();
        private final int startedAt;

        private Match(UUID first, UUID second, int startedAt) {
            this.first = first;
            this.second = second;
            this.startedAt = startedAt;
        }

        public UUID id() { return id; }
        public UUID first() { return first; }
        public UUID second() { return second; }
        public UUID opponent(UUID player) { return first.equals(player) ? second : first; }
        public boolean votedToStop(UUID player) { return stopVotes.contains(player); }
        public int startedAt() { return startedAt; }
        public void rememberEffects(UUID player, List<MobEffectInstance> effects) {
            originalEffects.put(player, effects);
        }
        public List<MobEffectInstance> originalEffects(UUID player) {
            return originalEffects.getOrDefault(player, List.of());
        }
    }

    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private final Map<UUID, Match> active = new HashMap<>();
    private final Map<UUID, Integer> recovery = new HashMap<>();

    public Match match(UUID player) { return active.get(player); }

    public boolean opponents(UUID first, UUID second) {
        Match match = active.get(first);
        return match != null && !first.equals(second) && match.opponent(first).equals(second);
    }

    public boolean recovering(UUID player, int now) {
        Integer until = recovery.get(player);
        if (until == null) return false;
        if (now - until < 0) return true;
        recovery.remove(player);
        return false;
    }

    public Invitation invite(UUID sender, UUID recipient, int now) {
        invitations.values().removeIf(invite -> now - invite.expiresAt() >= 0);
        if (sender.equals(recipient) || active.containsKey(sender) || active.containsKey(recipient)
                || recovering(sender, now) || recovering(recipient, now)) return null;
        // Один исходящий и один входящий вызов предотвращают повторную рассылку.
        if (invitations.containsKey(recipient)
                || invitations.values().stream().anyMatch(invite -> invite.sender().equals(sender))) return null;
        Invitation invite = new Invitation(UUID.randomUUID(), sender, recipient, now + INVITATION_DURATION_TICKS);
        invitations.put(recipient, invite);
        return invite;
    }

    public Invitation take(UUID recipient, UUID token, int now) {
        Invitation invite = invitations.get(recipient);
        if (invite == null || !invite.token().equals(token)) return null;
        invitations.remove(recipient);
        return now - invite.expiresAt() < 0 ? invite : null;
    }

    public Match start(UUID first, UUID second, int now) {
        if (first.equals(second) || active.containsKey(first) || active.containsKey(second)
                || recovering(first, now) || recovering(second, now)) return null;
        Match match = new Match(first, second, now);
        active.put(first, match);
        active.put(second, match);
        clearInvitations(first);
        clearInvitations(second);
        return match;
    }

    public boolean voteToStop(UUID player, UUID matchId) {
        Match match = active.get(player);
        if (match == null || !match.id.equals(matchId)) return false;
        match.stopVotes.add(player);
        return match.stopVotes.size() == 2;
    }

    public Match finish(UUID player, int now) {
        Match match = active.remove(player);
        if (match == null) return null;
        active.remove(match.opponent(player));
        // Три секунды на выход из огня и прекращение цепочек урона после дуэли.
        recovery.put(match.first, now + RECOVERY_DURATION_TICKS);
        recovery.put(match.second, now + RECOVERY_DURATION_TICKS);
        return match;
    }

    public void clearInvitations(UUID player) {
        invitations.values().removeIf(invite -> invite.sender().equals(player) || invite.recipient().equals(player));
    }
}
