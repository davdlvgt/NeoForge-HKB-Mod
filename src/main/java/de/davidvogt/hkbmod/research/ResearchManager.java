package de.davidvogt.hkbmod.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ResearchManager {
    private static final Map<String, List<Research>> RESEARCHES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadResearches(ResourceManager resourceManager) {
        RESEARCHES.clear();

        // Define research files to load
        String[] researchFiles = {
            "knight_level_0", "knight_level_1", "knight_level_2", "knight_level_3", "knight_level_4", "knight_level_5",
            "archer_level_0", "archer_level_1", "archer_level_2", "archer_level_3", "archer_level_4", "archer_level_5",
            "cavalier_level_0", "cavalier_level_1", "cavalier_level_2", "cavalier_level_3", "cavalier_level_4", "cavalier_level_5",
            "magician_level_0", "magician_level_1", "magician_level_2", "magician_level_3", "magician_level_4", "magician_level_5"
        };

        // Load each research file from resources
        for (String fileName : researchFiles) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "research/" + fileName + ".json");

            try {
                var resource = resourceManager.getResource(location);
                if (resource.isPresent()) {
                    try (InputStream inputStream = resource.get().open();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                        StringBuilder jsonBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            jsonBuilder.append(line);
                        }

                        loadFromJsonString(fileName, jsonBuilder.toString());
                    }
                } else {
                    HKBMod.LOGGER.warn("Research file not found: {}", location);
                }
            } catch (Exception e) {
                HKBMod.LOGGER.error("Failed to load research file: {}", fileName, e);
            }
        }

        // Sort by level
        RESEARCHES.values().forEach(list -> list.sort(Comparator.comparingInt(Research::level)));

        HKBMod.LOGGER.info("Loaded {} research classes with {} total levels",
                RESEARCHES.size(),
                RESEARCHES.values().stream().mapToInt(List::size).sum());
    }

    private static void loadFromJsonString(String id, String jsonString) {
        try {
            JsonElement json = JsonParser.parseString(jsonString);
            Research research = Research.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> HKBMod.LOGGER.error("Failed to parse research {}: {}", id, error))
                    .orElse(null);

            if (research != null) {
                RESEARCHES.computeIfAbsent(research.classType(), k -> new ArrayList<>()).add(research);
                HKBMod.LOGGER.info("Loaded research: {} - Level {}", research.classType(), research.level());
            }
        } catch (Exception e) {
            HKBMod.LOGGER.error("Error loading research {}", id, e);
        }
    }

    public static List<Research> getResearchForClass(String classType) {
        return RESEARCHES.getOrDefault(classType, Collections.emptyList());
    }

    public static List<String> getAllClasses() {
        return new ArrayList<>(RESEARCHES.keySet());
    }

    public static Research getResearch(String classType, int level) {
        return getResearchForClass(classType).stream()
                .filter(r -> r.level() == level)
                .findFirst()
                .orElse(null);
    }

    public static boolean isLoaded() {
        return !RESEARCHES.isEmpty();
    }

    public static void addResearch(Research research) {
        RESEARCHES.computeIfAbsent(research.classType(), k -> new ArrayList<>()).add(research);
    }

    public static void clearResearches() {
        RESEARCHES.clear();
    }
}
