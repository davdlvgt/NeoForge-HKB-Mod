package de.davidvogt.hkbmod.gate;

import de.davidvogt.hkbmod.block.custom.GateBlock;
import de.davidvogt.hkbmod.block.custom.GateControlBlock;
import de.davidvogt.hkbmod.block.custom.GateSlideBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Utility class for detecting and validating gate structures.
 * Handles the complex logic of finding connected gate blocks and slide blocks.
 *
 * Gate structure is like a Nether Portal:
 * - Slide blocks form a frame (left side, right side, top, bottom)
 * - Gate blocks fill the inside of the frame
 * - Control block is part of one of the side columns
 */
public class GateDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateDetector.class);

    /**
     * Detects a gate structure starting from a control block position.
     *
     * @param level      The level/world
     * @param controlPos Position of the gate control block
     * @return GateStructure if valid, null if invalid or not found
     */
    public static GateStructure detectGateStructure(Level level, BlockPos controlPos) {
        LOGGER.info("Starting gate detection from control block at {}", controlPos);

        // Get the control block's facing direction
        BlockState controlState = level.getBlockState(controlPos);
        if (!(controlState.getBlock() instanceof GateControlBlock)) {
            LOGGER.warn("Block at {} is not a GateControlBlock", controlPos);
            return null;
        }

        Direction controlFacing = controlState.getValue(GateControlBlock.FACING);
        LOGGER.info("Control block facing: {}", controlFacing);

        // Step 1: Find first gate block - look in the direction the control block is facing
        BlockPos firstGatePos = findFirstGateBlock(level, controlPos, controlFacing);
        if (firstGatePos == null) {
            LOGGER.warn("No gate blocks found in direction {} from control block", controlFacing);
            return null;
        }

        LOGGER.info("Found first gate block at {}", firstGatePos);

        // Step 2: Find all connected gate blocks (flood fill)
        Set<BlockPos> gateBlocks = findAllGateBlocks(level, firstGatePos);
        if (gateBlocks.isEmpty()) {
            LOGGER.warn("No connected gate blocks found");
            return null;
        }

        LOGGER.info("Found {} gate blocks", gateBlocks.size());

        // CRITICAL FIX: Check if gate is currently open and normalize to closed positions
        // This ensures the structure always stores CLOSED positions regardless of current state
        boolean gateCurrentlyOpen = isGateOpen(level, gateBlocks);
        Set<BlockPos> closedGateBlocks = gateBlocks;

        if (gateCurrentlyOpen) {
            LOGGER.info("Gate is currently OPEN - calculating closed positions");
            closedGateBlocks = calculateClosedPositions(level, gateBlocks);
            LOGGER.info("Normalized {} gate blocks to closed positions", closedGateBlocks.size());
        } else {
            LOGGER.info("Gate is currently CLOSED - using detected positions as-is");
        }

        // Use closedGateBlocks for all further calculations
        gateBlocks = closedGateBlocks;

        // Step 3: Calculate gate bounds
        GateBounds bounds = calculateGateBounds(gateBlocks);
        LOGGER.info("Gate bounds: {} x {} (width x height)", bounds.width(), bounds.height());

        // Step 4: Determine gate orientation and find slide columns
        // The gate extends perpendicular to the control facing direction
        // For example, if control faces SOUTH (Z+), the gate extends in X direction
        // and slides are at minX-1 and maxX+1, all with same Z range as gate

        Direction leftDir = controlFacing.getCounterClockWise();
        Direction rightDir = controlFacing.getClockWise();

        LOGGER.info("Gate orientation: facing={}, left={}, right={}", controlFacing, leftDir, rightDir);

        // Find slide columns on both sides of the gate
        // The slides form the vertical frame on left and right sides
        List<BlockPos> leftSlides = findSlideColumnAtBounds(level, bounds, leftDir);
        List<BlockPos> rightSlides = findSlideColumnAtBounds(level, bounds, rightDir);

        if (leftSlides.isEmpty() || rightSlides.isEmpty()) {
            LOGGER.warn("Missing slide blocks - Left: {}, Right: {}", leftSlides.size(), rightSlides.size());
            return null;
        }

        LOGGER.info("Found slide blocks - Left: {}, Right: {}", leftSlides.size(), rightSlides.size());

        // Step 5: Find boundary markers
        // Boundaries are horizontal blocks ABOVE and BELOW the gate, not in the side columns!
        // Search for boundaries at the gate's Z positions, not at the slide column positions
        BlockPos leftLowerBoundary = findHorizontalBoundaryMarker(level, bounds, true);
        BlockPos leftUpperBoundary = findHorizontalBoundaryMarker(level, bounds, false);

        // Boundaries are OPTIONAL - if not found, calculate them from slide column positions
        if (leftLowerBoundary == null || leftUpperBoundary == null) {
            LOGGER.info("Boundary markers not found or incomplete - calculating from slide columns");

            // Find the actual boundary positions (first and last slide in column)
            BlockPos leftMin = leftSlides.stream()
                .min(Comparator.comparingInt(BlockPos::getY))
                .orElse(null);
            BlockPos leftMax = leftSlides.stream()
                .max(Comparator.comparingInt(BlockPos::getY))
                .orElse(null);
            BlockPos rightMin = rightSlides.stream()
                .min(Comparator.comparingInt(BlockPos::getY))
                .orElse(null);
            BlockPos rightMax = rightSlides.stream()
                .max(Comparator.comparingInt(BlockPos::getY))
                .orElse(null);

            if (leftMin == null || leftMax == null || rightMin == null || rightMax == null) {
                LOGGER.warn("Could not determine slide boundaries");
                return null;
            }

            // Use lowest and highest slides as boundaries
            leftLowerBoundary = leftMin;
            leftUpperBoundary = leftMax;

            LOGGER.info("Using slide positions as boundaries - Lower: Y={}, Upper: Y={}",
                       leftLowerBoundary.getY(), leftUpperBoundary.getY());
        } else {
            LOGGER.info("Found boundary markers - Lower: Y={}, Upper: Y={}",
                       leftLowerBoundary.getY(), leftUpperBoundary.getY());
        }

        // Boundaries must be at the same Y level on both sides (already ensured by our search)
        if (leftLowerBoundary.getY() >= leftUpperBoundary.getY()) {
            LOGGER.warn("Invalid boundary configuration - Lower Y={} >= Upper Y={}",
                    leftLowerBoundary.getY(), leftUpperBoundary.getY());
            return null;
        }


        // Step 6: Calculate maximum travel distance based on boundaries
        int lowerBoundY = leftLowerBoundary.getY();
        int upperBoundY = leftUpperBoundary.getY();
        int maxTravel = calculateMaxTravelFromBoundaries(lowerBoundY, upperBoundY, bounds.height());

        LOGGER.info("Maximum travel distance: {} blocks (from Y{} to Y{})", maxTravel, lowerBoundY, upperBoundY);

        if (maxTravel <= 0) {
            LOGGER.warn("Cannot move gate - insufficient space between boundaries");
            return null;
        }

        return new GateStructure(
                new ArrayList<>(gateBlocks),
                leftSlides,
                rightSlides,
                controlPos,
                maxTravel,
                bounds.width(),
                bounds.height(),
                leftLowerBoundary,
                leftUpperBoundary
        );
    }

    /**
     * Finds the first gate block in the direction the control block is facing.
     * Searches horizontally and vertically upward if gate is moved.
     */
    private static BlockPos findFirstGateBlock(Level level, BlockPos controlPos, Direction facing) {
        // First check in the facing direction at the same level
        BlockPos checkPos = controlPos.relative(facing);
        if (level.getBlockState(checkPos).getBlock() instanceof GateBlock) {
            return checkPos;
        }

        // If no gate block found horizontally, search vertically upward
        // This handles the case where the gate is open and blocks are above
        for (int yOffset = 1; yOffset <= 64; yOffset++) {
            checkPos = controlPos.relative(facing).above(yOffset);
            if (level.getBlockState(checkPos).getBlock() instanceof GateBlock) {
                LOGGER.info("Found gate block {} blocks above control block", yOffset);
                return checkPos;
            }

            // Stop searching if we hit a non-slide block in the column above control
            BlockPos aboveControl = controlPos.above(yOffset);
            BlockState state = level.getBlockState(aboveControl);
            if (!(state.getBlock() instanceof GateSlideBlock || state.getBlock() instanceof GateControlBlock)) {
                // We've left the slide column, stop searching
                break;
            }
        }

        return null;
    }

    /**
     * Finds all connected gate blocks using flood fill
     */
    private static Set<BlockPos> findAllGateBlocks(Level level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        toCheck.add(start);

        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.poll();

            if (found.contains(current)) {
                continue;
            }

            if (!(level.getBlockState(current).getBlock() instanceof GateBlock)) {
                continue;
            }

            found.add(current);

            // Check all 6 directions (including up/down for multi-layer gates)
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!found.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        return found;
    }

    /**
     * Calculates the bounding box of the gate
     */
    private static GateBounds calculateGateBounds(Set<BlockPos> gateBlocks) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : gateBlocks) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        int width = Math.max(maxX - minX + 1, maxZ - minZ + 1);
        int height = maxY - minY + 1;

        return new GateBounds(minX, minY, minZ, maxX, maxY, maxZ, width, height);
    }

    /**
     * Finds slide blocks forming a vertical column at the gate boundary.
     * The slides form the left and right "rails" of the gate structure.
     *
     * Strategy: Since the gate can be in any orientation, we need to find which
     * direction the slides are actually at. We check both perpendicular directions.
     */
    private static List<BlockPos> findSlideColumnAtBounds(Level level, GateBounds bounds, Direction sideDirection) {
        List<BlockPos> slides = new ArrayList<>();

        LOGGER.info("Looking for slide column on {} side of gate (bounds: X={} to {}, Z={} to {})",
                    sideDirection, bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ());

        // The gate extends in one direction, slides are perpendicular
        // Check if gate extends in X or Z direction
        boolean gateExtendsInX = (bounds.maxX() - bounds.minX()) > 0;
        boolean gateExtendsInZ = (bounds.maxZ() - bounds.minZ()) > 0;

        LOGGER.info("Gate extends: X={}, Z={}", gateExtendsInX, gateExtendsInZ);

        // Case 1: Gate extends in Z direction (like your build)
        // Slides should be at minZ-1 (NORTH) and maxZ+1 (SOUTH)
        if (gateExtendsInZ && !gateExtendsInX) {
            LOGGER.info("Gate extends in Z direction - looking for slides NORTH/SOUTH");

            if (sideDirection == Direction.WEST || sideDirection == Direction.NORTH) {
                // Left side = NORTH (minZ - 1)
                int slideZ = bounds.minZ() - 1;
                LOGGER.info("Searching for LEFT slides at Z={}, X={}", slideZ, bounds.minX());
                findSlideBlocksInColumn(level, new BlockPos(bounds.minX(), bounds.maxY(), slideZ), slides);
            } else if (sideDirection == Direction.EAST || sideDirection == Direction.SOUTH) {
                // Right side = SOUTH (maxZ + 1)
                int slideZ = bounds.maxZ() + 1;
                LOGGER.info("Searching for RIGHT slides at Z={}, X={}", slideZ, bounds.minX());
                findSlideBlocksInColumn(level, new BlockPos(bounds.minX(), bounds.maxY(), slideZ), slides);
            }
        }
        // Case 2: Gate extends in X direction
        // Slides should be at minX-1 (WEST) and maxX+1 (EAST)
        else if (gateExtendsInX && !gateExtendsInZ) {
            LOGGER.info("Gate extends in X direction - looking for slides WEST/EAST");

            if (sideDirection == Direction.WEST) {
                // Left side = WEST (minX - 1)
                int slideX = bounds.minX() - 1;
                LOGGER.info("Searching for LEFT slides at X={}, Z={}", slideX, bounds.minZ());
                findSlideBlocksInColumn(level, new BlockPos(slideX, bounds.maxY(), bounds.minZ()), slides);
            } else if (sideDirection == Direction.EAST) {
                // Right side = EAST (maxX + 1)
                int slideX = bounds.maxX() + 1;
                LOGGER.info("Searching for RIGHT slides at X={}, Z={}", slideX, bounds.minZ());
                findSlideBlocksInColumn(level, new BlockPos(slideX, bounds.maxY(), bounds.minZ()), slides);
            } else if (sideDirection == Direction.NORTH) {
                // North interpreted as left for X-extending gates
                int slideX = bounds.minX() - 1;
                LOGGER.info("Searching for LEFT (NORTH) slides at X={}, Z={}", slideX, bounds.minZ());
                findSlideBlocksInColumn(level, new BlockPos(slideX, bounds.maxY(), bounds.minZ()), slides);
            } else if (sideDirection == Direction.SOUTH) {
                // South interpreted as right for X-extending gates
                int slideX = bounds.maxX() + 1;
                LOGGER.info("Searching for RIGHT (SOUTH) slides at X={}, Z={}", slideX, bounds.minZ());
                findSlideBlocksInColumn(level, new BlockPos(slideX, bounds.maxY(), bounds.minZ()), slides);
            }
        }
        // Case 3: Gate is single block or extends in both directions (3D gate)
        else {
            LOGGER.warn("Gate has unusual dimensions - trying all directions");
            // Try all four directions
            BlockPos testPos = null;
            switch (sideDirection) {
                case WEST:
                    testPos = new BlockPos(bounds.minX() - 1, bounds.maxY(), bounds.minZ());
                    break;
                case EAST:
                    testPos = new BlockPos(bounds.maxX() + 1, bounds.maxY(), bounds.minZ());
                    break;
                case NORTH:
                    testPos = new BlockPos(bounds.minX(), bounds.maxY(), bounds.minZ() - 1);
                    break;
                case SOUTH:
                    testPos = new BlockPos(bounds.minX(), bounds.maxY(), bounds.maxZ() + 1);
                    break;
            }
            if (testPos != null) {
                findSlideBlocksInColumn(level, testPos, slides);
            }
        }

        // Remove duplicates
        Set<BlockPos> uniqueSlides = new LinkedHashSet<>(slides);
        slides.clear();
        slides.addAll(uniqueSlides);

        LOGGER.info("Found {} slide blocks on {} side", slides.size(), sideDirection);
        return slides;
    }

    /**
     * Helper method to find all slide/control blocks in a vertical column at a given X/Z position.
     * Scans downward to find bottom, then upward to collect all blocks.
     */
    private static void findSlideBlocksInColumn(Level level, BlockPos startPos, List<BlockPos> results) {
        LOGGER.info("Scanning column at X={}, Z={}, starting Y={}", startPos.getX(), startPos.getZ(), startPos.getY());
        
        // Check what's at the start position
        BlockState startState = level.getBlockState(startPos);
        LOGGER.info("  Block at start position: {}", startState.getBlock().getClass().getSimpleName());
        
        // Scan down to find the bottom - extend search range to include boundaries below gate
        BlockPos scanPos = startPos;
        int minY = Math.max(level.getMinY(), startPos.getY() - 128); // Increased range to find lower boundaries

        LOGGER.info("  Scanning DOWN from Y={} to Y={}", startPos.getY(), minY);
        while (scanPos.getY() > minY) {
            BlockPos below = scanPos.below();
            BlockState belowState = level.getBlockState(below);
            LOGGER.info("    Y={}: {}", below.getY(), belowState.getBlock().getClass().getSimpleName());

            if (belowState.getBlock() instanceof GateSlideBlock || belowState.getBlock() instanceof GateControlBlock) {
                scanPos = below;
            } else {
                LOGGER.info("    Found non-slide block, stopping downward scan");
                break;
            }
        }

        LOGGER.info("  Bottom found at Y={}", scanPos.getY());

        // Now scan upward from the bottom to collect ALL slide/control blocks
        int maxY = Math.min(level.getMaxY(), scanPos.getY() + 256); // Scan higher to include upper boundaries
        BlockPos currentPos = scanPos;

        LOGGER.info("  Scanning UP from Y={} to Y={}", scanPos.getY(), maxY);
        int foundCount = 0;
        while (currentPos.getY() <= maxY) {
            BlockState state = level.getBlockState(currentPos);
            String blockName = state.getBlock().getClass().getSimpleName();
            LOGGER.info("    Y={}: {}", currentPos.getY(), blockName);

            if (state.getBlock() instanceof GateSlideBlock || state.getBlock() instanceof GateControlBlock) {
                if (!results.contains(currentPos)) {
                    results.add(currentPos.immutable());
                    foundCount++;
                    LOGGER.info("      -> Added to results (total: {})", foundCount);
                }
                currentPos = currentPos.above();
            } else {
                LOGGER.info("      -> Not a slide/control block, stopping upward scan");
                break;
            }
        }

        LOGGER.info("  Column scan complete: found {} slide/control blocks at X={}, Z={}", foundCount, startPos.getX(), startPos.getZ());
    }

    /**
     * Finds a boundary marker in a slide column
     * @param slideBlocks List of slide block positions
     * @param findLower true to find lower boundary (FACING=UP), false for upper boundary (FACING=DOWN)
     * @return Position of the boundary marker, or null if not found
     */
    private static BlockPos findBoundaryMarker(Level level, List<BlockPos> slideBlocks, boolean findLower) {
        for (BlockPos pos : slideBlocks) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GateSlideBlock) {
                if (findLower && GateSlideBlock.isLowerBoundary(state)) {
                    return pos;
                } else if (!findLower && GateSlideBlock.isUpperBoundary(state)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Finds a horizontal boundary marker above or below the gate
     * @param level The world level
     * @param bounds The gate bounds
     * @param findLower True to find the lower boundary (below the gate), false for the upper boundary (above the gate)
     * @return Position of the boundary marker, or null if not found
     */
    private static BlockPos findHorizontalBoundaryMarker(Level level, GateBounds bounds, boolean findLower) {
        // Determine the Y level to search at
        int searchY = findLower ? bounds.minY() - 1 : bounds.maxY() + 1;

        LOGGER.info("Searching for {} boundary at Y={}", findLower ? "LOWER" : "UPPER", searchY);

        // Scan all positions in the horizontal plane at searchY that align with the gate
        // For a gate extending in Z direction, scan all Z positions at the gate's X
        boolean gateExtendsInZ = (bounds.maxZ() - bounds.minZ()) > 0;

        if (gateExtendsInZ) {
            // Gate extends in Z direction (Z=13 to 17), so scan all those Z positions
            int x = bounds.minX();
            LOGGER.info("  Scanning Z={} to Z={} at X={}, Y={}", bounds.minZ(), bounds.maxZ(), x, searchY);

            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                BlockPos checkPos = new BlockPos(x, searchY, z);
                BlockState state = level.getBlockState(checkPos);

                if (state.getBlock() instanceof GateSlideBlock) {
                    boolean isCorrectType = findLower ? GateSlideBlock.isLowerBoundary(state) : GateSlideBlock.isUpperBoundary(state);

                    LOGGER.info("    Z={}: Found {} (isCorrectType={})",
                               z, state.getBlock().getClass().getSimpleName(), isCorrectType);

                    if (isCorrectType) {
                        LOGGER.info("  -> Found {} boundary marker at {}", findLower ? "LOWER" : "UPPER", checkPos);
                        return checkPos;
                    }
                }
            }
        } else {
            // Gate extends in X direction, so scan all X positions
            int z = bounds.minZ();
            LOGGER.info("  Scanning X={} to X={} at Z={}, Y={}", bounds.minX(), bounds.maxX(), z, searchY);

            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                BlockPos checkPos = new BlockPos(x, searchY, z);
                BlockState state = level.getBlockState(checkPos);

                if (state.getBlock() instanceof GateSlideBlock) {
                    boolean isCorrectType = findLower ? GateSlideBlock.isLowerBoundary(state) : GateSlideBlock.isUpperBoundary(state);

                    LOGGER.info("    X={}: Found {} (isCorrectType={})",
                               x, state.getBlock().getClass().getSimpleName(), isCorrectType);

                    if (isCorrectType) {
                        LOGGER.info("  -> Found {} boundary marker at {}", findLower ? "LOWER" : "UPPER", checkPos);
                        return checkPos;
                    }
                }
            }
        }

        LOGGER.warn("  -> {} boundary marker not found at Y={}", findLower ? "LOWER" : "UPPER", searchY);
        return null;
    }

    /**
     * Calculates maximum travel distance based on boundary positions and gate height.
     *
     * The gate blocks should move from their starting position upward until they reach
     * the upper boundary. The boundaries are HORIZONTAL blocks at the top and bottom.
     *
     * NEW LOGIC: The gate should be able to move COMPLETELY between the boundaries,
     * from just above the lower boundary to just below the upper boundary.
     * This means the maximum travel is the full space minus the gate height.
     */
    private static int calculateMaxTravelFromBoundaries(int lowerBoundY, int upperBoundY, int gateHeight) {
        // The gate should be able to move from:
        //   CLOSED: Starting at (lowerBoundY + 1) to (lowerBoundY + gateHeight)
        //   OPEN: Ending at (upperBoundY - gateHeight) to (upperBoundY - 1)
        //
        // Example: Lower at Y=-61, Upper at Y=-56, Gate height=2
        //   Total space = -56 - (-61) - 1 = 4 blocks (exclusive of boundaries)
        //   Closed position: Y=-60 to Y=-59 (bottom at Y=-60)
        //   Open position: Y=-57 to Y=-56 (top at Y=-56)
        //   Travel distance: -56 - (-60) = 4 blocks? No!
        //
        // Better calculation:
        //   Lowest Y the gate bottom can be: lowerBoundY + 1 = -60
        //   Highest Y the gate top can be: upperBoundY - 1 = -57
        //   Highest Y the gate bottom can be: upperBoundY - gateHeight = -58
        //   Max travel: (upperBoundY - gateHeight) - (lowerBoundY + 1) = -58 - (-60) = 2

        int lowestGateBottom = lowerBoundY + 1;  // Gate can't go below this
        int highestGateBottom = upperBoundY - gateHeight;  // Gate can't go above this
        int maxTravel = highestGateBottom - lowestGateBottom;

        LOGGER.info("Boundary calculation: lowerBoundY={}, upperBoundY={}, gateHeight={}",
                    lowerBoundY, upperBoundY, gateHeight);
        LOGGER.info("  Lowest gate bottom Y: {}", lowestGateBottom);
        LOGGER.info("  Highest gate bottom Y: {}", highestGateBottom);
        LOGGER.info("  Max travel: {}", maxTravel);

        return Math.max(0, maxTravel);
    }

    /**
     * Helper record for gate bounds
     */
    private record GateBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int width, int height) {
    }

    /**
     * Checks if the gate is currently in the open (raised) position by examining the OPEN blockstate property.
     * If most blocks have OPEN=true, the gate is considered open.
     */
    private static boolean isGateOpen(Level level, Set<BlockPos> gateBlocks) {
        int openCount = 0;
        int totalCount = 0;

        for (BlockPos pos : gateBlocks) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GateBlock) {
                totalCount++;
                if (state.getValue(GateBlock.OPEN)) {
                    openCount++;
                }
            }
        }

        // Gate is considered open if more than half the blocks are marked as open
        boolean isOpen = openCount > totalCount / 2;
        LOGGER.info("Gate open check: {}/{} blocks are marked OPEN -> gate is {}",
                   openCount, totalCount, isOpen ? "OPEN" : "CLOSED");
        return isOpen;
    }

    /**
     * Calculates the CLOSED (original) positions of gate blocks that are currently OPEN (raised).
     *
     * IMPORTANT: This handles irregular gates where players added blocks at different heights.
     * - Blocks with OPEN=false are already at closed position (keep as-is)
     * - Blocks with OPEN=true need to be normalized down to closed position
     */
    private static Set<BlockPos> calculateClosedPositions(Level level, Set<BlockPos> openGateBlocks) {
        if (openGateBlocks.isEmpty()) {
            return openGateBlocks;
        }

        // Separate blocks by their OPEN state
        Set<BlockPos> alreadyClosedBlocks = new HashSet<>();
        Set<BlockPos> openBlocks = new HashSet<>();

        for (BlockPos pos : openGateBlocks) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GateBlock) {
                if (state.getValue(GateBlock.OPEN)) {
                    openBlocks.add(pos);
                } else {
                    alreadyClosedBlocks.add(pos);
                }
            }
        }

        LOGGER.info("Block state analysis: {} blocks at closed position, {} blocks at open position",
                   alreadyClosedBlocks.size(), openBlocks.size());

        // If no blocks are open, all are already at closed position
        if (openBlocks.isEmpty()) {
            LOGGER.info("All blocks already at closed position");
            return openGateBlocks;
        }

        // If we have blocks already at closed position, use them to determine the closed Y level
        Integer closedMinY = null;
        if (!alreadyClosedBlocks.isEmpty()) {
            closedMinY = alreadyClosedBlocks.stream()
                    .mapToInt(BlockPos::getY)
                    .min()
                    .orElse(0);
            LOGGER.info("Using already-closed blocks to determine closed level: Y={}", closedMinY);
        }

        // If we don't have closed blocks to reference, find the lower boundary
        if (closedMinY == null) {
            // Find minimum Y of open blocks
            int openMinY = openBlocks.stream()
                    .mapToInt(BlockPos::getY)
                    .min()
                    .orElse(0);

            LOGGER.info("No closed blocks found, searching for lower boundary (open blocks at Y >= {})", openMinY);

            // Find ANY gate block to use as reference
            BlockPos referenceGatePos = openBlocks.iterator().next();
            BlockState referenceState = level.getBlockState(referenceGatePos);
            Direction gateFacing = referenceState.getValue(GateBlock.FACING);

            // Look for slide blocks adjacent to the gate
            Direction slideDir1, slideDir2;
            if (gateFacing == Direction.NORTH || gateFacing == Direction.SOUTH) {
                slideDir1 = Direction.EAST;
                slideDir2 = Direction.WEST;
            } else {
                slideDir1 = Direction.NORTH;
                slideDir2 = Direction.SOUTH;
            }

            // Find slide column
            BlockPos slideColumnPos = null;
            for (Direction dir : new Direction[]{slideDir1, slideDir2}) {
                BlockPos checkPos = referenceGatePos.relative(dir);
                BlockState checkState = level.getBlockState(checkPos);
                if (checkState.getBlock() instanceof GateSlideBlock || checkState.getBlock() instanceof GateControlBlock) {
                    slideColumnPos = checkPos;
                    break;
                }
            }

            if (slideColumnPos == null) {
                LOGGER.warn("Could not find slide column to determine closed position - using current positions");
                return openGateBlocks;
            }

            // Find lower boundary
            BlockPos lowerBoundary = findLowerBoundaryInColumn(level, slideColumnPos, openMinY);
            if (lowerBoundary == null) {
                LOGGER.warn("Could not find lower boundary - using current positions");
                return openGateBlocks;
            }

            closedMinY = lowerBoundary.getY() + 1;
            LOGGER.info("Determined closed level from lower boundary: Y={}", closedMinY);
        }

        // Now normalize the OPEN blocks to closed positions
        // Calculate offset based on the lowest open block
        int openMinY = openBlocks.stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElse(closedMinY);

        int yOffset = openMinY - closedMinY;
        LOGGER.info("Normalizing open blocks: currentMinY={}, closedMinY={}, offset={}",
                   openMinY, closedMinY, yOffset);

        // Build the final set of closed positions
        Set<BlockPos> closedPositions = new HashSet<>();

        // Keep already-closed blocks as-is
        closedPositions.addAll(alreadyClosedBlocks);

        // Normalize open blocks by moving them down by the offset
        for (BlockPos openPos : openBlocks) {
            BlockPos closedPos = openPos.below(yOffset);
            closedPositions.add(closedPos);
            LOGGER.debug("Normalizing open block: {} -> {}", openPos, closedPos);
        }

        LOGGER.info("Final result: {} closed positions ({} kept, {} normalized)",
                   closedPositions.size(), alreadyClosedBlocks.size(), openBlocks.size());

        return closedPositions;
    }

    /**
     * Finds the lower boundary marker in a slide column by scanning downward from a starting position.
     */
    private static BlockPos findLowerBoundaryInColumn(Level level, BlockPos startPos, int minY) {
        // Scan down from startPos to find a lower boundary marker (FACING=UP)
        for (int y = startPos.getY(); y >= Math.max(level.getMinY(), minY - 64); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockState state = level.getBlockState(checkPos);

            if (state.getBlock() instanceof GateSlideBlock && GateSlideBlock.isLowerBoundary(state)) {
                LOGGER.info("Found lower boundary marker at {}", checkPos);
                return checkPos;
            }

            // Also check if we've left the slide column (hit a non-slide/control block)
            // In that case, the last slide/control block we saw is the de-facto lower boundary
            if (!(state.getBlock() instanceof GateSlideBlock || state.getBlock() instanceof GateControlBlock)) {
                // Went past the slide column, return the position just above
                BlockPos lastSlide = checkPos.above();
                LOGGER.info("Slide column ends at Y={}, using {} as lower boundary", y, lastSlide);
                return lastSlide;
            }
        }

        LOGGER.warn("Could not find lower boundary in column at X={}, Z={}", startPos.getX(), startPos.getZ());
        return null;
    }
}
