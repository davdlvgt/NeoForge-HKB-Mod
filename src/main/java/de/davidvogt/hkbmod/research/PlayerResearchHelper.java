package de.davidvogt.hkbmod.research;

import de.davidvogt.hkbmod.attachment.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * Helper class for accessing and manipulating player research data.
 * Provides convenient static methods to interact with the PlayerResearchData attachment.
 */
public class PlayerResearchHelper {

    // ========== Recipe Unlocking Methods ==========

    /**
     * Checks if a player has unlocked a specific recipe
     *
     * @param player   The player to check
     * @param recipeId The recipe ID to check
     * @return true if the player has unlocked the recipe, false otherwise
     */
    public static boolean hasRecipeUnlocked(Player player, ResourceLocation recipeId) {
        if (player == null || recipeId == null) {
            return false;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        return data.isRecipeUnlocked(recipeId);
    }

    /**
     * Unlocks a recipe for a player
     *
     * @param player   The player to unlock the recipe for
     * @param recipeId The recipe ID to unlock
     * @return true if the recipe was newly unlocked, false if already unlocked
     */
    public static boolean unlockRecipe(Player player, ResourceLocation recipeId) {
        if (player == null || recipeId == null) {
            return false;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        boolean wasUnlocked = data.unlockRecipe(recipeId);
        player.setData(ModAttachments.PLAYER_RESEARCH, data);
        return wasUnlocked;
    }

    /**
     * Locks a recipe for a player (removes from unlocked recipes)
     *
     * @param player   The player to lock the recipe for
     * @param recipeId The recipe ID to lock
     * @return true if the recipe was unlocked and is now locked, false if it wasn't unlocked
     */
    public static boolean lockRecipe(Player player, ResourceLocation recipeId) {
        if (player == null || recipeId == null) {
            return false;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        boolean wasLocked = data.lockRecipe(recipeId);
        player.setData(ModAttachments.PLAYER_RESEARCH, data);
        return wasLocked;
    }

    /**
     * Gets all unlocked recipes for a player
     *
     * @param player The player to get recipes for
     * @return An unmodifiable set of unlocked recipe IDs
     */
    public static Set<ResourceLocation> getUnlockedRecipes(Player player) {
        if (player == null) {
            return Set.of();
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        return data.getUnlockedRecipes();
    }

    /**
     * Clears all unlocked recipes for a player
     *
     * @param player The player to clear recipes for
     */
    public static void clearUnlockedRecipes(Player player) {
        if (player == null) {
            return;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        data.clearUnlockedRecipes();
        player.setData(ModAttachments.PLAYER_RESEARCH, data);
    }

    /**
     * Gets the count of unlocked recipes for a player
     *
     * @param player The player to count recipes for
     * @return Number of unlocked recipes
     */
    public static int getUnlockedRecipeCount(Player player) {
        if (player == null) {
            return 0;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        return data.getUnlockedRecipeCount();
    }

    // ========== Research Level Methods (existing functionality) ==========

    /**
     * Checks if a player has completed a specific research level
     *
     * @param player    The player to check
     * @param classType The class type (e.g., "archer", "knight")
     * @param level     The level to check
     * @return true if the level is completed, false otherwise
     */
    public static boolean isLevelCompleted(Player player, String classType, int level) {
        if (player == null || classType == null) {
            return false;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        return data.isLevelCompleted(classType, level);
    }

    /**
     * Gets the highest completed level for a player in a specific class
     *
     * @param player    The player to check
     * @param classType The class type (e.g., "archer", "knight")
     * @return The highest completed level, or -1 if none completed
     */
    public static int getHighestCompletedLevel(Player player, String classType) {
        if (player == null || classType == null) {
            return -1;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        return data.getHighestCompletedLevel(classType);
    }

    /**
     * Completes a research level for a player
     *
     * @param player    The player to complete the level for
     * @param classType The class type (e.g., "archer", "knight")
     * @param level     The level to complete
     */
    public static void completeLevel(Player player, String classType, int level) {
        if (player == null || classType == null) {
            return;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        data.completeLevel(classType, level);
        player.setData(ModAttachments.PLAYER_RESEARCH, data);
    }

    /**
     * Checks if a player can research a specific level
     *
     * @param player    The player to check
     * @param classType The class type (e.g., "archer", "knight")
     * @param level     The level to check
     * @param research  The research object with prerequisites
     * @return true if the player can research this level, false otherwise
     */
    public static boolean canResearch(Player player, String classType, int level, Research research) {
        if (player == null || classType == null) {
            return false;
        }
        PlayerResearchData data = player.getData(ModAttachments.PLAYER_RESEARCH);
        return data.canResearch(classType, level, research);
    }
}
