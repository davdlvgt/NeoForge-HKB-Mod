package de.davidvogt.hkbmod.gate;

import de.davidvogt.hkbmod.block.custom.GateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Handles the animated movement of gate blocks.
 * Each step moves ALL blocks by exactly one block.
 */
public class GateAnimator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateAnimator.class);

    /**
     * Moves all gate blocks up by one position during opening animation.
     * Called once per step - moves ALL blocks one block higher.
     *
     * @param level         The server level
     * @param structure     The gate structure
     * @param currentStep   Current step in the animation (1-based, represents current height offset)
     * @return true if successful, false if blocked
     */
    public static boolean moveGateBlockUp(ServerLevel level, GateStructure structure, int currentStep) {
        if (currentStep <= 0 || currentStep > structure.getMaxTravel()) {
            return true; // Nothing to do
        }

        // Sort gate blocks by Y coordinate (bottom to top)
        List<BlockPos> sortedBlocks = new ArrayList<>(structure.getGateBlocks());
        sortedBlocks.sort(Comparator.comparingInt(BlockPos::getY));

        // Calculate positions: currentStep tells us the current offset from original position
        // currentStep=1 means blocks should be 1 above original
        // currentStep=2 means blocks should be 2 above original, etc.

        // Move from TOP to BOTTOM to avoid overwriting blocks that still need to move
        for (int i = sortedBlocks.size() - 1; i >= 0; i--) {
            BlockPos originalPos = sortedBlocks.get(i);
            BlockPos currentPos = originalPos.above(currentStep - 1); // Where the block is NOW (before this step)
            BlockPos newPos = originalPos.above(currentStep);          // Where the block should be AFTER this step

            // Get the block at current position
            BlockState currentState = level.getBlockState(currentPos);

            // Verify it's a gate block
            if (!(currentState.getBlock() instanceof GateBlock)) {
                LOGGER.warn("Expected gate block at {} but found {}", currentPos, currentState.getBlock());
                continue;
            }

            // Check if target position is clear
            BlockState targetState = level.getBlockState(newPos);
            if (!targetState.isAir() && !(targetState.getBlock() instanceof GateBlock)) {
                LOGGER.warn("Gate movement blocked at {} by {}", newPos, targetState.getBlock());
                return false;
            }

            // Move the block: place at new position, remove from old position
            level.setBlock(newPos, currentState.setValue(GateBlock.OPEN, true), 3);
            level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 3);
        }

        return true;
    }

    /**
     * Moves all gate blocks down by one position during closing animation.
     * Called once per step - moves ALL blocks one block lower.
     *
     * @param level         The server level
     * @param structure     The gate structure
     * @param currentStep   Current step in the animation (1-based, counting down from maxTravel)
     * @return true if successful, false if blocked
     */
    public static boolean moveGateBlockDown(ServerLevel level, GateStructure structure, int currentStep) {
        if (currentStep <= 0 || currentStep > structure.getMaxTravel()) {
            return true; // Nothing to do
        }

        // Sort gate blocks by Y coordinate (bottom to top)
        List<BlockPos> sortedBlocks = new ArrayList<>(structure.getGateBlocks());
        sortedBlocks.sort(Comparator.comparingInt(BlockPos::getY));

        // When closing: currentStep counts from 1 to maxTravel
        // currentStep=1 means we go from maxTravel offset to (maxTravel-1) offset
        // currentStep=maxTravel means we go from offset 1 to offset 0 (original position)
        int currentOffset = structure.getMaxTravel() - currentStep + 1; // Where blocks are NOW
        int newOffset = structure.getMaxTravel() - currentStep;          // Where blocks should be AFTER

        LOGGER.info("Moving gate DOWN: step {}/{}, currentOffset={}, newOffset={}",
                    currentStep, structure.getMaxTravel(), currentOffset, newOffset);

        // Move from BOTTOM to TOP to avoid overwriting blocks that still need to move
        for (int i = 0; i < sortedBlocks.size(); i++) {
            BlockPos originalPos = sortedBlocks.get(i);
            BlockPos currentPos = originalPos.above(currentOffset); // Where the block is NOW
            BlockPos newPos = originalPos.above(newOffset);          // Where the block should be AFTER

            // Get the block at current position
            BlockState currentState = level.getBlockState(currentPos);

            // Verify it's a gate block
            if (!(currentState.getBlock() instanceof GateBlock)) {
                LOGGER.warn("Expected gate block at {} but found {}", currentPos, currentState.getBlock());
                // CRITICAL: Do NOT continue - this means we've lost a gate block!
                // Try to find it nearby
                boolean found = false;
                for (int yCheck = -2; yCheck <= 2; yCheck++) {
                    BlockPos checkPos = currentPos.offset(0, yCheck, 0);
                    if (level.getBlockState(checkPos).getBlock() instanceof GateBlock) {
                        LOGGER.warn("Found gate block at {} instead of expected {}", checkPos, currentPos);
                        currentPos = checkPos;
                        currentState = level.getBlockState(currentPos);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOGGER.error("CRITICAL: Gate block lost at {}! Aborting movement.", currentPos);
                    return false;
                }
            }

            // Check if target position is clear
            BlockState targetState = level.getBlockState(newPos);
            if (!targetState.isAir() && !(targetState.getBlock() instanceof GateBlock)) {
                // Target is blocked by something (likely a boundary or another block)
                // Do NOT overwrite it - this would destroy the block!
                if (newPos.equals(currentPos)) {
                    // Already at target position, nothing to do
                    continue;
                }
                LOGGER.warn("Gate movement blocked at {} by {} - STOPPING to prevent block loss",
                           newPos, targetState.getBlock());
                return false;
            }

            // Move the block: place at new position, remove from old position
            if (!newPos.equals(currentPos)) {
                boolean isClosed = (newOffset == 0);
                level.setBlock(newPos, currentState.setValue(GateBlock.OPEN, !isClosed), 3);
                level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 3);
                LOGGER.debug("Moved gate block from {} to {} (offset {} -> {})",
                            currentPos, newPos, currentOffset, newOffset);
            }
        }

        return true;
    }

    /**
     * Alternative implementation: Instant movement (for testing or config option)
     */
    public static boolean moveGateInstant(ServerLevel level, GateStructure structure, boolean open) {
        List<BlockPos> sortedBlocks = new ArrayList<>(structure.getGateBlocks());
        sortedBlocks.sort(Comparator.comparingInt(BlockPos::getY));

        if (open) {
            // Move all blocks up instantly
            for (int i = sortedBlocks.size() - 1; i >= 0; i--) {
                BlockPos originalPos = sortedBlocks.get(i);
                BlockPos newPos = originalPos.above(structure.getMaxTravel());

                BlockState gateState = level.getBlockState(originalPos);
                if (gateState.getBlock() instanceof GateBlock) {
                    level.setBlock(newPos, gateState.setValue(GateBlock.OPEN, true), 3);
                    level.setBlock(originalPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        } else {
            // Move all blocks down instantly
            for (BlockPos originalPos : sortedBlocks) {
                BlockPos raisedPos = originalPos.above(structure.getMaxTravel());

                BlockState gateState = level.getBlockState(raisedPos);
                if (gateState.getBlock() instanceof GateBlock) {
                    level.setBlock(originalPos, gateState.setValue(GateBlock.OPEN, false), 3);
                    level.setBlock(raisedPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        return true;
    }

    /**
     * Checks if a gate can move (no obstructions in the path).
     *
     * When OPENING (moving up): Checks the path from current position to (current + maxTravel)
     * When CLOSING (moving down): Checks the path from current position to (current - maxTravel)
     *
     * Gate blocks are allowed to pass through each other, but all other blocks are obstructions.
     *
     * @param level   The server level
     * @param structure The gate structure with CLOSED positions
     * @param opening True if opening (moving up), false if closing (moving down)
     * @return true if the path is clear, false if obstructed
     */
    public static boolean canGateMove(ServerLevel level, GateStructure structure, boolean opening) {
        LOGGER.info("Checking if gate can move {} - scanning {} blocks over {} steps",
                   opening ? "UP" : "DOWN", structure.getGateBlocks().size(), structure.getMaxTravel());

        for (BlockPos closedPos : structure.getGateBlocks()) {
            if (opening) {
                // When opening, check the path UPWARD from the closed position
                for (int offset = 1; offset <= structure.getMaxTravel(); offset++) {
                    BlockPos checkPos = closedPos.above(offset);
                    BlockState state = level.getBlockState(checkPos);

                    // Allow air and gate blocks (gate blocks can overlap during detection)
                    if (!state.isAir() && !(state.getBlock() instanceof GateBlock)) {
                        LOGGER.warn("Opening blocked at {} by {} (offset +{})",
                                   checkPos, state.getBlock().getName().getString(), offset);
                        return false;
                    }
                }
            } else {
                // When closing, check the path DOWNWARD to the closed position
                // This is trickier because gate might currently be open (at closedPos + maxTravel)
                // We need to check from the current position down to the closed position

                // First, find where this block currently is
                BlockPos currentPos = null;

                // Check if block is at its closed position
                if (level.getBlockState(closedPos).getBlock() instanceof GateBlock) {
                    currentPos = closedPos;
                }

                // If not, check if it's at the open position
                if (currentPos == null) {
                    BlockPos openPos = closedPos.above(structure.getMaxTravel());
                    if (level.getBlockState(openPos).getBlock() instanceof GateBlock) {
                        currentPos = openPos;
                    }
                }

                // If we still can't find it, search nearby
                if (currentPos == null) {
                    for (int searchOffset = 0; searchOffset <= structure.getMaxTravel(); searchOffset++) {
                        BlockPos searchPos = closedPos.above(searchOffset);
                        if (level.getBlockState(searchPos).getBlock() instanceof GateBlock) {
                            currentPos = searchPos;
                            break;
                        }
                    }
                }

                if (currentPos == null) {
                    LOGGER.warn("Could not find gate block for closed position {} - skipping obstruction check", closedPos);
                    continue;
                }

                // Now check the path from current position down to closed position
                int currentOffset = currentPos.getY() - closedPos.getY();
                for (int offset = currentOffset - 1; offset >= 0; offset--) {
                    BlockPos checkPos = closedPos.above(offset);
                    BlockState state = level.getBlockState(checkPos);

                    // Allow air and gate blocks
                    if (!state.isAir() && !(state.getBlock() instanceof GateBlock)) {
                        LOGGER.warn("Closing blocked at {} by {} (offset +{})",
                                   checkPos, state.getBlock().getName().getString(), offset);
                        return false;
                    }
                }
            }
        }

        LOGGER.info("Gate movement path is clear");
        return true;
    }
}
