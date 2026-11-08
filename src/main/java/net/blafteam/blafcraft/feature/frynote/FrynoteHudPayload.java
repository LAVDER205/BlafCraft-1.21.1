package net.blafteam.blafcraft.feature.frynote;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FrynoteHudPayload(boolean enabled, int charges, int recharge, int glance, int anchorLife, int anchorCooldown)
        implements CustomPacketPayload {
    public static final Type<FrynoteHudPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "frynote_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrynoteHudPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeBoolean(value.enabled());
                buffer.writeVarInt(value.charges());
                buffer.writeVarInt(value.recharge());
                buffer.writeVarInt(value.glance());
                buffer.writeVarInt(value.anchorLife());
                buffer.writeVarInt(value.anchorCooldown());
            },
            buffer -> new FrynoteHudPayload(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Type<FrynoteHudPayload> type() { return TYPE; }
}
