package de.davidvogt.hkbmod.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/**
 * Custom crafting recipe that can only be crafted in the Research Crafting Table.
 * This prevents the recipe from being used in vanilla crafting tables.
 */
public class ResearchCraftingRecipe implements CraftingRecipe {

    private final ShapedRecipe internalRecipe;
    private final CraftingBookCategory category;
    private final ItemStack resultItem;

    /**
     * Creates a research crafting recipe from a shaped recipe
     */
    public ResearchCraftingRecipe(ShapedRecipe recipe) {
        this.internalRecipe = recipe;
        this.category = recipe.category();
        // Create empty 3x3 grid for getting result item
        java.util.List<ItemStack> emptyGrid = java.util.List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        );
        this.resultItem = recipe.assemble(CraftingInput.of(3, 3, emptyGrid), null);
    }

    /**
     * Constructor for deserialization
     */
    public ResearchCraftingRecipe(CraftingBookCategory category, ShapedRecipe recipe) {
        this.internalRecipe = recipe;
        this.category = category;
        // Create empty 3x3 grid for getting result item
        java.util.List<ItemStack> emptyGrid = java.util.List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        );
        this.resultItem = recipe.assemble(CraftingInput.of(3, 3, emptyGrid), null);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return internalRecipe.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return internalRecipe.assemble(input, registries);
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModRecipeTypes.RESEARCH_CRAFTING_SERIALIZER.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeType<CraftingRecipe> getType() {
        return (RecipeType<CraftingRecipe>) (RecipeType<?>) ModRecipeTypes.RESEARCH_CRAFTING_TYPE.get();
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public PlacementInfo placementInfo() {
        return internalRecipe.placementInfo();
    }

    /**
     * Gets the internal shaped recipe
     */
    public ShapedRecipe getInternalRecipe() {
        return internalRecipe;
    }

    /**
     * Gets the width of the recipe pattern
     */
    public int getWidth() {
        return internalRecipe.getWidth();
    }

    /**
     * Gets the height of the recipe pattern
     */
    public int getHeight() {
        return internalRecipe.getHeight();
    }

    /**
     * Gets the result item stack (for displaying in recipe book)
     */
    public ItemStack getResultItem() {
        return resultItem.copy();
    }
}
