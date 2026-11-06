package net.blafteam.blafcraft.social;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.nio.file.Files;
import java.util.UUID;

public final class SocialPersistenceCheck {
    public static void main(String[] args) throws Exception {
        UUID leader = UUID.randomUUID();
        UUID coLeader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID neutral = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        SocialData data = new SocialData();

        check(data.createFaction("north", "Север") && data.createFaction("south", "Юг"), "create factions");
        check(data.join("north", leader, "Leader", true), "appoint leader");
        check(data.join("north", coLeader, "CoLeader", true), "appoint another leader");
        check(data.join("north", member, "Member", false), "join faction");
        check(!data.join("south", member, "Member", true), "prevent forced faction transfer");
        check(!data.removeMember("north", coLeader), "prevent removing another leader");
        check(data.sameFaction(leader, member) && !data.sameFaction(neutral, outsider), "neutral is not a shared faction");
        data.addFriendship(leader, "Leader", neutral, "Neutral");
        check(data.areFriends(neutral, leader), "friendship is mutual");

        var invite = data.invite(SocialData.InviteKind.FACTION, outsider, leader, "Leader", "north", 10);
        check(invite != null, "create invitation");
        check(data.takeInvite(SocialData.InviteKind.FACTION, neutral, invite.token(), 11) == null, "reject wrong recipient");
        check(data.takeInvite(SocialData.InviteKind.FACTION, outsider, invite.token(), 1210) == null, "reject expired invite");
        var valid = data.invite(SocialData.InviteKind.FACTION, outsider, leader, "Leader", "north", 1211);
        check(data.takeInvite(SocialData.InviteKind.FACTION, outsider, valid.token(), 1212) != null, "accept once");
        check(data.takeInvite(SocialData.InviteKind.FACTION, outsider, valid.token(), 1213) == null, "reject replay");

        var file = Files.createTempFile("blafcraft-social", ".dat");
        try {
            NbtIo.writeCompressed(data.save(new CompoundTag(), RegistryAccess.EMPTY), file);
            var loaded = SocialData.load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), RegistryAccess.EMPTY);
            check(loaded.members("north").size() == 3 && loaded.isLeader(coLeader), "restore offline members and roles");
            UUID oldFactionKey = loaded.factionKeyOf(leader);
            check(data.factionKeyOf(leader).equals(oldFactionKey), "restore permanent faction identity");
            check(loaded.removeFriendship(neutral, leader) && !loaded.areFriends(leader, neutral), "remove both sides offline");
            check(loaded.deleteFaction("north") && loaded.factionOf(member) == null, "clear membership on deletion");
            check(loaded.createFaction("north", "Новый Север") && loaded.join("north", leader, "Leader", true),
                    "reuse human-readable ID");
            check(!oldFactionKey.equals(loaded.factionKeyOf(leader)), "old factons stay detached from the new faction");
        } finally {
            Files.deleteIfExists(file);
        }
        System.out.println("Faction and friendship checks passed.");
    }

    private static void check(boolean condition, String scenario) {
        if (!condition) throw new AssertionError(scenario);
    }
}
