package net.blafteam.blafcraft.component;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.UUIDUtil;
import net.blafteam.blafcraft.mana.ManaData;
import net.blafteam.blafcraft.feature.frynote.FrynoteData;
import java.util.Objects;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "blafcraft");

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FrynoteData>> FRYNOTE =
            ATTACHMENT_TYPES.register("frynote", () -> AttachmentType.serializable(FrynoteData::new).copyOnDeath().build());

    // Абсолютное время мира: выход из игры и смерть не сбрасывают перезарядку.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> KING_CALL_READY_AT =
            ATTACHMENT_TYPES.register("king_call_ready_at", () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG, time -> time > 0).copyOnDeath().build());

    // Запас привязан к игроку. Поднятая или заново изготовленная реликвия использует тот же заряд.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> RELIC_CHARGE =
            ATTACHMENT_TYPES.register("relic_charge", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT, charge -> charge > 0).copyOnDeath().build());

    // Пустая строка означает старого игрока без явного назначения. "none" отключает набор.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> CHARACTER_ID =
            ATTACHMENT_TYPES.register("character_id", () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING, id -> !id.isEmpty()).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ManaData>> MANA =
            ATTACHMENT_TYPES.register("mana", () -> AttachmentType.builder(ManaData::defaults).serialize(ManaData.CODEC).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ResourceLocation>> ENTITY =
            ATTACHMENT_TYPES.register("morph_entity", () -> AttachmentType.builder(() -> (ResourceLocation) null).serialize(ResourceLocation.CODEC, Objects::nonNull).copyOnDeath().build());

    // Старые односторонние списки остаются читаемыми. Новая взаимная дружба хранится в SocialData.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<UUID>>> FRIEND_LIST =
            ATTACHMENT_TYPES.register("friend_list",
                    () -> AttachmentType.<List<UUID>>builder(() -> new ArrayList<>()).serialize(UUIDUtil.CODEC.listOf().xmap(ArrayList::new, ArrayList::new)).copyOnDeath().build());
}
