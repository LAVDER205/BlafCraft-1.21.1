package net.blafteam.blafcraft.mana;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ManaSyncPayload(float currentMana, int maxMana) implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "mana_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, ManaSyncPayload::currentMana,
                    ByteBufCodecs.VAR_INT, ManaSyncPayload::maxMana,
                    ManaSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
