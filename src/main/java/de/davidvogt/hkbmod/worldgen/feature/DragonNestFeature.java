package de.davidvogt.hkbmod.worldgen.feature;

import com.mojang.serialization.Codec;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.item.entity.ModEntities;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Custom feature that places dragon nests on mountain peaks.
 * Loads the structure from data/hkbmod/structure/dragon_nest.nbt
 * Spawns 1-3 dragons near the nest after placement.
 */
public class DragonNestFeature extends Feature<NoneFeatureConfiguration> {
    private static final int DRAGON_COUNT = 1;

    private static final ResourceLocation DRAGON_NEST_STRUCTURE =
        ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "dragon_nest");

    public DragonNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        HKBMod.LOGGER.info("=== DRAGON NEST: Attempting to place at: {} ===", origin);

        // Find the actual surface/peak by scanning from TOP to BOTTOM
        BlockPos surfacePos = findSurfaceFromTop(level, origin);

        if (surfacePos == null) {
            HKBMod.LOGGER.info("Dragon nest rejected: no suitable surface found");
            return false;
        }

        HKBMod.LOGGER.info("Dragon nest: Found surface at height {}", surfacePos.getY());

        // Check if we're at a suitable height (mountain peaks: 100-256, lowered for testing)
        if (surfacePos.getY() < 100 || surfacePos.getY() > 256) {
            HKBMod.LOGGER.info("Dragon nest rejected: height {} out of range (100-256)", surfacePos.getY());
            return false;
        }

        // Load and place the structure from NBT file
        boolean success = placeStructure(level, surfacePos);

        if (success) {
            // Spawn dragons after placing the structure
            spawnDragons(level, surfacePos, context.random());
            HKBMod.LOGGER.info("=== DRAGON NEST: Successfully placed at: {} ===", surfacePos);
        } else {
            HKBMod.LOGGER.warn("=== DRAGON NEST: Failed to load structure template ===");
        }

        return success;
    }

    /**
     * Finds the surface by scanning FROM TOP TO BOTTOM.
     * Returns the position ON TOP of the first solid block found.
     */
    private BlockPos findSurfaceFromTop(WorldGenLevel level, BlockPos start) {
        // Scan downward from y=256 to find the first solid block (the mountain peak)
        for (int y = 256; y >= 64; y--) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());
            BlockState state = level.getBlockState(checkPos);

            // Found a solid block - place nest ON TOP of it
            if (state.isSolidRender() || !state.isAir()) {
                // Return the position ABOVE this solid block
                BlockPos surfacePos = checkPos.above();

                // Only return if this is high enough for mountains
                if (surfacePos.getY() >= 100) {
                    HKBMod.LOGGER.info("Dragon nest: Scanning found solid block at y={}, placing nest at y={}",
                        y, surfacePos.getY());
                    return surfacePos;
                }
            }
        }

        HKBMod.LOGGER.info("Dragon nest: No solid blocks found above y=100");
        return null;
    }


    /**
     * Loads and places the dragon nest structure from the NBT file.
     * The structure file is located at data/hkbmod/structure/dragon_nest.nbt
     */
    private boolean placeStructure(WorldGenLevel level, BlockPos pos) {
        // Get the structure template manager from the level
        StructureTemplateManager templateManager = level.getLevel().getStructureManager();

        // Load the structure template
        Optional<StructureTemplate> templateOptional = templateManager.get(DRAGON_NEST_STRUCTURE);

        if (templateOptional.isEmpty()) {
            HKBMod.LOGGER.error("Failed to load dragon nest structure template from: {}", DRAGON_NEST_STRUCTURE);
            return false;
        }

        StructureTemplate template = templateOptional.get();

        // Create placement settings
        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(Rotation.NONE)
            .setMirror(Mirror.NONE)
            .setIgnoreEntities(false);

        // Get the structure size to center it properly
        Vec3i size = template.getSize();

        // Calculate offset to center the structure at the placement position
        // Subtract half the structure size to center it
        BlockPos placementPos = pos.offset(-size.getX() / 2, 0, -size.getZ() / 2);

        // Place the structure
        template.placeInWorld(level, placementPos, placementPos, settings, level.getRandom(), 2);

        HKBMod.LOGGER.info("Dragon nest structure placed at {} (centered from {})", placementPos, pos);
        return true;
    }

    /**
     * Spawns 1-3 dragons near the nest after it's been placed.
     * Dragons are spawned in the air around the nest to make them look natural.
     */
    private void spawnDragons(WorldGenLevel level, BlockPos nestPos, RandomSource random) {
        // Only spawn dragons on server side
        if (!(level.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Random number of dragons: 1 to 3
        int dragonCount = DRAGON_COUNT;

        HKBMod.LOGGER.info("Dragon nest: Spawning {} dragons near nest at {}", dragonCount, nestPos);

        for (int i = 0; i < dragonCount; i++) {
            // Spawn dragons in a circle around the nest, in the air
            double angle = (2 * Math.PI * i) / dragonCount; // Evenly distribute around nest
            double radius = 5.0 + random.nextDouble() * 3.0; // 5-8 blocks away from center

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = 3.0 + random.nextDouble() * 2.0; // 3-5 blocks above nest

            BlockPos spawnPos = nestPos.offset((int) offsetX, (int) offsetY, (int) offsetZ);

            // Create and spawn the dragon using the EntityType constructor
            DragonEntity dragon = new DragonEntity(ModEntities.DRAGON.get(), serverLevel);

            // Position the dragon
            dragon.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            dragon.setYRot(random.nextFloat() * 360F);

            // Set the dragon's home position to this nest
            dragon.setNestPosition(nestPos);

            // Add the dragon to the world
            serverLevel.addFreshEntity(dragon);

            HKBMod.LOGGER.info("Dragon nest: Spawned dragon {} at {}", i + 1, spawnPos);
        }
    }
}
