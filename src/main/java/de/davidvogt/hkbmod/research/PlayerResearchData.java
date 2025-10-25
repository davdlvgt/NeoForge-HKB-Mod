package de.davidvogt.hkbmod.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class PlayerResearchData {
    public static final MapCodec<PlayerResearchData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("completedLevels").forGetter(data -> data.completedLevels),
                    Codec.list(ResourceLocation.CODEC).xmap(HashSet::new, list -> new ArrayList<>(list)).optionalFieldOf("unlockedRecipes", new HashSet<>()).forGetter(data -> new HashSet<>(data.unlockedRecipes))
            ).apply(instance, PlayerResearchData::new)
    );
    // Map: classType -> highest completed level
    private final Map<String, Integer> completedLevels = new HashMap<>();
    // Set of unlocked recipe IDs
    private final Set<ResourceLocation> unlockedRecipes = new HashSet<>();

    public PlayerResearchData() {
    }

    private PlayerResearchData(Map<String, Integer> completedLevels, Set<ResourceLocation> unlockedRecipes) {
        this.completedLevels.putAll(completedLevels);
        this.unlockedRecipes.addAll(unlockedRecipes);
    }

    public boolean isLevelCompleted(String classType, int level) {
        return completedLevels.getOrDefault(classType, -1) >= level;
    }

    public int getHighestCompletedLevel(String classType) {
        return completedLevels.getOrDefault(classType, -1);
    }

    public void completeLevel(String classType, int level) {
        int currentHighest = completedLevels.getOrDefault(classType, -1);
        if (level > currentHighest) {
            completedLevels.put(classType, level);
        }
    }

    public boolean canResearch(String classType, int level, Research research) {
        // Cannot research if already completed
        if (isLevelCompleted(classType, level)) {
            return false;
        }

        // Check if all prerequisites are completed
        if (research != null && research.prerequisites() != null) {
            for (Research.ResearchPrerequisite prereq : research.prerequisites()) {
                if (!isLevelCompleted(prereq.classType(), prereq.level())) {
                    return false;
                }
            }
        }

        return true;
    }

    // Deprecated: Use canResearch(String, int, Research) instead
    @Deprecated
    public boolean canResearch(String classType, int level) {
        // Level 0 is always available
        if (level == 0) {
            return !isLevelCompleted(classType, level);
        }
        // For other levels, previous level must be completed
        return !isLevelCompleted(classType, level) && isLevelCompleted(classType, level - 1);
    }

    public Map<String, Integer> getCompletedLevels() {
        return new HashMap<>(completedLevels);
    }

    // ========== Recipe Unlocking Methods ==========

    /**
     * Unlocks a recipe for the player
     *
     * @param recipeId The ResourceLocation of the recipe to unlock
     * @return true if the recipe was newly unlocked, false if already unlocked
     */
    public boolean unlockRecipe(ResourceLocation recipeId) {
        return unlockedRecipes.add(recipeId);
    }

    /**
     * Locks a recipe for the player (removes it from unlocked recipes)
     *
     * @param recipeId The ResourceLocation of the recipe to lock
     * @return true if the recipe was unlocked and is now locked, false if it wasn't unlocked
     */
    public boolean lockRecipe(ResourceLocation recipeId) {
        return unlockedRecipes.remove(recipeId);
    }

    /**
     * Checks if a recipe is unlocked for the player
     *
     * @param recipeId The ResourceLocation of the recipe to check
     * @return true if the recipe is unlocked, false otherwise
     */
    public boolean isRecipeUnlocked(ResourceLocation recipeId) {
        return unlockedRecipes.contains(recipeId);
    }

    /**
     * Gets all unlocked recipes
     *
     * @return An unmodifiable set of unlocked recipe IDs
     */
    public Set<ResourceLocation> getUnlockedRecipes() {
        return Collections.unmodifiableSet(unlockedRecipes);
    }

    /**
     * Unlocks multiple recipes at once
     *
     * @param recipeIds Collection of recipe IDs to unlock
     */
    public void unlockRecipes(Collection<ResourceLocation> recipeIds) {
        unlockedRecipes.addAll(recipeIds);
    }

    /**
     * Clears all unlocked recipes
     */
    public void clearUnlockedRecipes() {
        unlockedRecipes.clear();
    }

    /**
     * Gets the count of unlocked recipes
     *
     * @return Number of unlocked recipes
     */
    public int getUnlockedRecipeCount() {
        return unlockedRecipes.size();
    }
}
