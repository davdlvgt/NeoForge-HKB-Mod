package de.davidvogt.hkbmod.screen.cutsom.recipebook;

import com.google.common.collect.Lists;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.research.PlayerResearchHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizes and filters ResearchCraftingRecipes for display in the recipe book.
 * Only shows recipes that the player has unlocked through research.
 */
public class ResearchRecipeCollection {
    private final List<RecipeHolder<ResearchCraftingRecipe>> recipes = Lists.newArrayList();
    private final ResearchRecipeCategory category;

    public ResearchRecipeCollection(ResearchRecipeCategory category) {
        this.category = category;
    }

    /**
     * Gets the category of this collection
     */
    public ResearchRecipeCategory getCategory() {
        return category;
    }

    /**
     * Adds a recipe to this collection if the player has unlocked it
     */
    public void add(RecipeHolder<ResearchCraftingRecipe> recipe, Player player) {
        // Check if player has unlocked this recipe
        if (PlayerResearchHelper.hasRecipeUnlocked(player, recipe.id().location())) {
            this.recipes.add(recipe);
        }
    }

    /**
     * Gets all recipes in this collection that the player can see
     */
    public List<RecipeHolder<ResearchCraftingRecipe>> getRecipes() {
        return this.recipes;
    }

    /**
     * Checks if this collection has any recipes
     */
    public boolean hasRecipes() {
        return !this.recipes.isEmpty();
    }

    /**
     * Clears all recipes from this collection
     */
    public void clear() {
        this.recipes.clear();
    }

    /**
     * Gets the number of recipes in this collection
     */
    public int size() {
        return this.recipes.size();
    }

    /**
     * Updates the collection by rebuilding the recipe list based on current unlock status
     */
    public void updateUnlockedRecipes(List<RecipeHolder<ResearchCraftingRecipe>> allRecipes, Player player) {
        this.recipes.clear();
        for (RecipeHolder<ResearchCraftingRecipe> recipe : allRecipes) {
            if (recipe.value().category() == this.category.getCraftingCategory()) {
                add(recipe, player);
            }
        }
    }
}
