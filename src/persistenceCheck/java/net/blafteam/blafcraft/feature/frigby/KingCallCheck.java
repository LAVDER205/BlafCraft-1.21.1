package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.social.SocialData;

import java.util.UUID;

public final class KingCallCheck {
    public static void main(String[] args) {
        var social = new SocialData();
        UUID caller = UUID.randomUUID();
        UUID ally = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        social.createFaction("north", "Север");
        social.createFaction("south", "Юг");
        social.join("north", caller, "Caller", true);
        social.join("north", ally, "Ally", false);
        social.join("south", outsider, "Outsider", false);
        social.addFriendship(caller, "Caller", outsider, "Outsider");

        UUID faction = social.factionKeyOf(caller);
        check(KingCall.canReceive(social, faction, caller, ally, true), "same faction in the same dimension");
        check(!KingCall.canReceive(social, faction, caller, ally, false), "offline or in another dimension");
        check(!KingCall.canReceive(social, faction, caller, caller, true), "no entrance for the caller");
        check(!KingCall.canReceive(social, faction, caller, outsider, true), "friendship does not grant faction access");

        social.removeMember("north", ally);
        check(!KingCall.canReceive(social, faction, caller, ally, true), "membership is rechecked");
        social.join("north", ally, "Ally", false);
        social.demote("north", caller);
        check(!KingCall.canReceive(social, faction, caller, ally, true), "demoting the caller closes access");

        social.deleteFaction("north");
        social.createFaction("north", "Новый Север");
        social.join("north", caller, "Caller", true);
        social.join("north", ally, "Ally", false);
        check(!KingCall.canReceive(social, faction, caller, ally, true), "recreating the faction does not revive old portals");
        System.out.println("King's Call faction, dimension and leadership checks passed.");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
