package de.davidvogt.hkbmod.worldgen;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> ROBINIA_PLACED_KEY = registerKey("robinia_placed");
    public static final ResourceKey<PlacedFeature> DRAGON_NEST_PLACED_KEY = registerKey("dragon_nest_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // Java
        List<PlacementModifier> modifiers = new java.util.ArrayList<>();
        modifiers.add(RarityFilter.onAverageOnceEvery(50));
        modifiers.add(BiomeFilter.biome());
        modifiers.addAll(VegetationPlacements.treePlacement(
                PlacementUtils.countExtra(1, 0.01f, 1),
                ModBlocks.ROBINIA_SAPLING.get()
        ));

        register(context, ROBINIA_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.ROBINIA_KEY), modifiers);

        // Dragon Nest - Very rare on mountain peaks
        List<PlacementModifier> dragonNestModifiers = new java.util.ArrayList<>();
        dragonNestModifiers.add(RarityFilter.onAverageOnceEvery(2000)); // Very rare: 1 per 2000 chunks
        dragonNestModifiers.add(BiomeFilter.biome());

        register(context, DRAGON_NEST_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.DRAGON_NEST_KEY), dragonNestModifiers);

    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}
