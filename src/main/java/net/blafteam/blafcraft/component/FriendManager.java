package net.blafteam.blafcraft.component;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class FriendManager {
    public static boolean addFriend(ServerPlayer player, ServerPlayer friend) {
        List<UUID> friends = player.getData(ModAttachments.FRIEND_LIST.get());
        if (friends.contains(friend.getUUID())) return false;
        friends.add(friend.getUUID());
        player.setData(ModAttachments.FRIEND_LIST.get(), friends); // возможно, нужно обновить ссылку
        player.sendSystemMessage(Component.literal(friend.getDisplayName().getString() + " is your friend now!"));
        return true;
    }

    public static boolean removeFriend(ServerPlayer player, ServerPlayer friend) {
        List<UUID> friends = player.getData(ModAttachments.FRIEND_LIST.get());
        boolean removed = friends.remove(friend.getUUID());
        if (removed) {
            player.setData(ModAttachments.FRIEND_LIST.get(), friends);
            player.sendSystemMessage(Component.literal(friend.getDisplayName().getString() + " is not your friend anymore..."));
        }
        return removed;
    }

    public static boolean isFriend(ServerPlayer player, ServerPlayer other) {
        List<UUID> friends = player.getData(ModAttachments.FRIEND_LIST.get());
        return friends.contains(other.getUUID());
    }
}
