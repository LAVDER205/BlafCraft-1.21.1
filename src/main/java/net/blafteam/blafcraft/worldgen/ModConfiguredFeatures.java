package net.blafteam.blafcraft.worldgen;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures { // what structure
    // CF -> PF -> BM
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_BISMUTH_ORE_KEY = registerKey("bismuth_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_BISMUTH_ORE_KEY = registerKey("nether_bismuth_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_BISMUTH_ORE_KEY = registerKey("end_bismuth_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_SCULK_ORE_KEY = registerKey("sculk_ore");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrackReplaceables = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endReplaceables = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> overworldBismuthOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.BISMUTH_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.BISMUTH_DEEPSLATE_ORE.get().defaultBlockState()));

        register(context, OVERWORLD_BISMUTH_ORE_KEY, Feature.ORE, new OreConfiguration(overworldBismuthOres, 9)); // size of the vein
        register(context, NETHER_BISMUTH_ORE_KEY, Feature.ORE, new OreConfiguration(netherrackReplaceables,
                ModBlocks.BISMUTH_NETHER_ORE.get().defaultBlockState(), 9));
        register(context, END_BISMUTH_ORE_KEY, Feature.ORE, new OreConfiguration(endReplaceables,
                ModBlocks.BISMUTH_END_ORE.get().defaultBlockState(), 9));

        // Actual mod

        List<OreConfiguration.TargetBlockState> overworldSculkOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.SCULK_ORE.get().defaultBlockState()));
                // OreConfiguration.target(deepslateReplaceables, ModBlocks.SCULK_DEEPSLATE_ORE.get().defaultBlockState()));

        register(context, OVERWORLD_SCULK_ORE_KEY, Feature.ORE, new OreConfiguration(overworldSculkOres, 4));

    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(BlafCraft.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
