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
                        output.accept(ModItems.CHISEL);
                        output.accept(ModItems.RADISH);
                        output.accept(ModItems.FROSTFIRE_ICE);
                        output.accept(ModItems.STARLIGHT_ASHES);

                        output.accept(ModItems.BISMUTH_SWORD);
                        output.accept(ModItems.BISMUTH_PICKAXE);
                        output.accept(ModItems.BISMUTH_SHOVEL);
                        output.accept(ModItems.BISMUTH_AXE);
                        output.accept(ModItems.BISMUTH_HOE);

                        output.accept(ModItems.BISMUTH_HAMMER);

                        output.accept(ModItems.BISMUTH_HELMET);
                        output.accept(ModItems.BISMUTH_CHESTPLATE);
                        output.accept(ModItems.BISMUTH_LEGGINGS);
                        output.accept(ModItems.BISMUTH_BOOTS);
                        output.accept(ModItems.REVENGE_MUSIC_DISC);

                        output.accept(ModItems.GECKO_SPAWN_EGG);

                        output.accept(ModItems.TOMAHAWK);

                        // Actual mod

                        output.accept(ModItems.SCULK_INGOT);
                        output.accept(ModItems.RAW_SCULK);

                        output.accept(ModItems.RED_FACTON);
                        output.accept(ModItems.ORANGE_FACTON);
                        output.accept(ModItems.YELLOW_FACTON);
                        output.accept(ModItems.GREEN_FACTON);
                        output.accept(ModItems.BLUE_FACTON);
                        output.accept(ModItems.PURPLE_FACTON);
                        output.accept(ModItems.PINK_FACTON);
                        output.accept(ModItems.LIGHT_FACTON );
                        output.accept(ModItems.DARK_FACTON);

                        output.accept(ModItems.STIMULATOR);
                        output.accept(ModItems.STIMULATOR_SPEED);
                        output.accept(ModItems.STIMULATOR_STRENGTH);
                        output.accept(ModItems.STIMULATOR_REGENERATION);
                        output.accept(ModItems.STIMULATOR_JUMP);
                        output.accept(ModItems.STIMULATOR_RESISTANCE);


                    }).build());

    public static final Supplier<CreativeModeTab> BLAFCRAFT_BLOCKS_TAB = CREATIVE_MODE_TAB .register("blafcraft_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.BISMUTH_BLOCK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, "blafcraft_items_tab")) // order
                    .title(Component.translatable("creativetab.blafcraft.blafcraft_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.BISMUTH_BLOCK);
                        output.accept(ModBlocks.BISMUTH_ORE);
                        output.accept(ModBlocks.BISMUTH_DEEPSLATE_ORE);
                        output.accept(ModBlocks.BISMUTH_NETHER_ORE);
                        output.accept(ModBlocks.BISMUTH_END_ORE);
                        output.accept(ModBlocks.MAGIC_BLOCK);

                        output.accept(ModBlocks.BISMUTH_STAIRS);
                        output.accept(ModBlocks.BISMUTH_SLAB);
                        output.accept(ModBlocks.BISMUTH_PRESSURE_PLATE);
                        output.accept(ModBlocks.BISMUTH_BUTTON);
                        output.accept(ModBlocks.BISMUTH_FENCE);
                        output.accept(ModBlocks.BISMUTH_FENCE_GATE);
                        output.accept(ModBlocks.BISMUTH_WALL);
                        output.accept(ModBlocks.BISMUTH_DOOR);
                        output.accept(ModBlocks.BISMUTH_TRAPDOOR);

                        output.accept(ModBlocks.BISMUTH_LAMP);

                        output.accept(ModBlocks.CHAIR);


                        output.accept(ModBlocks.SCULK_ORE);
                        output.accept(ModBlocks.REALIZER);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }


}
