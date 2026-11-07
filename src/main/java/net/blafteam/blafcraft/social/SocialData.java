package net.blafteam.blafcraft.social;

import net.minecraft.core.HolderLookup;
import net.blafteam.blafcraft.duel.DuelState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Общие данные мира. UUID позволяет управлять составом и дружбой, пока игрок офлайн. */
public final class SocialData extends SavedData {
    public record Member(UUID id, String name, boolean leader) {}
    public record Friend(UUID id, String name) {}
    public enum InviteKind { FACTION, FRIEND }
    public record Invite(UUID token, UUID sender, String senderName, String faction, int expiresAt) {}

    private static final class Faction {
        private final String name;
        private UUID key = UUID.randomUUID();
        private final Map<UUID, Member> members = new LinkedHashMap<>();

        private Faction(String name) {
            this.name = name;
        }
    }

    private final Map<String, Faction> factions = new LinkedHashMap<>();
    private final Map<UUID, String> membership = new HashMap<>();
    private final Map<UUID, Map<UUID, Friend>> friends = new HashMap<>();
    // Активные поединки не записываются в NBT и принадлежат именно этому миру.
    private final DuelState duels = new DuelState();

    public DuelState duels() {
        return duels;
    }
    // Приглашения действуют только в текущем запуске сервера. По одному каждого вида на получателя.
    private final Map<UUID, Invite> factionInvites = new HashMap<>();
    private final Map<UUID, Invite> friendInvites = new HashMap<>();

    public static SocialData get(MinecraftServer server) {
        // Храним один экземпляр в верхнем мире. Измерение игрока на отношения не влияет.
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SocialData::new, SocialData::load), "blafcraft_social");
    }

    public static boolean validId(String id) {
        return id.matches("[a-z0-9_-]{1,32}");
    }

    public boolean createFaction(String id, String name) {
        if (!validId(id) || name.isBlank() || name.length() > 64 || factions.containsKey(id)) return false;
        factions.put(id, new Faction(name));
        setDirty();
        return true;
    }

    public boolean deleteFaction(String id) {
        Faction removed = factions.remove(id);
        if (removed == null) return false;
        removed.members.keySet().forEach(membership::remove);
        factionInvites.values().removeIf(invite -> id.equals(invite.faction()));
        setDirty();
        return true;
    }

    public List<String> factionIds() {
        return List.copyOf(factions.keySet());
    }

    public String factionOf(UUID player) {
        return membership.get(player);
    }

    public String factionName(String id) {
        Faction faction = factions.get(id);
        return faction == null ? id : faction.name;
    }

    public boolean isLeader(UUID player) {
        Faction faction = factions.get(factionOf(player));
        return faction != null && faction.members.get(player).leader();
    }

    public boolean sameFaction(UUID first, UUID second) {
        String id = factionOf(first);
        return id != null && id.equals(factionOf(second));
    }

    public UUID factionKeyOf(UUID player) {
        Faction faction = factions.get(factionOf(player));
        return faction == null ? null : faction.key;
    }

    public List<Member> members(String id) {
        Faction faction = factions.get(id);
        return faction == null ? List.of() : List.copyOf(faction.members.values());
    }

    public boolean join(String factionId, UUID player, String name, boolean leader) {
        Faction faction = factions.get(factionId);
        String existing = factionOf(player);
        if (faction == null || (existing != null && !existing.equals(factionId))) return false;
        // Повторное приглашение не меняет роль. Повысить может админ через leader.
        if (existing != null && !leader) return false;
        membership.put(player, factionId);
        faction.members.put(player, new Member(player, name, leader));
        setDirty();
        return true;
    }

    public boolean demote(String factionId, UUID player) {
        if (!factionId.equals(factionOf(player)) || !isLeader(player)) return false;
        Faction faction = factions.get(factionId);
        Member member = faction.members.get(player);
        faction.members.put(player, new Member(player, member.name(), false));
        factionInvites.values().removeIf(invite -> invite.sender().equals(player));
        setDirty();
        return true;
    }

    public boolean removeMember(String factionId, UUID player) {
        if (!factionId.equals(factionOf(player)) || isLeader(player)) return false;
        factions.get(factionId).members.remove(player);
        membership.remove(player);
        setDirty();
        return true;
    }

    public boolean areFriends(UUID first, UUID second) {
        return friends.getOrDefault(first, Map.of()).containsKey(second);
    }

    public List<Friend> friendsOf(UUID player) {
        return new ArrayList<>(friends.getOrDefault(player, Map.of()).values());
    }

    public void addFriendship(UUID first, String firstName, UUID second, String secondName) {
        if (first.equals(second)) return;
        // Каждая сторона видит другую. Удаление также работает сразу в обе стороны.
        friends.computeIfAbsent(first, id -> new HashMap<>()).put(second, new Friend(second, secondName));
        friends.computeIfAbsent(second, id -> new HashMap<>()).put(first, new Friend(first, firstName));
        setDirty();
    }

    public boolean removeFriendship(UUID first, UUID second) {
        if (!areFriends(first, second)) return false;
        friends.get(first).remove(second);
        friends.get(second).remove(first);
        setDirty();
        return true;
    }

    public void rememberName(UUID player, String name) {
        String factionId = factionOf(player);
        if (factionId != null) {
            Faction faction = factions.get(factionId);
            Member member = faction.members.get(player);
            if (!name.equals(member.name())) {
                faction.members.put(player, new Member(player, name, member.leader()));
                setDirty();
            }
        }
        for (UUID friend : friends.getOrDefault(player, Map.of()).keySet()) {
            Map<UUID, Friend> theirFriends = friends.get(friend);
            if (!name.equals(theirFriends.get(player).name())) {
                theirFriends.put(player, new Friend(player, name));
                setDirty();
            }
        }
    }

    private Map<UUID, Invite> invites(InviteKind kind) {
        return kind == InviteKind.FACTION ? factionInvites : friendInvites;
    }

    public Invite invite(InviteKind kind, UUID recipient, UUID sender, String senderName, String faction, int now) {
        Map<UUID, Invite> pending = invites(kind);
        pending.values().removeIf(invite -> now - invite.expiresAt() >= 0);
        if (pending.containsKey(recipient)) return null;
        Invite invite = new Invite(UUID.randomUUID(), sender, senderName, faction, now + 1200);
        pending.put(recipient, invite);
        return invite;
    }

    public Invite takeInvite(InviteKind kind, UUID recipient, UUID token, int now) {
        Map<UUID, Invite> pending = invites(kind);
        Invite invite = pending.get(recipient);
        // Токен привязан к адресату. Старая кнопка не может принять новое приглашение.
        if (invite == null || !invite.token().equals(token)) return null;
        pending.remove(recipient);
        return now - invite.expiresAt() < 0 ? invite : null;
    }

    public static SocialData load(CompoundTag root, HolderLookup.Provider registries) {
        SocialData data = new SocialData();
        for (Tag tag : root.getList("Factions", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            String id = entry.getString("Id");
            if (!data.createFaction(id, entry.getString("Name"))) continue;
            if (entry.hasUUID("Key")) data.factions.get(id).key = entry.getUUID("Key");
            for (Tag rawMember : entry.getList("Members", Tag.TAG_COMPOUND)) {
                CompoundTag member = (CompoundTag) rawMember;
                if (member.hasUUID("UUID")) {
                    data.join(id, member.getUUID("UUID"), member.getString("Name"), member.getBoolean("Leader"));
                }
            }
        }
        for (Tag tag : root.getList("Friendships", Tag.TAG_COMPOUND)) {
            CompoundTag pair = (CompoundTag) tag;
            if (pair.hasUUID("First") && pair.hasUUID("Second")) {
                data.addFriendship(pair.getUUID("First"), pair.getString("FirstName"),
                        pair.getUUID("Second"), pair.getString("SecondName"));
            }
        }
        data.setDirty(false);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag factionList = new ListTag();
        factions.forEach((id, faction) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", id);
            entry.putString("Name", faction.name);
            entry.putUUID("Key", faction.key);
            ListTag memberList = new ListTag();
            for (Member member : faction.members.values()) {
                CompoundTag record = new CompoundTag();
                record.putUUID("UUID", member.id());
                record.putString("Name", member.name());
                record.putBoolean("Leader", member.leader());
                memberList.add(record);
            }
            entry.put("Members", memberList);
            factionList.add(entry);
        });
        root.put("Factions", factionList);

        ListTag pairs = new ListTag();
        friends.forEach((first, list) -> list.forEach((second, friend) -> {
            // В памяти связь двусторонняя, на диске достаточно одной записи.
            if (first.compareTo(second) >= 0) return;
            CompoundTag pair = new CompoundTag();
            pair.putUUID("First", first);
            pair.putString("FirstName", friends.get(second).get(first).name());
            pair.putUUID("Second", second);
            pair.putString("SecondName", friend.name());
            pairs.add(pair);
        }));
        root.put("Friendships", pairs);
        return root;
    }
}
