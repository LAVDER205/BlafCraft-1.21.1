package net.blafteam.blafcraft.custom;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record FriendUpdatePacket(UUID targetUUID, boolean add) implements CustomPacketPayload {

    public static final Type<FriendUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "friend_update"));

    // Кодек для сериализации
    public static final StreamCodec<RegistryFriendlyByteBuf, FriendUpdatePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeUUID(packet.targetUUID);
                        buf.writeBoolean(packet.add);
                    },
                    buf -> new FriendUpdatePacket(
                            buf.readUUID(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
