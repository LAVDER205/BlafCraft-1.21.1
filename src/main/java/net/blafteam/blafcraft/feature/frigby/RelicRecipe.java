package net.blafteam.blafcraft.feature.frigby;

import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.item.ModItems;
import net.blafteam.blafcraft.social.SocialData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RelicRecipe extends CustomRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BlafCraft.MODID);

    private static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RelicRecipe>> SERIALIZER = SERIALIZERS.register("relic", () ->
            new SimpleCraftingRecipeSerializer<>(RelicRecipe::new));

    public RelicRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        // У автоматического крафтера нет игрока. Верстак передаёт игрока при расчёте результата.
        return CommonHooks.getCraftingPlayer() instanceof ServerPlayer player && canCraft(player) && matchesPattern(input);
    }

    static boolean canCraft(ServerPlayer player) {
        return SocialData.get(player.serverLevel().getServer()).canCraftFrigbyRelic(player.getUUID());
    }

    static boolean matchesPattern(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 6) return false;
        if (!input.getItem(4).is(Items.AMETHYST_SHARD)
                || !input.getItem(1).is(Items.COPPER_INGOT) || !input.getItem(3).is(Items.COPPER_INGOT)
                || !input.getItem(5).is(Items.COPPER_INGOT) || !input.getItem(7).is(Items.COPPER_INGOT)) return false;
        // Пять обязательных клеток уже заняты. Последняя — золотой слиток в верхнем углу.
        return input.getItem(0).is(Items.GOLD_INGOT) || input.getItem(2).is(Items.GOLD_INGOT);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(ModItems.RELIC.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }
}
