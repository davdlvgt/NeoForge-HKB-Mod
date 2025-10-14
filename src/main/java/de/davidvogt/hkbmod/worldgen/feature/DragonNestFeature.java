package de.davidvogt.hkbmod.worldgen.feature;

import com.mojang.serialization.Codec;
import de.davidvogt.hkbmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Custom feature that places dragon nests on mountain peaks.
 * Creates a small platform with a dragon nest block in the center.
 */
public class DragonNestFeature extends Feature<NoneFeatureConfiguration> {

    public DragonNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        // Check if we're at a suitable height (mountain peaks are usually 150-250)
        if (origin.getY() < 140 || origin.getY() > 256) {
            return false;
        }

        // Check if the area is relatively flat and suitable for a nest
        if (!isSuitableLocation(level, origin)) {
            return false;
        }

        // Build the nest platform
        buildNestPlatform(level, origin);

        // Place the nest block in the center
        BlockPos nestPos = origin.above();
        level.setBlock(nestPos, ModBlocks.DRAGON_NEST.get().defaultBlockState(), 3);

        return true;
    }

    /**
     * Checks if the location is suitable for a dragon nest
     */
    private boolean isSuitableLocation(WorldGenLevel level, BlockPos pos) {
        // Check that we have solid ground beneath
        BlockState below = level.getBlockState(pos.below());
        if (!below.isSolid()) {
            return false;
        }

        // Check that there's open air above (at least 10 blocks for dragon takeoff)
        for (int y = 1; y <= 10; y++) {
            BlockState above = level.getBlockState(pos.above(y));
            if (!above.isAir() && !above.canBeReplaced()) {
                return false;
            }
        }

        // Check that the area is somewhat flat (check 5x5 area)
        int solidBlocks = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                BlockState state = level.getBlockState(checkPos.below());
                if (state.isSolid()) {
                    solidBlocks++;
                }
            }
        }

        // At least 60% of the 5x5 area should be solid
        return solidBlocks >= 15;
    }

    /**
     * Builds a small stone platform for the nest
     */
    private void buildNestPlatform(WorldGenLevel level, BlockPos center) {
        // Create a 7x7 platform at the center position
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double distance = Math.sqrt(x * x + z * z);

                // Create circular-ish platform
                if (distance <= 3.5) {
                    BlockPos platformPos = center.offset(x, 0, z);

                    // Use stone, cobblestone, or andesite for natural look
                    BlockState platformBlock;
                    int random = level.getRandom().nextInt(10);
                    if (random < 5) {
                        platformBlock = Blocks.STONE.defaultBlockState();
                    } else if (random < 8) {
                        platformBlock = Blocks.COBBLESTONE.defaultBlockState();
                    } else {
                        platformBlock = Blocks.ANDESITE.defaultBlockState();
                    }

                    // Only place if there's air or replaceable block
                    BlockState existing = level.getBlockState(platformPos);
                    if (existing.isAir() || existing.canBeReplaced()) {
                        level.setBlock(platformPos, platformBlock, 3);
                    }
                }
            }
        }

        // Add some scattered bones/decoration around the nest
        for (int i = 0; i < 5; i++) {
            int offsetX = level.getRandom().nextInt(5) - 2;
            int offsetZ = level.getRandom().nextInt(5) - 2;
            BlockPos decorPos = center.offset(offsetX, 1, offsetZ);

            if (level.getBlockState(decorPos).isAir()) {
                // Randomly place bones or nothing
                if (level.getRandom().nextFloat() < 0.3f) {
                    level.setBlock(decorPos, Blocks.BONE_BLOCK.defaultBlockState(), 3);
                }
            }
        }
    }
}
