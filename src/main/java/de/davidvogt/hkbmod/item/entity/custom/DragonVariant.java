package de.davidvogt.hkbmod.item.entity.custom;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * Dragon variants with different colors, sizes, stats, and behavior.
 * Each variant has an elemental theme and spawns more commonly in specific biomes.
 */
public enum DragonVariant implements StringRepresentable {
    // Friendly variants
    ICE(
            "ice",
            0x88CCFF,  // Light blue color tint
            80.0,      // Min health
            100.0,     // Max health
            6.0,       // Min attack damage
            8.0,       // Max attack damage
            0.40f,     // Min scale
            0.50f,     // Max scale
            3.0,       // Armor
            createBiomeWeights(
                    Biomes.SNOWY_PLAINS, 0.4,
                    Biomes.SNOWY_TAIGA, 0.4,
                    Biomes.ICE_SPIKES, 0.5,
                    Biomes.FROZEN_PEAKS, 0.5,
                    Biomes.SNOWY_SLOPES, 0.4
            )
    ),

    NATURE(
            "nature",
            0x44CC44,  // Green color tint
            90.0,      // Min health
            110.0,     // Max health
            7.0,       // Min attack damage
            9.0,       // Max attack damage
            0.45f,     // Min scale
            0.55f,     // Max scale
            3.5,       // Armor
            createBiomeWeights(
                    Biomes.FOREST, 0.4,
                    Biomes.BIRCH_FOREST, 0.35,
                    Biomes.FLOWER_FOREST, 0.35,
                    Biomes.JUNGLE, 0.4,
                    Biomes.PLAINS, 0.3,
                    Biomes.MEADOW, 0.35
            )
    ),

    // Neutral variants
    STORM(
            "storm",
            0x6644CC,  // Purple/Dark blue color tint
            100.0,     // Min health
            120.0,     // Max health
            8.0,       // Min attack damage
            10.0,      // Max attack damage
            0.50f,     // Min scale
            0.60f,     // Max scale
            4.0,       // Armor
            createBiomeWeights(
                    Biomes.WINDSWEPT_HILLS, 0.4,
                    Biomes.WINDSWEPT_GRAVELLY_HILLS, 0.4,
                    Biomes.STONY_PEAKS, 0.35,
                    Biomes.JAGGED_PEAKS, 0.4
            )
    ),

    EARTH(
            "earth",
            0x8B7355,  // Brown/Tan color tint
            110.0,     // Min health
            120.0,     // Max health
            8.0,       // Min attack damage
            10.0,      // Max attack damage
            0.50f,     // Min scale
            0.60f,     // Max scale
            5.0,       // Armor
            createBiomeWeights(
                    Biomes.PLAINS, 0.35,
                    Biomes.SAVANNA, 0.4,
                    Biomes.BADLANDS, 0.3,
                    Biomes.WOODED_BADLANDS, 0.3
            )
    ),

    // Aggressive variants
    FIRE(
            "fire",
            0xFF4400,  // Red/Orange color tint
            110.0,     // Min health
            130.0,     // Max health
            10.0,      // Min attack damage
            12.0,      // Max attack damage
            0.55f,     // Min scale
            0.65f,     // Max scale
            4.5,       // Armor
            createBiomeWeights(
                    Biomes.DESERT, 0.4,
                    Biomes.BADLANDS, 0.45,
                    Biomes.ERODED_BADLANDS, 0.4,
                    Biomes.SAVANNA, 0.3,
                    Biomes.NETHER_WASTES, 0.5
            )
    ),

    SHADOW(
            "shadow",
            0x222222,  // Black/Dark gray color tint
            120.0,     // Min health
            140.0,     // Max health
            12.0,      // Min attack damage
            15.0,      // Max attack damage
            0.60f,     // Min scale
            0.70f,     // Max scale
            6.0,       // Armor
            createBiomeWeights(
                    Biomes.DARK_FOREST, 0.5,
                    Biomes.DEEP_DARK, 0.6,
                    Biomes.SWAMP, 0.3,
                    Biomes.MANGROVE_SWAMP, 0.3
            )
    ),

    // Rare/Special variant
    ARCANE(
            "arcane",
            0xFF44FF,  // Magenta/Purple color tint
            100.0,     // Min health
            130.0,     // Max health
            9.0,       // Min attack damage
            13.0,      // Max attack damage
            0.50f,     // Min scale
            0.60f,     // Max scale
            5.0,       // Armor
            createBiomeWeights()  // Can spawn anywhere but rare (no biome preference)
    );

    private final String name;
    private final int colorTint;
    private final double minHealth;
    private final double maxHealth;
    private final double minAttackDamage;
    private final double maxAttackDamage;
    private final float minScale;
    private final float maxScale;
    private final double armor;
    private final Map<ResourceKey<Biome>, Double> biomeWeights;

    DragonVariant(String name, int colorTint, double minHealth, double maxHealth,
                  double minAttackDamage, double maxAttackDamage,
                  float minScale, float maxScale, double armor,
                  Map<ResourceKey<Biome>, Double> biomeWeights) {
        this.name = name;
        this.colorTint = colorTint;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
        this.minAttackDamage = minAttackDamage;
        this.maxAttackDamage = maxAttackDamage;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.armor = armor;
        this.biomeWeights = biomeWeights;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String getName() {
        return name;
    }

    public int getColorTint() {
        return colorTint;
    }

    public float getRed() {
        return ((colorTint >> 16) & 0xFF) / 255.0f;
    }

    public float getGreen() {
        return ((colorTint >> 8) & 0xFF) / 255.0f;
    }

    public float getBlue() {
        return (colorTint & 0xFF) / 255.0f;
    }

    public double getMinHealth() {
        return minHealth;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getMinAttackDamage() {
        return minAttackDamage;
    }

    public double getMaxAttackDamage() {
        return maxAttackDamage;
    }

    public float getMinScale() {
        return minScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public double getArmor() {
        return armor;
    }

    /**
     * Gets the spawn weight for this variant in the given biome.
     * Higher weight = more likely to spawn.
     * Default weight is 0.1 for biomes not in the preference map.
     */
    public double getBiomeWeight(ResourceKey<Biome> biome) {
        return biomeWeights.getOrDefault(biome, 0.1);
    }

    /**
     * Generates random health within the variant's range
     */
    public double randomHealth(net.minecraft.util.RandomSource random) {
        return minHealth + random.nextDouble() * (maxHealth - minHealth);
    }

    /**
     * Generates random attack damage within the variant's range
     */
    public double randomAttackDamage(net.minecraft.util.RandomSource random) {
        return minAttackDamage + random.nextDouble() * (maxAttackDamage - minAttackDamage);
    }

    /**
     * Generates random scale within the variant's range
     */
    public float randomScale(net.minecraft.util.RandomSource random) {
        return minScale + random.nextFloat() * (maxScale - minScale);
    }

    /**
     * Selects a random variant based on biome weights
     */
    public static DragonVariant selectForBiome(ResourceKey<Biome> biome, net.minecraft.util.RandomSource random) {
        // Calculate total weight for this biome
        double totalWeight = 0.0;
        for (DragonVariant variant : values()) {
            totalWeight += variant.getBiomeWeight(biome);
        }

        // Random selection based on weights
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;

        for (DragonVariant variant : values()) {
            currentWeight += variant.getBiomeWeight(biome);
            if (randomValue <= currentWeight) {
                return variant;
            }
        }

        // Fallback (should never happen)
        return STORM;
    }

    /**
     * Helper method to create biome weight maps
     */
    @SafeVarargs
    private static <T> Map<ResourceKey<Biome>, Double> createBiomeWeights(Object... pairs) {
        Map<ResourceKey<Biome>, Double> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length && pairs[i] instanceof ResourceKey && pairs[i + 1] instanceof Number) {
                @SuppressWarnings("unchecked")
                ResourceKey<Biome> biome = (ResourceKey<Biome>) pairs[i];
                Double weight = ((Number) pairs[i + 1]).doubleValue();
                map.put(biome, weight);
            }
        }
        return map;
    }

    /**
     * Gets variant by ordinal (for syncing)
     */
    public static DragonVariant byOrdinal(int ordinal) {
        DragonVariant[] variants = values();
        if (ordinal >= 0 && ordinal < variants.length) {
            return variants[ordinal];
        }
        return STORM; // Default fallback
    }
}
