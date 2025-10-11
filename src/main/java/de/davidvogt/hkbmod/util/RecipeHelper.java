package de.davidvogt.hkbmod.util;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Helper class for recipe-related operations
 */
public class RecipeHelper {

    /**
     * Checks if a recipe is allowed in the research crafting table.
     * Now checks if the recipe is a ResearchCraftingRecipe type.
     */
    public static boolean isResearchCraftingTableRecipe(RecipeHolder<?> recipeHolder) {
        if (recipeHolder == null) {
            return false;
        }

        // Check if the recipe is a ResearchCraftingRecipe
        return recipeHolder.value() instanceof ResearchCraftingRecipe;
    }

    /**
     * Gets a list of all allowed recipe IDs for the research crafting table.
     *
     * @return List of recipe paths that are whitelisted
     */
    public static List<String> getAllowedRecipeIds() {
        return List.of(
                // Archer recipes
                "elastic_wood",
                "long_stick",
                "long_string",
                "longbow_stick",
                "longbow",
                "longbow_arrow",
                // Knight recipes
                "emerald_sword",
                // Magician recipes
                "time_setter",
                // Miner recipes
                "emerald_pickaxe",
                "magic_pickaxe"
        );
    }

    /**
     * Gets a list of all allowed recipe ResourceLocations for the research crafting table.
     *
     * @return List of ResourceLocations with the mod namespace
     */
    public static List<ResourceLocation> getAllowedRecipeLocations() {
        return getAllowedRecipeIds().stream()
                .map(path -> ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, path))
                .toList();
    }

    /**
     * Whitelist specific recipes that can be crafted in the research crafting table.
     * Add your recipe names here.
     */
    private static boolean isWhitelistedRecipe(String recipePath) {
        return getAllowedRecipeIds().contains(recipePath);
    }


}
