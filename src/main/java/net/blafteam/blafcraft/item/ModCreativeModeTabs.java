package net.blafteam.blafcraft.item;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlafCraft.MODID);

    public static final Supplier<CreativeModeTab> BLAFCRAFT_ITEMS_TAB = CREATIVE_MODE_TAB .register("blafcraft_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BISMUTH.get()))
                    .title(Component.translatable("creativetab.blafcraft.blafcraft_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.BISMUTH);
                        output.accept(ModItems.RAW_BISMUTH);
                    }).build());

    public static final Supplier<CreativeModeTab> BLAFCRAFT_BLOCKS_TAB = CREATIVE_MODE_TAB .register("blafcraft_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.BISMUTH_BLOCK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "blafcraft_items_tab")) // order
                    .title(Component.translatable("creativetab.blafcraft.blafcraft_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.BISMUTH_BLOCK);
                        output.accept(ModBlocks.BISMUTH_ORE);
                        output.accept(ModBlocks.BISMUTH_DEEPSLATE_ORE);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }


}
