package de.davidvogt.hkbmod.gate;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class that stores information about a detected gate structure.
 * Contains all positions and calculated values needed for gate operation.
 */
public class GateStructure {
    private final List<BlockPos> gateBlocks;
    private final List<BlockPos> leftSlideBlocks;
    private final List<BlockPos> rightSlideBlocks;
    private final BlockPos controlBlockPos;
    private final int maxTravel;
    private final int gateWidth;
    private final int gateHeight;
    private final BlockPos lowerBoundary;
    private final BlockPos upperBoundary;

    public GateStructure(List<BlockPos> gateBlocks,
                         List<BlockPos> leftSlideBlocks,
                         List<BlockPos> rightSlideBlocks,
                         BlockPos controlBlockPos,
                         int maxTravel,
                         int gateWidth,
                         int gateHeight,
                         BlockPos lowerBoundary,
                         BlockPos upperBoundary) {
        this.gateBlocks = new ArrayList<>(gateBlocks);
        this.leftSlideBlocks = new ArrayList<>(leftSlideBlocks);
        this.rightSlideBlocks = new ArrayList<>(rightSlideBlocks);
        this.controlBlockPos = controlBlockPos;
        this.maxTravel = maxTravel;
        this.gateWidth = gateWidth;
        this.gateHeight = gateHeight;
        this.lowerBoundary = lowerBoundary;
        this.upperBoundary = upperBoundary;
    }

    public List<BlockPos> getGateBlocks() {
        return new ArrayList<>(gateBlocks);
    }

    public List<BlockPos> getLeftSlideBlocks() {
        return new ArrayList<>(leftSlideBlocks);
    }

    public List<BlockPos> getRightSlideBlocks() {
        return new ArrayList<>(rightSlideBlocks);
    }

    public BlockPos getControlBlockPos() {
        return controlBlockPos;
    }

    public int getMaxTravel() {
        return maxTravel;
    }

    public int getGateWidth() {
        return gateWidth;
    }

    public int getGateHeight() {
        return gateHeight;
    }

    public BlockPos getLowerBoundary() {
        return lowerBoundary;
    }

    public BlockPos getUpperBoundary() {
        return upperBoundary;
    }

    /**
     * Checks if this is a valid gate structure
     */
    public boolean isValid() {
        return !gateBlocks.isEmpty()
                && !leftSlideBlocks.isEmpty()
                && !rightSlideBlocks.isEmpty()
                && maxTravel > 0
                && lowerBoundary != null
                && upperBoundary != null;
    }

    @Override
    public String toString() {
        return String.format("GateStructure{gates=%d, leftSlides=%d, rightSlides=%d, maxTravel=%d, size=%dx%d, bounds=Y%d to Y%d}",
                gateBlocks.size(), leftSlideBlocks.size(), rightSlideBlocks.size(),
                maxTravel, gateWidth, gateHeight,
                lowerBoundary != null ? lowerBoundary.getY() : -1,
                upperBoundary != null ? upperBoundary.getY() : -1);
    }
}
