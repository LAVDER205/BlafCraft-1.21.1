package net.blafteam.blafcraft.highlight;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HighlightEntityPacket(int entityId, boolean highlight, float r, float g, float b) implements CustomPacketPayload {
    public static final Type<HighlightEntityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "highlight_entity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightEntityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, HighlightEntityPacket::entityId,
                    ByteBufCodecs.BOOL,    HighlightEntityPacket::highlight,
                    ByteBufCodecs.FLOAT,   HighlightEntityPacket::r,
                    ByteBufCodecs.FLOAT,   HighlightEntityPacket::g,
                    ByteBufCodecs.FLOAT,   HighlightEntityPacket::b,
                    HighlightEntityPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
