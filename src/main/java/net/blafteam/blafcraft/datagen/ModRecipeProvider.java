package net.blafteam.blafcraft.datagen;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.block.ModBlocks;
import net.blafteam.blafcraft.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        List<ItemLike> BISMUTH_SMELTABLES = List.of(ModItems.RAW_BISMUTH, // smelting
                ModBlocks.BISMUTH_ORE, ModBlocks.BISMUTH_DEEPSLATE_ORE);

        List<ItemLike> SCULK_SMELTABLES = List.of(ModItems.RAW_SCULK,
                ModBlocks.SCULK_ORE);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BISMUTH_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.BISMUTH.get())
                .unlockedBy("has_bismuth", has(ModItems.BISMUTH)).save(recipeOutput); // recipe book

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BISMUTH.get(), 9)
                .requires(ModBlocks.BISMUTH_BLOCK)
                .unlockedBy("has_bismuth_block", has(ModBlocks.BISMUTH_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BISMUTH.get(), 18)
                .requires(ModBlocks.MAGIC_BLOCK)
                .unlockedBy("has_magic_block", has(ModBlocks.MAGIC_BLOCK))
                .save(recipeOutput, "blafcraft:bismuth_from_magic_block"); // custom name, need to do when same output

        oreSmelting(recipeOutput, BISMUTH_SMELTABLES, RecipeCategory.MISC, ModItems.BISMUTH.get(), 0.25f, 200, "bismuth");
        oreBlasting(recipeOutput, BISMUTH_SMELTABLES, RecipeCategory.MISC, ModItems.BISMUTH.get(), 0.25f, 100, "bismuth");

        stairBuilder(ModBlocks.BISMUTH_STAIRS.get(), Ingredient.of(ModItems.BISMUTH)).group("bismuth")
                .unlockedBy("has_bismuth", has(ModItems.BISMUTH)).save(recipeOutput);
        slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.BISMUTH_SLAB.get(), ModItems.BISMUTH.get());

        buttonBuilder(ModBlocks.BISMUTH_BUTTON.get(), Ingredient.of(ModItems.BISMUTH.get())).group("bismuth")
                .unlockedBy("has_bismuth", has(ModItems.BISMUTH.get())).save(recipeOutput);
        pressurePlate(recipeOutput, ModBlocks.BISMUTH_PRESSURE_PLATE.get(), ModItems.BISMUTH.get());

        fenceBuilder(ModBlocks.BISMUTH_FENCE.get(), Ingredient.of(ModItems.BISMUTH.get())).group("bismuth")
                .unlockedBy("has_bismuth", has(ModItems.BISMUTH.get())).save(recipeOutput);
        fenceGateBuilder(ModBlocks.BISMUTH_FENCE_GATE.get(), Ingredient.of(ModItems.BISMUTH.get())).group("bismuth")
                .unlockedBy("has_bismuth", has(ModItems.BISMUTH.get())).save(recipeOutput);
        wall(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.BISMUTH_WALL.get(), ModItems.BISMUTH.get());

        doorBuilder(ModBlocks.BISMUTH_DOOR.get(), Ingredient.of(ModItems.BISMUTH.get())).group("bismuth")
                .unlockedBy("has_bismuth", has(ModItems.BISMUTH.get())).save(recipeOutput);
        trapdoorBuilder(ModBlocks.BISMUTH_TRAPDOOR.get(), Ingredient.of(ModItems.BISMUTH.get())).group("bismuth")
                .unlockedBy("has_bismuth", has(ModItems.BISMUTH.get())).save(recipeOutput);

        swordRecipe(recipeOutput, ModItems.BISMUTH_SWORD.get(), ModItems.BISMUTH.get(), Items.STICK);
        axeRecipe(recipeOutput, ModItems.BISMUTH_AXE.get(), ModItems.BISMUTH.get(), Items.STICK);
        pickaxeRecipe(recipeOutput, ModItems.BISMUTH_PICKAXE.get(), ModItems.BISMUTH.get(), Items.STICK);
        shovelRecipe(recipeOutput, ModItems.BISMUTH_SHOVEL .get(), ModItems.BISMUTH.get(), Items.STICK);
        hoeRecipe(recipeOutput, ModItems.BISMUTH_HOE.get(), ModItems.BISMUTH.get(), Items.STICK);

        helmetRecipe(recipeOutput, ModItems.BISMUTH_HELMET.get(), ModItems.BISMUTH.get());
        chestplateRecipe(recipeOutput, ModItems.BISMUTH_CHESTPLATE.get(), ModItems.BISMUTH.get());
        leggingsRecipe(recipeOutput, ModItems.BISMUTH_LEGGINGS.get(), ModItems.BISMUTH.get());
        bootsRecipe(recipeOutput, ModItems.BISMUTH_BOOTS.get(), ModItems.BISMUTH.get());

        // Actual mod

        swordRecipe(recipeOutput, ModItems.SCULK_SWORD.get(), ModItems.SCULK_INGOT.get(), Items.STICK);
        axeRecipe(recipeOutput, ModItems.SCULK_AXE.get(), ModItems.SCULK_INGOT.get(), Items.STICK);


        oreSmelting(recipeOutput, SCULK_SMELTABLES, RecipeCategory.MISC, ModItems.SCULK_INGOT.get(), 0.25f, 200, "sculk");
        oreBlasting(recipeOutput, SCULK_SMELTABLES, RecipeCategory.MISC, ModItems.SCULK_INGOT.get(), 0.25f, 100, "sculk");
    }

    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTime, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    private void swordRecipe(RecipeOutput output, ItemLike result, ItemLike swordMaterial, ItemLike stickMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern(" B ")
                .pattern(" B ")
                .pattern(" S ")
                .define('B', swordMaterial)
                .define('S', stickMaterial)
                .unlockedBy("has_" + swordMaterial.asItem().getDescriptionId(), has(swordMaterial))
                .save(output);
    }

    private void axeRecipe(RecipeOutput output, ItemLike result, ItemLike axeMaterial, ItemLike stickMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern(" BB")
                .pattern(" SB")
                .pattern(" S ")
                .define('B', axeMaterial)
                .define('S', stickMaterial)
                .unlockedBy("has_" + axeMaterial.asItem().getDescriptionId(), has(axeMaterial))
                .save(output);
//        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result) // NOT NECESSARY BUT GOOD TO KNOW
//                .pattern("BB ")
//                .pattern("BS ")
//                .pattern(" S ")
//                .define('B', axeMaterial)
//                .define('S', stickMaterial)
//                .unlockedBy("has_" + axeMaterial.asItem().getDescriptionId(), has(axeMaterial))
//                .save(output, ResourceLocation.fromNamespaceAndPath("mymodid",
//                        result.asItem().getDescriptionId().replace("item.mymodid.", "") + "_left"));
    }

    private void pickaxeRecipe(RecipeOutput output, ItemLike result, ItemLike pickaxeMaterial, ItemLike stickMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("BBB")
                .pattern(" S ")
                .pattern(" S ")
                .define('B', pickaxeMaterial)
                .define('S', stickMaterial)
                .unlockedBy("has_" + pickaxeMaterial.asItem().getDescriptionId(), has(pickaxeMaterial))
                .save(output);
    }

    private void shovelRecipe(RecipeOutput output, ItemLike result, ItemLike shovelMaterial, ItemLike stickMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern(" B ")
                .pattern(" S ")
                .pattern(" S ")
                .define('B', shovelMaterial)
                .define('S', stickMaterial)
                .unlockedBy("has_" + shovelMaterial.asItem().getDescriptionId(), has(shovelMaterial))
                .save(output);
    }

    private void hoeRecipe(RecipeOutput output, ItemLike result, ItemLike hoeMaterial, ItemLike stickMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern(" BB")
                .pattern(" S ")
                .pattern(" S ")
                .define('B', hoeMaterial)
                .define('S', stickMaterial)
                .unlockedBy("has_" + hoeMaterial.asItem().getDescriptionId(), has(hoeMaterial))
                .save(output);
    }

    private void helmetRecipe(RecipeOutput output, ItemLike result, ItemLike helmetMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("BBB")
                .pattern("B B")
                .pattern("   ")
                .define('B', helmetMaterial)
                .unlockedBy("has_" + helmetMaterial.asItem().getDescriptionId(), has(helmetMaterial))
                .save(output);
    }
    private void chestplateRecipe(RecipeOutput output, ItemLike result, ItemLike chestplateMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("B B")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', chestplateMaterial)
                .unlockedBy("has_" + chestplateMaterial.asItem().getDescriptionId(), has(chestplateMaterial))
                .save(output);
    }
    private void leggingsRecipe(RecipeOutput output, ItemLike result, ItemLike leggingsMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("BBB")
                .pattern("B B")
                .pattern("B B")
                .define('B', leggingsMaterial)
                .unlockedBy("has_" + leggingsMaterial.asItem().getDescriptionId(), has(leggingsMaterial))
                .save(output);
    }
    private void bootsRecipe(RecipeOutput output, ItemLike result, ItemLike bootsMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("   ")
                .pattern("B B")
                .pattern("B B")
                .define('B', bootsMaterial)
                .unlockedBy("has_" + bootsMaterial.asItem().getDescriptionId(), has(bootsMaterial))
                .save(output);
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(recipeOutput, BlafCraft.MODID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
