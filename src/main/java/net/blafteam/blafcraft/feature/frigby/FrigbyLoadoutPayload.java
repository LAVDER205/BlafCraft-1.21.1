package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FrigbyLoadoutPayload(String choices, String queuedChoices) implements CustomPacketPayload {
    public static final Type<FrigbyLoadoutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "frigby_loadout"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrigbyLoadoutPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(128), FrigbyLoadoutPayload::choices,
                    ByteBufCodecs.stringUtf8(128), FrigbyLoadoutPayload::queuedChoices, FrigbyLoadoutPayload::new);

    public static FrigbyLoadoutPayload request(String choices) {
        return new FrigbyLoadoutPayload(choices, "");
    }

    @Override
    public Type<FrigbyLoadoutPayload> type() {
        return TYPE;
    }

    public static void handleChange(FrigbyLoadoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                    || !FrigbyController.isFrigby(player)) return;
            if (payload.choices().isEmpty()) {
                FrigbyActivity.requestLoadout(player, null); // Пустая команда отменяет очередь.
                return;
            }
            FrigbyLoadout requested = FrigbyLoadout.decode(payload.choices());
            if (requested != null) FrigbyActivity.requestLoadout(player, requested);
        });
    }
}
