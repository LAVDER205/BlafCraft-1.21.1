package net.blafteam.blafcraft.component;

import net.minecraft.resources.ResourceLocation;
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

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ResourceLocation>> ENTITY =
            ATTACHMENT_TYPES.register("morph_entity", () -> AttachmentType.builder(() -> (ResourceLocation) null).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<UUID>>> FRIEND_LIST =
            ATTACHMENT_TYPES.register("friend_list",
                    () -> AttachmentType.<List<UUID>>builder(() -> new ArrayList<>()).build());
}
