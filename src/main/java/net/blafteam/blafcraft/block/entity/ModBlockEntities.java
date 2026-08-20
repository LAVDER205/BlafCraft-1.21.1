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

//    public static final Supplier<BlockEntityType<RuneStoreBlockEntity>> RUNE_STORE_BE =
//            BLOCK_ENTITIES.register("rune_store_be", () -> BlockEntityType.Builder.of(
//                    RuneStoreBlockEntity::new, ModBlocks.RUNE_STORE_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<RuneBlockEntity>> RUNE_ACTION_BE =
            BLOCK_ENTITIES.register("rune_action_be", () -> BlockEntityType.Builder.of(
                    RuneBlockEntity::new, ModBlocks.RUNE_BLOCK.get()).build(null));

//    public static final Supplier<BlockEntityType<RuneCraftBlockEntity>> RUNE_CRAFT_BE =
//            BLOCK_ENTITIES.register("rune_craft_be", () -> BlockEntityType.Builder.of(
//                    RuneCraftBlockEntity::new, ModBlocks.RUNE_CRAFT_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
