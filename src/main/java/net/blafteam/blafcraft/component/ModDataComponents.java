package net.blafteam.blafcraft.component;

import com.mojang.serialization.Codec;
import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(BlafCraft.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> COORDINATES = register("coordinates",
            builder -> builder.persistent(BlockPos.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> COLOR = register("color",
            builder -> builder.persistent(DyeColor.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COUNTER = register("counter",
            builder -> builder.persistent(Codec.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> ENTITY = register("entity",
            builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));



    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name,
                                                                                          UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
