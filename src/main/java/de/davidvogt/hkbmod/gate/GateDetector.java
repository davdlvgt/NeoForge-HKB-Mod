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

    // Konstanten für magische Zahlen
    private static final int MAX_VERTICAL_SEARCH_OFFSET = 64;
    private static final int MAX_GATE_BLOCKS = 1000; // Verhindert Speicherprobleme bei fehlerhaften Strukturen
    private static final int SLIDE_COLUMN_SEARCH_RADIUS = 6;
    // PROBLEM 5 FIX: Reduziere übermäßige Suchbereiche - wir suchen nur in der Slide-Säule
    private static final int SLIDE_COLUMN_EXTENSION = 10; // Blocks über/unter dem Gate zu scannen
    private static final int LOWER_BOUNDARY_SEARCH_RANGE = 32; // Reduziert von 64

    /**
     * Detects a gate structure starting from a control block position.
     *
     * @param level      The level/world
     * @param controlPos Position of the gate control block
     * @return GateStructure if valid, null if invalid or not found
     */
    public static GateStructure detectGateStructure(Level level, BlockPos controlPos) {
        if (level == null || controlPos == null) {
            LOGGER.error("Cannot detect gate structure - invalid parameters");
            return null;
        }

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
            if (closedGateBlocks.isEmpty()) {
                LOGGER.error("Failed to calculate closed positions for open gate");
                return null;
            }
            LOGGER.info("Normalized {} gate blocks to closed positions", closedGateBlocks.size());
        } else {
            LOGGER.info("Gate is currently CLOSED - using detected positions as-is");
        }

        // Use closedGateBlocks for all further calculations
        gateBlocks = closedGateBlocks;

        // Step 3: Calculate gate bounds
        GateBounds bounds = calculateGateBounds(gateBlocks);
        if (bounds == null) {
            LOGGER.error("Failed to calculate gate bounds");
            return null;
        }
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
        // PROBLEM 1 & 3 FIX: Search for boundaries in BOTH locations:
        // 1. In the slide columns themselves (FACING=UP/DOWN markers)
        // 2. As horizontal blocks above/below the gate
        // This makes boundary detection more robust and consistent
        BlockPos leftLowerBoundary = findBoundaryMarker(level, leftSlides, true);
        BlockPos leftUpperBoundary = findBoundaryMarker(level, leftSlides, false);

        // If not found in slide columns, try horizontal search
        if (leftLowerBoundary == null || leftUpperBoundary == null) {
            LOGGER.info("Boundaries not found in slide columns - trying horizontal search");
            if (leftLowerBoundary == null) {
                leftLowerBoundary = findHorizontalBoundaryMarker(level, bounds, true);
            }
            if (leftUpperBoundary == null) {
                leftUpperBoundary = findHorizontalBoundaryMarker(level, bounds, false);
            }
        }

        // PROBLEM 3 FIX: Consistent fallback - use min/max of slide columns as boundaries
        if (leftLowerBoundary == null || leftUpperBoundary == null) {
            LOGGER.info("No explicit boundary markers found - using slide column extents as boundaries");

            // Find the actual boundary positions (first and last slide in column)
            Optional<BlockPos> leftMin = leftSlides.stream()
                .min(Comparator.comparingInt(BlockPos::getY));
            Optional<BlockPos> leftMax = leftSlides.stream()
                .max(Comparator.comparingInt(BlockPos::getY));
            Optional<BlockPos> rightMin = rightSlides.stream()
                .min(Comparator.comparingInt(BlockPos::getY));
            Optional<BlockPos> rightMax = rightSlides.stream()
                .max(Comparator.comparingInt(BlockPos::getY));

            if (leftMin.isEmpty() || leftMax.isEmpty() || rightMin.isEmpty() || rightMax.isEmpty()) {
                LOGGER.warn("Could not determine slide boundaries from empty streams");
                return null;
            }

            // Use the min/max of BOTH columns to ensure consistency
            int minSlideY = Math.min(leftMin.get().getY(), rightMin.get().getY());
            int maxSlideY = Math.max(leftMax.get().getY(), rightMax.get().getY());

            // Create boundary positions (use left column X/Z for consistency)
            leftLowerBoundary = new BlockPos(leftMin.get().getX(), minSlideY, leftMin.get().getZ());
            leftUpperBoundary = new BlockPos(leftMax.get().getX(), maxSlideY, leftMax.get().getZ());

            LOGGER.info("Using slide extents as boundaries - Lower: Y={}, Upper: Y={}",
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
        if (level == null || controlPos == null || facing == null) {
            LOGGER.error("Invalid parameters: level={}, controlPos={}, facing={}", level, controlPos, facing);
            return null;
        }

        // First check in the facing direction at the same level
        BlockPos checkPos = controlPos.relative(facing);
        if (level.getBlockState(checkPos).getBlock() instanceof GateBlock) {
            return checkPos;
        }

        // If no gate block found horizontally, search vertically upward and downward
        int minWorldY = level.getMinY();
        int maxWorldY = level.getMaxY();

        boolean continueUp = true;
        boolean continueDown = true;

        for (int yOffset = 1; yOffset <= MAX_VERTICAL_SEARCH_OFFSET && (continueUp || continueDown); yOffset++) {
            if (continueUp) {
                BlockPos upPos = controlPos.relative(facing).above(yOffset);
                if (upPos.getY() <= maxWorldY && level.getBlockState(upPos).getBlock() instanceof GateBlock) {
                    LOGGER.info("Found gate block {} blocks above control block", yOffset);
                    return upPos;
                }

                BlockPos aboveControl = controlPos.above(yOffset);
                if (aboveControl.getY() > maxWorldY) {
                    continueUp = false;
                } else {
                    BlockState state = level.getBlockState(aboveControl);
                    if (!(state.getBlock() instanceof GateSlideBlock || state.getBlock() instanceof GateControlBlock)) {
                        continueUp = false;
                    }
                }
            }

            if (continueDown) {
                BlockPos downPos = controlPos.relative(facing).below(yOffset);
                if (downPos.getY() >= minWorldY && level.getBlockState(downPos).getBlock() instanceof GateBlock) {
                    LOGGER.info("Found gate block {} blocks below control block", yOffset);
                    return downPos;
                }

                BlockPos belowControl = controlPos.below(yOffset);
                if (belowControl.getY() < minWorldY) {
                    continueDown = false;
                } else {
                    BlockState stateDown = level.getBlockState(belowControl);
                    if (!(stateDown.getBlock() instanceof GateSlideBlock || stateDown.getBlock() instanceof GateControlBlock)) {
                        continueDown = false;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds all connected gate blocks using flood fill with size limit for safety
     */
    private static Set<BlockPos> findAllGateBlocks(Level level, BlockPos start) {
        if (level == null || start == null) {
            LOGGER.error("Invalid parameters: level={}, start={}", level, start);
            return Collections.emptySet();
        }

        Set<BlockPos> found = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        toCheck.add(start);

        while (!toCheck.isEmpty()) {
            // Sicherheitsprüfung: Verhindere zu große Strukturen
            if (found.size() >= MAX_GATE_BLOCKS) {
                LOGGER.warn("Gate structure exceeds maximum size of {} blocks - stopping flood fill", MAX_GATE_BLOCKS);
                break;
            }

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
        if (gateBlocks == null || gateBlocks.isEmpty()) {
            LOGGER.error("Cannot calculate bounds for null or empty gate blocks");
            return null;
        }

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
        if (level == null || bounds == null || sideDirection == null) {
            LOGGER.error("Invalid parameters in findSlideColumnAtBounds");
            return Collections.emptyList();
        }

        List<BlockPos> slides = new ArrayList<>();

        LOGGER.info("Looking for slide column on {} side of gate (bounds: X={} to {}, Z={} to {})",
                sideDirection, bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ());

        // Check if gate extends in X or Z direction
        boolean gateExtendsInX = (bounds.maxX() - bounds.minX()) > 0;
        boolean gateExtendsInZ = (bounds.maxZ() - bounds.minZ()) > 0;

        LOGGER.info("Gate extends: X={}, Z={}", gateExtendsInX, gateExtendsInZ);

        // Case 1: Gate extends in Z direction -> slides run along X, columns at Z = minZ-1 or maxZ+1
        if (gateExtendsInZ && !gateExtendsInX) {
            LOGGER.info("Gate extends in Z direction - scanning X range for slide columns");

            int slideZ;
            // Prefer explicit NORTH/SOUTH, but accept WEST/EAST as aliases from caller (left/right)
            if (sideDirection == Direction.NORTH) {
                slideZ = bounds.minZ() - 1;
            } else if (sideDirection == Direction.SOUTH) {
                slideZ = bounds.maxZ() + 1;
            } else {
                // Fallback: decide by closeness to min/max Z (choose left/right semantics handled by caller)
                slideZ = (Math.abs(sideDirection.getStepZ() - bounds.minZ()) <= Math.abs(sideDirection.getStepZ() - bounds.maxZ()))
                        ? bounds.minZ() - 1
                        : bounds.maxZ() + 1;
            }

            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                findSlideBlocksInColumn(level, new BlockPos(x, bounds.maxY(), slideZ), slides);
            }
        }
        // Case 2: Gate extends in X direction -> slides run along Z, columns at X = minX-1 or maxX+1
        else if (gateExtendsInX && !gateExtendsInZ) {
            LOGGER.info("Gate extends in X direction - scanning Z range for slide columns");

            int slideX;
            if (sideDirection == Direction.WEST) {
                slideX = bounds.minX() - 1;
            } else if (sideDirection == Direction.EAST) {
                slideX = bounds.maxX() + 1;
            } else {
                // Fallback similar to above
                slideX = (Math.abs(sideDirection.getStepX() - bounds.minX()) <= Math.abs(sideDirection.getStepX() - bounds.maxX()))
                        ? bounds.minX() - 1
                        : bounds.maxX() + 1;
            }

            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                findSlideBlocksInColumn(level, new BlockPos(slideX, bounds.maxY(), z), slides);
            }
        }
        // Case 3: Single block or 3D gate - scan a limited neighborhood around gate center
        else {
            LOGGER.warn("Gate has unusual dimensions - scanning neighborhood to locate slides");

            int centerX = (bounds.minX() + bounds.maxX()) / 2;
            int centerZ = (bounds.minZ() + bounds.maxZ()) / 2;

            if (sideDirection == Direction.NORTH || sideDirection == Direction.SOUTH) {
                int[] candidateZ = new int[]{bounds.minZ() - 1, bounds.maxZ() + 1, bounds.minZ(), bounds.maxZ()};
                int startX = centerX - SLIDE_COLUMN_SEARCH_RADIUS;
                int endX = centerX + SLIDE_COLUMN_SEARCH_RADIUS;
                LOGGER.info("  Scanning X={}..{} at Z candidates {} for slide columns (side={})", startX, endX, Arrays.toString(candidateZ), sideDirection);

                for (int z : candidateZ) {
                    for (int x = startX; x <= endX; x++) {
                        findSlideBlocksInColumn(level, new BlockPos(x, bounds.maxY(), z), slides);
                    }
                    // Frühes Abbrechen wenn Slides gefunden wurden
                    if (!slides.isEmpty()) break;
                }
            } else {
                int[] candidateX = new int[]{bounds.minX() - 1, bounds.maxX() + 1, bounds.minX(), bounds.maxX()};
                int startZ = centerZ - SLIDE_COLUMN_SEARCH_RADIUS;
                int endZ = centerZ + SLIDE_COLUMN_SEARCH_RADIUS;
                LOGGER.info("  Scanning Z={}..{} at X candidates {} for slide columns (side={})", startZ, endZ, Arrays.toString(candidateX), sideDirection);

                for (int x : candidateX) {
                    for (int z = startZ; z <= endZ; z++) {
                        findSlideBlocksInColumn(level, new BlockPos(x, bounds.maxY(), z), slides);
                    }
                    // Frühes Abbrechen wenn Slides gefunden wurden
                    if (!slides.isEmpty()) break;
                }
            }
        }

        return slides;
    }

    /**
     * Helper method to find all slide/control blocks in a vertical column at a given X/Z position.
     * OPTIMIZED: Only scans until FACING=UP (lower boundary) or FACING=DOWN (upper boundary) markers are found.
     */
    private static void findSlideBlocksInColumn(Level level, BlockPos startPos, List<BlockPos> results) {
        if (level == null || startPos == null || results == null) {
            LOGGER.error("Invalid parameters in findSlideBlocksInColumn");
            return;
        }

        int x = startPos.getX();
        int z = startPos.getZ();

        // Prüfe ob diese Spalte bereits gescannt wurde (Duplikatsvermeidung)
        boolean alreadyScanned = results.stream()
                .anyMatch(pos -> pos.getX() == x && pos.getZ() == z);

        if (alreadyScanned) {
            LOGGER.debug("Column at X={}, Z={} already scanned - skipping", x, z);
            return;
        }

        LOGGER.info("Scanning column at X={}, Z={}, starting Y={}", x, z, startPos.getY());

        // PROBLEM 5 FIX: Scanne nur mit begrenztem Abstand vom Startpunkt
        int minY = Math.max(level.getMinY(), startPos.getY() - SLIDE_COLUMN_EXTENSION);
        int maxY = Math.min(level.getMaxY(), startPos.getY() + SLIDE_COLUMN_EXTENSION);

        LOGGER.info("  Initial scan range: Y={} to Y={}", minY, maxY);

        List<BlockPos> foundInColumn = new ArrayList<>();
        BlockPos lowerBoundary = null;
        BlockPos upperBoundary = null;

        // Scan downward from start to find lower boundary and all blocks
        for (int y = startPos.getY(); y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof GateSlideBlock) {
                foundInColumn.add(pos.immutable());

                // Check if this is the lower boundary (FACING=UP)
                if (GateSlideBlock.isLowerBoundary(state)) {
                    lowerBoundary = pos;
                    LOGGER.info("  Found LOWER boundary at Y={}", y);
                    break; // Stop scanning down
                }
            } else if (state.getBlock() instanceof GateControlBlock) {
                foundInColumn.add(pos.immutable());
            } else {
                // Hit a non-slide/control block, stop scanning down
                LOGGER.debug("  Hit non-slide block at Y={}, stopping downward scan", y);
                break;
            }
        }

        // Scan upward from start to find upper boundary and all blocks
        for (int y = startPos.getY() + 1; y <= maxY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof GateSlideBlock) {
                foundInColumn.add(pos.immutable());

                // Check if this is the upper boundary (FACING=DOWN)
                if (GateSlideBlock.isUpperBoundary(state)) {
                    upperBoundary = pos;
                    LOGGER.info("  Found UPPER boundary at Y={}", y);
                    break; // Stop scanning up
                }
            } else if (state.getBlock() instanceof GateControlBlock) {
                foundInColumn.add(pos.immutable());
            } else {
                // Hit a non-slide/control block, stop scanning up
                LOGGER.debug("  Hit non-slide block at Y={}, stopping upward scan", y);
                break;
            }
        }

        // Add all found blocks to results
        for (BlockPos pos : foundInColumn) {
            if (!results.contains(pos)) {
                results.add(pos);
            }
        }

        LOGGER.info("  Column scan complete: found {} slide/control blocks at X={}, Z={} (boundaries: lower={}, upper={})",
                   foundInColumn.size(), x, z,
                   lowerBoundary != null ? "Y=" + lowerBoundary.getY() : "none",
                   upperBoundary != null ? "Y=" + upperBoundary.getY() : "none");
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
        if (level == null || gateBlocks == null || gateBlocks.isEmpty()) {
            LOGGER.warn("Cannot check gate open state - invalid parameters");
            return false;
        }

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

        if (totalCount == 0) {
            LOGGER.warn("No valid gate blocks found when checking open state");
            return false;
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
     * PROBLEM 4 FIX: Simplified and more robust approach
     * - Blocks with OPEN=false are already at closed position (keep as-is)
     * - Blocks with OPEN=true need to be normalized down to closed position
     * - Uses consistent Y-level calculation to avoid irregular gate issues
     */
    private static Set<BlockPos> calculateClosedPositions(Level level, Set<BlockPos> openGateBlocks) {
        if (openGateBlocks == null || openGateBlocks.isEmpty()) {
            return openGateBlocks != null ? openGateBlocks : Collections.emptySet();
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

        // Determine the closed Y level
        Integer closedMinY = null;

        // Strategy 1: Use already-closed blocks as reference
        if (!alreadyClosedBlocks.isEmpty()) {
            closedMinY = alreadyClosedBlocks.stream()
                    .mapToInt(BlockPos::getY)
                    .min()
                    .orElse(0);
            LOGGER.info("Using already-closed blocks to determine closed level: Y={}", closedMinY);
        }

        // Strategy 2: Find lower boundary from slide column
        if (closedMinY == null) {
            closedMinY = findClosedLevelFromSlideColumn(level, openBlocks);
            if (closedMinY != null) {
                LOGGER.info("Determined closed level from slide column boundary: Y={}", closedMinY);
            }
        }

        // Strategy 3: Fallback - use minimum Y of open blocks as closed position
        // This is a last resort when we can't find proper boundaries
        if (closedMinY == null) {
            LOGGER.warn("Could not determine closed position from boundaries - using minimum Y of open blocks");

            closedMinY = openBlocks.stream()
                    .mapToInt(BlockPos::getY)
                    .min()
                    .orElse(0);

            LOGGER.info("Using fallback closed level: Y={}", closedMinY);
        }

        // Now normalize all open blocks to closed positions
        int openMinY = openBlocks.stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElse(closedMinY);

        int yOffset = openMinY - closedMinY;
        LOGGER.info("Normalizing open blocks: currentMinY={}, closedMinY={}, yOffset={}",
                   openMinY, closedMinY, yOffset);

        // Sanity check: offset should be non-negative (blocks are moving down or staying)
        if (yOffset < 0) {
            LOGGER.error("Invalid offset calculated: {} (should be >= 0) - using current positions", yOffset);
            return openGateBlocks;
        }

        // Build the final set of closed positions
        Set<BlockPos> closedPositions = new HashSet<>(alreadyClosedBlocks);

        // Normalize open blocks by moving them down by the offset
        for (BlockPos openPos : openBlocks) {
            BlockPos closedPos = openPos.below(yOffset);
            closedPositions.add(closedPos);
            LOGGER.debug("Normalizing open block: {} -> {} (offset={})", openPos, closedPos, yOffset);
        }

        LOGGER.info("Final result: {} closed positions ({} kept, {} normalized)",
                   closedPositions.size(), alreadyClosedBlocks.size(), openBlocks.size());

        return closedPositions;
    }

    /**
     * PROBLEM 4 FIX: Helper method to find the closed Y level from the slide column.
     * Extracted for clarity and reusability.
     */
    private static Integer findClosedLevelFromSlideColumn(Level level, Set<BlockPos> openBlocks) {
        if (openBlocks.isEmpty()) {
            return null;
        }

        // Find ANY gate block to use as reference
        BlockPos referenceGatePos = openBlocks.iterator().next();
        BlockState referenceState = level.getBlockState(referenceGatePos);

        if (!(referenceState.getBlock() instanceof GateBlock)) {
            return null;
        }

        Direction gateFacing = referenceState.getValue(GateBlock.FACING);

        // CRITICAL FIX: We need to search for the slide column based on the GATE BOUNDS,
        // not just the reference block position. When the gate is open, the blocks
        // are at a different Y level, so we need to look at all X/Z positions.

        // Calculate bounds of open blocks to find slide column location
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : openBlocks) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // Determine slide column locations based on gate extension direction
        // The key insight: slides are perpendicular to the gate's WIDTH direction, not its FACING
        boolean gateExtendsInX = (maxX - minX) > 0;
        boolean gateExtendsInZ = (maxZ - minZ) > 0;

        int slideCheckX, slideCheckZ;

        if (gateExtendsInZ && !gateExtendsInX) {
            // Gate extends in Z direction (e.g., Z=26 to Z=31 at X=-26)
            // Slides are perpendicular to Z extension, so at Z=minZ-1 or Z=maxZ+1
            slideCheckX = minX;
            slideCheckZ = minZ - 1; // Try one side first

            LOGGER.info("Gate extends in Z (minZ={}, maxZ={}), checking slide at X={}, Z={}",
                       minZ, maxZ, slideCheckX, slideCheckZ);
        } else if (gateExtendsInX && !gateExtendsInZ) {
            // Gate extends in X direction
            // Slides are perpendicular to X extension, so at X=minX-1 or X=maxX+1
            slideCheckX = minX - 1; // Try one side first
            slideCheckZ = minZ;

            LOGGER.info("Gate extends in X (minX={}, maxX={}), checking slide at X={}, Z={}",
                       minX, maxX, slideCheckX, slideCheckZ);
        } else {
            // Single block gate or unusual configuration - use facing as fallback
            LOGGER.warn("Gate has unusual dimensions (X span: {}, Z span: {}), using facing-based logic",
                       maxX - minX, maxZ - minZ);

            if (gateFacing == Direction.NORTH || gateFacing == Direction.SOUTH) {
                slideCheckX = minX;
                slideCheckZ = minZ - 1;
            } else {
                slideCheckX = minX - 1;
                slideCheckZ = minZ;
            }
        }

        // Try to find slide column at the calculated position
        BlockPos slideColumnPos = new BlockPos(slideCheckX, referenceGatePos.getY(), slideCheckZ);
        BlockState checkState = level.getBlockState(slideColumnPos);

        if (!(checkState.getBlock() instanceof GateSlideBlock || checkState.getBlock() instanceof GateControlBlock)) {
            // Try the other side based on gate extension direction
            if (gateExtendsInZ && !gateExtendsInX) {
                slideCheckZ = maxZ + 1; // Try the opposite side
                LOGGER.info("First side not found, trying opposite at X={}, Z={}", slideCheckX, slideCheckZ);
            } else if (gateExtendsInX && !gateExtendsInZ) {
                slideCheckX = maxX + 1; // Try the opposite side
                LOGGER.info("First side not found, trying opposite at X={}, Z={}", slideCheckX, slideCheckZ);
            } else {
                // Unusual configuration fallback
                if (gateFacing == Direction.NORTH || gateFacing == Direction.SOUTH) {
                    slideCheckZ = maxZ + 1;
                } else {
                    slideCheckX = maxX + 1;
                }
            }

            slideColumnPos = new BlockPos(slideCheckX, referenceGatePos.getY(), slideCheckZ);
            checkState = level.getBlockState(slideColumnPos);

            if (!(checkState.getBlock() instanceof GateSlideBlock || checkState.getBlock() instanceof GateControlBlock)) {
                LOGGER.warn("Could not find slide column adjacent to gate at X={}, Z={}", slideCheckX, slideCheckZ);
                return null;
            }
        }

        LOGGER.info("Found slide column at {} for closed level detection", slideColumnPos);

        // Find the lower boundary in the slide column
        int openMinY = openBlocks.stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElse(referenceGatePos.getY());

        BlockPos lowerBoundary = findLowerBoundaryInColumn(level, slideColumnPos, openMinY);
        if (lowerBoundary == null) {
            LOGGER.warn("Could not find lower boundary in slide column");
            return null;
        }

        // Closed position starts one block above the lower boundary
        return lowerBoundary.getY() + 1;
    }

    /**
     * Finds the lower boundary marker in a slide column by scanning downward from a starting position.
     * Used when normalizing open gate positions to closed positions.
     */
    private static BlockPos findLowerBoundaryInColumn(Level level, BlockPos startPos, int minY) {
        if (level == null || startPos == null) {
            LOGGER.error("Invalid parameters in findLowerBoundaryInColumn");
            return null;
        }

        // Scan down from startPos to find a lower boundary marker (FACING=UP)
        int searchLimit = Math.max(level.getMinY(), minY - LOWER_BOUNDARY_SEARCH_RANGE);

        for (int y = startPos.getY(); y >= searchLimit; y--) {
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

        LOGGER.warn("Could not find lower boundary in column at X={}, Z={} after searching down to Y={}",
                   startPos.getX(), startPos.getZ(), searchLimit);
        return null;
    }
}
