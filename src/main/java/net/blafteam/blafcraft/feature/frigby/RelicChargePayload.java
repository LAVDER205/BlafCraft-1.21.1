package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RelicChargePayload(int points) implements CustomPacketPayload {
    public static final Type<RelicChargePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "relic_charge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RelicChargePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RelicChargePayload::points, RelicChargePayload::new);

    @Override
    public Type<RelicChargePayload> type() {
        return TYPE;
    }
}
