package de.davidvogt.hkbmod.worldgen;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.worldgen.feature.DragonNestFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, HKBMod.MODID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> DRAGON_NEST =
            FEATURES.register("dragon_nest", () -> new DragonNestFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
