package de.davidvogt.hkbmod.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Custom crafting recipe that can only be crafted in the Research Crafting Table.
 * This prevents the recipe from being used in vanilla crafting tables.
 */
public class ResearchCraftingRecipe implements CraftingRecipe {

    private final ShapedRecipe internalRecipe;
    private final CraftingBookCategory category;

    /**
     * Creates a research crafting recipe from a shaped recipe
     */
    public ResearchCraftingRecipe(ShapedRecipe recipe) {
        this.internalRecipe = recipe;
        this.category = recipe.category();
    }

    /**
     * Constructor for deserialization
     */
    public ResearchCraftingRecipe(CraftingBookCategory category, ShapedRecipe recipe) {
        this.internalRecipe = recipe;
        this.category = category;
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
    public RecipeType<CraftingRecipe> getType() {
        return RecipeType.CRAFTING;
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
}
