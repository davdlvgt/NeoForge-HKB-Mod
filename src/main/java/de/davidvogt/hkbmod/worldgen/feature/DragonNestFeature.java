package de.davidvogt.hkbmod.worldgen.feature;

import com.mojang.serialization.Codec;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Custom feature that places dragon nests on mountain peaks.
 * Creates a 7x7 platform with mossy cobblestone stairs, slabs, nest block and dragon eggs.
 */
public class DragonNestFeature extends Feature<NoneFeatureConfiguration> {

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

        // Build the nest structure (no space check - we build it anyway for testing)
        buildDragonNest(level, surfacePos);

        HKBMod.LOGGER.info("=== DRAGON NEST: Successfully placed at: {} ===", surfacePos);

        return true;
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
     * Checks if there's enough open space above for the nest
     */
    private boolean hasEnoughSpace(WorldGenLevel level, BlockPos pos) {
        // Check at least 5 blocks of air above
        for (int y = 1; y <= 5; y++) {
            BlockState above = level.getBlockState(pos.above(y));
            if (!above.isAir() && !above.canBeReplaced()) {
                return false;
            }
        }

        // Check that we have solid ground
        BlockState below = level.getBlockState(pos.below());
        return below.isSolidRender();
    }

    /**
     * Checks if the location is suitable for a dragon nest
     * Requires at least a 5x5 flat area, ideally 7x7
     */
    private boolean isSuitableLocation(WorldGenLevel level, BlockPos pos) {
        // DISABLED FOR TESTING - always return true
        return true;
    }


    /**
     * Builds the complete dragon nest structure:
     * - 7x7 base with mossy cobblestone stairs at edges
     * - Mossy cobblestone slabs filling between stairs and center
     * - Dragon nest block in center
     * - Double slabs with dragon eggs in N, E, S, W positions
     */
    private void buildDragonNest(WorldGenLevel level, BlockPos center) {
        // Clear and flatten the area if needed
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos basePos = center.offset(x, 0, z);
                BlockState existing = level.getBlockState(basePos);

                // Replace air or replaceable blocks with solid base
                if (existing.isAir() || existing.canBeReplaced()) {
                    level.setBlock(basePos, Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }

        // Layer 1: Mossy Cobblestone Slabs as base layer
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x == 0 && z == 0) {
                    continue; // Center is for nest block
                }

                BlockPos slabPos = center.offset(x, 1, z);
                level.setBlock(slabPos, Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState(), 3);
            }
        }

        // Layer 2: Place stairs at the outer edge (distance 3 from center)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int absX = Math.abs(x);
                int absZ = Math.abs(z);

                // Only place stairs at the very edge (distance 3)
                if (absX == 3 || absZ == 3) {
                    BlockPos stairPos = center.offset(x, 1, z);
                    BlockState stairState = getStairStateForPosition(x, z);
                    level.setBlock(stairPos, stairState, 3);
                }
            }
        }

        // Place the nest block in the center at ground level + 1
        BlockPos nestPos = center.above();
        level.setBlock(nestPos, ModBlocks.DRAGON_NEST.get().defaultBlockState(), 3);

        // ========================================
        // DRAGON EGGS PLATZIERUNG
        // ========================================
        // HIER WERDEN DIE 4 DRAGON EGGS PLATZIERT!
        // - Auf doppelten Mossy Cobblestone Slabs (ein Slab auf dem anderen)
        // - In den 4 Himmelsrichtungen: Nord, Ost, Süd, West
        // - Direkt neben dem Nest-Block
        // ========================================
        placeDoubleSlabWithEgg(level, center.offset(0, 1, -1)); // North (Norden)
        placeDoubleSlabWithEgg(level, center.offset(1, 1, 0));   // East (Osten)
        placeDoubleSlabWithEgg(level, center.offset(0, 1, 1));  // South (Süden)
        placeDoubleSlabWithEgg(level, center.offset(-1, 1, 0));  // West (Westen)
        // ========================================
    }

    /**
     * Returns the appropriate stair state based on position to create an inward-facing border
     */
    private BlockState getStairStateForPosition(int x, int z) {
        BlockState baseState = Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState();

        // Determine facing direction (stairs should face inward toward nest)
        Direction facing;

        if (z == -3) {
            // North edge, face south (inward)
            facing = Direction.SOUTH;
        } else if (z == 3) {
            // South edge, face north (inward)
            facing = Direction.NORTH;
        } else if (x == -3) {
            // West edge, face east (inward)
            facing = Direction.EAST;
        } else {
            // East edge, face west (inward)
            facing = Direction.WEST;
        }

        return baseState.setValue(StairBlock.FACING, facing)
                       .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    /**
     * Places a second slab on top of an existing slab and puts a dragon egg on top
     */
    private void placeDoubleSlabWithEgg(WorldGenLevel level, BlockPos baseSlabPos) {
        // Place second slab on top of the existing one
        level.setBlock(baseSlabPos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        BlockPos eggPos = baseSlabPos.above();
        level.setBlock(eggPos, ModBlocks.DRAGON_EGG.get().defaultBlockState(), 3);
    }
}
