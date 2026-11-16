package net.blafteam.blafcraft.character;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент рисует набор, который выдал сервер, включая назначения через команду. */
public record CharacterHudPayload(String kitId) implements CustomPacketPayload {
    public static final Type<CharacterHudPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "character_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterHudPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, CharacterHudPayload::kitId, CharacterHudPayload::new);

    @Override
    public Type<CharacterHudPayload> type() {
        return TYPE;
    }
}
