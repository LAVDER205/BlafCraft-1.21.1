package net.blafteam.blafcraft.morph;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MorphSyncPayload(ResourceLocation morphId) implements CustomPacketPayload {
    public static final Type<MorphSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "morph_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, MorphSyncPayload::morphId,
                    MorphSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
