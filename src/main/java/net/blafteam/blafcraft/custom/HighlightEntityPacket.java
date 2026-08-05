package net.blafteam.blafcraft.custom;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HighlightEntityPacket(int entityId, boolean highlight) implements CustomPacketPayload {
    public static final Type<HighlightEntityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "highlight_entity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightEntityPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.entityId);
                buf.writeBoolean(packet.highlight);
            },
            buf -> new HighlightEntityPacket(buf.readInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
