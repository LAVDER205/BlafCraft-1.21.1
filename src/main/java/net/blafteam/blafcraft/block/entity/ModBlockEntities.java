package net.blafteam.blafcraft.block.entity;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BlafCraft.MODID);

    public static final Supplier<BlockEntityType<RealizerBlockEntity>> REALIZER_BE =
            BLOCK_ENTITIES.register("realizer_be", () -> BlockEntityType.Builder.of(
                    RealizerBlockEntity::new, ModBlocks.REALIZER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
