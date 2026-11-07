package net.blafteam.blafcraft.duel;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record DuelShortcutPayload(UUID target) implements CustomPacketPayload {
    public static final Type<DuelShortcutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "duel_shortcut"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DuelShortcutPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeUUID(payload.target()),
                    buffer -> new DuelShortcutPayload(buffer.readUUID()));

    @Override
    public Type<DuelShortcutPayload> type() { return TYPE; }

    public static void handle(DuelShortcutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.getUUID().equals(payload.target())) {
                DuelService.showMenu(player);
            } else if (player.serverLevel().getEntity(payload.target()) instanceof ServerPlayer target
                    && player.distanceToSqr(target) <= 36 && player.hasLineOfSight(target)) {
                DuelService.challenge(player, target);
            }
        });
    }
}
