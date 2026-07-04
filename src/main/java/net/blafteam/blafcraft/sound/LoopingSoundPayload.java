package net.blafteam.blafcraft.sound;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;

public record LoopingSoundPayload(SoundEvent sound, float volume, float pitch, boolean start) implements CustomPacketPayload {
    public static final Type<LoopingSoundPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "looping_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoopingSoundPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(BuiltInRegistries.SOUND_EVENT.key()), LoopingSoundPayload::sound,
                    ByteBufCodecs.FLOAT, LoopingSoundPayload::volume,
                    ByteBufCodecs.FLOAT, LoopingSoundPayload::pitch,
                    ByteBufCodecs.BOOL, LoopingSoundPayload::start,
                    LoopingSoundPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
