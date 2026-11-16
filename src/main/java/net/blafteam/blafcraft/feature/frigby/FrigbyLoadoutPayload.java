package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FrigbyLoadoutPayload(String choices) implements CustomPacketPayload {
    public static final Type<FrigbyLoadoutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "frigby_loadout"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrigbyLoadoutPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(128), FrigbyLoadoutPayload::choices, FrigbyLoadoutPayload::new);

    @Override
    public Type<FrigbyLoadoutPayload> type() {
        return TYPE;
    }

    public static void handleChange(FrigbyLoadoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                    || !FrigbyController.isFrigby(player)) return;
            FrigbyLoadout requested = FrigbyLoadout.decode(payload.choices());
            if (requested == null) return;
            int remaining = FrigbyActivity.remaining(player);
            if (remaining > 0) {
                player.displayClientMessage(Component.literal("Смена набора через " + ((remaining + 19) / 20) + " с"), true);
                return;
            }
            requested.apply(player);
            FrigbyActivity.syncLoadout(player);
            player.displayClientMessage(Component.literal("Набор созидания сохранён"), true);
        });
    }
}
