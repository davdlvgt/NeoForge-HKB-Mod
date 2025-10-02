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

        // Automatically discover all research JSON files in data/hkbmod/research/
        // The structure is: data/hkbmod/research/<classname>/<filename>.json
        // or flat: data/hkbmod/research/<filename>.json

        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> allResources =
            resourceManager.listResources("research", location -> location.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> entry : allResources.entrySet()) {
            ResourceLocation location = entry.getKey();

            // Only process files from our mod
            if (!location.getNamespace().equals(HKBMod.MODID)) {
                continue;
            }

            try {
                try (InputStream inputStream = entry.getValue().open();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }

                    // Extract filename for logging (without .json extension)
                    String path = location.getPath();
                    String fileName = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);

                    loadFromJsonString(fileName, jsonBuilder.toString());
                }
            } catch (Exception e) {
                HKBMod.LOGGER.error("Failed to load research file: {}", location, e);
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
