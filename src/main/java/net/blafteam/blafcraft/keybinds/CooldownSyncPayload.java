package net.blafteam.blafcraft.keybinds;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CooldownSyncPayload(ActionType action, int ticks) implements CustomPacketPayload {
    public static final Type<CooldownSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "ability_cooldown"));

    // Передаём имя способности. Порядок значений в ActionType можно менять.
    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(ActionType::valueOf, ActionType::name), CooldownSyncPayload::action,
            ByteBufCodecs.VAR_INT, CooldownSyncPayload::ticks,
            CooldownSyncPayload::new);

    @Override
    public Type<CooldownSyncPayload> type() {
        return TYPE;
    }
}
