package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActionPacket(int slot) implements CustomPacketPayload {
    // Новый ID отделяет формат слота от старого формата с именем способности.
    public static final Type<ActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "ability_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ActionPacket::slot, ActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
