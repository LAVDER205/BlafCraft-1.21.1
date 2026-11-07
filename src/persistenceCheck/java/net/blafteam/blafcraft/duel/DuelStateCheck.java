package net.blafteam.blafcraft.duel;

import java.util.UUID;

public final class DuelStateCheck {
    public static void main(String[] args) {
        DuelState state = new DuelState();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        check(state.invite(first, first, 0) == null, "self-challenge");
        var invite = state.invite(first, second, 0);
        check(invite != null && state.invite(first, third, 0) == null, "one outgoing invitation");
        check(state.take(third, invite.token(), 1) == null, "recipient authentication");
        check(state.take(second, UUID.randomUUID(), 1) == null, "wrong token");
        check(state.take(second, invite.token(), 1200) == null, "expiry");
        invite = state.invite(first, second, 1201);
        check(state.take(second, invite.token(), 1202) != null
                && state.take(second, invite.token(), 1202) == null, "one-time acceptance");
        var match = state.start(first, second, 1202);
        check(match != null && state.opponents(first, second) && state.opponents(second, first), "symmetric opponents");
        check(!state.opponents(first, third) && !state.opponents(first, first), "third party and self");
        check(state.start(third, second, 1202) == null, "one duel per player");
        check(!state.voteToStop(first, match.id()) && !state.voteToStop(first, match.id()), "two different votes required");
        check(!state.voteToStop(third, match.id()) && !state.voteToStop(second, UUID.randomUUID()), "session ownership");
        check(state.voteToStop(second, match.id()), "mutual agreement");
        check(state.finish(first, 1203) == match && state.match(first) == null && state.match(second) == null,
                "both participants released");
        check(state.finish(second, 1203) == null, "finish is idempotent");
        check(state.start(first, second, 1204) == null && state.recovering(first, 1262), "recovery protects loser");
        var next = state.start(first, second, 1263);
        check(next != null && !state.voteToStop(first, match.id()), "old button cannot end new duel");
        check(!next.votedToStop(first) && !next.votedToStop(second), "votes do not leak");
        state.finish(second, 1264);
        check(!state.opponents(first, second), "lethal or disconnect end restores normal relations");
        check(new DuelState().match(first) == null, "world instances do not share state");
        System.out.println("Duel invitation and lifecycle checks passed.");
    }

    private static void check(boolean condition, String scenario) {
        if (!condition) throw new AssertionError(scenario);
    }
}
