package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActionPacket(ActionType action) implements CustomPacketPayload {

    public static final Type<ActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "action"));

    // Кодек: записываем имя enum как строку (очень просто)
    public static final StreamCodec<RegistryFriendlyByteBuf, ActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(
                            name -> ActionType.valueOf(name),   // из строки → enum
                            action -> action.name()             // из enum → строка
                    ),
                    ActionPacket::action,
                    ActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}