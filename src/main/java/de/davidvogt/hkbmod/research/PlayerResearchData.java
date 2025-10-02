package de.davidvogt.hkbmod.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

public class PlayerResearchData {
    // Map: classType -> highest completed level
    private final Map<String, Integer> completedLevels = new HashMap<>();

    public static final MapCodec<PlayerResearchData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("completedLevels").forGetter(data -> data.completedLevels)
            ).apply(instance, PlayerResearchData::new)
    );

    public PlayerResearchData() {
    }

    private PlayerResearchData(Map<String, Integer> completedLevels) {
        this.completedLevels.putAll(completedLevels);
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
}
