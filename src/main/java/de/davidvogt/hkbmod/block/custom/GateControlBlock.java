package de.davidvogt.hkbmod.block.custom;

import com.mojang.serialization.MapCodec;
import de.davidvogt.hkbmod.block.entity.GateControlBlockEntity;
import de.davidvogt.hkbmod.block.entity.ModBlockEntities;
import de.davidvogt.hkbmod.gate.GateDetector;
import de.davidvogt.hkbmod.gate.GateStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gate Control Block - Special slide block that controls the gate.
 * Can be placed anywhere in the slide column.
 * Right-click to toggle gate open/closed.
 */
public class GateControlBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateControlBlock.class);
    public static final MapCodec<GateControlBlock> CODEC = simpleCodec(GateControlBlock::new);

    // Visual indicator when gate is active/moving
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    public GateControlBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(POWERED, FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GateControlBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.GATE_CONTROL_BE.get(), GateControlBlockEntity::tick);
    }

    /**
     * Handle right-click interaction to toggle gate
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        // Get the block entity
        if (!(level.getBlockEntity(pos) instanceof GateControlBlockEntity controlBlockEntity)) {
            LOGGER.warn("Gate control block at {} has no block entity!", pos);
            return InteractionResult.FAIL;
        }

        // Check if gate is currently moving
        if (controlBlockEntity.isMoving()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Gate is already moving!"), true);
            return InteractionResult.FAIL;
        }

        // Toggle the gate
        toggleGate(serverLevel, pos, state, controlBlockEntity, null);

        return InteractionResult.SUCCESS;
    }

    /**
     * Handle redstone signal changes - triggers gate toggle on rising edge (power on)
     * This is called by Forge/NeoForge
     */
    @Override
    public void onNeighborChange(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos, BlockPos neighbor) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Get the block entity
        if (!(serverLevel.getBlockEntity(pos) instanceof GateControlBlockEntity controlBlockEntity)) {
            return;
        }

        // Re-detect structure when neighbors change (in case gate blocks were added/removed)
        redetectStructureIfNeeded(serverLevel, pos, controlBlockEntity);

        // Check if gate is currently moving
        if (controlBlockEntity.isMoving()) {
            return;
        }

        // Check current redstone power state
        boolean isPowered = serverLevel.hasNeighborSignal(pos);
        boolean wasPowered = controlBlockEntity.wasPowered();

        // Only trigger on RISING EDGE (transition from unpowered to powered)
        if (isPowered && !wasPowered) {
            LOGGER.info("Redstone rising edge detected (onNeighborChange) - toggling gate at {}", pos);
            toggleGate(serverLevel, pos, state, controlBlockEntity, null);
        }

        // Update the stored power state
        controlBlockEntity.setPowered(isPowered);
    }

    /**
     * Handle Vanilla redstone updates
     * This is called by Vanilla Minecraft for redstone changes
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);

        if (level.isClientSide) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Get the block entity
        if (!(serverLevel.getBlockEntity(pos) instanceof GateControlBlockEntity controlBlockEntity)) {
            return;
        }

        // Re-detect structure when neighbors change (in case gate blocks were added/removed)
        redetectStructureIfNeeded(serverLevel, pos, controlBlockEntity);

        // Check if gate is currently moving
        if (controlBlockEntity.isMoving()) {
            return;
        }

        // Check current redstone power state
        boolean isPowered = serverLevel.hasNeighborSignal(pos);
        boolean wasPowered = controlBlockEntity.wasPowered();

        // Only trigger on RISING EDGE (transition from unpowered to powered)
        if (isPowered && !wasPowered) {
            LOGGER.info("Redstone rising edge detected (neighborChanged) - toggling gate at {}", pos);
            toggleGate(serverLevel, pos, state, controlBlockEntity, null);
        }

        // Update the stored power state
        controlBlockEntity.setPowered(isPowered);
    }

    /**
     * Re-detect the gate structure when neighbors change.
     * This ensures that changes to the gate (adding/removing blocks) are recognized.
     */
    private void redetectStructureIfNeeded(ServerLevel level, BlockPos pos, GateControlBlockEntity controlBlockEntity) {
        // Don't re-detect while gate is moving
        if (controlBlockEntity.isMoving()) {
            return;
        }

        // Always re-detect structure to catch any changes
        LOGGER.debug("Re-detecting gate structure at {} due to neighbor change", pos);
        GateStructure newStructure = GateDetector.detectGateStructure(level, pos);

        if (newStructure != null) {
            GateStructure oldStructure = controlBlockEntity.getStructure();

            // Check if structure changed
            if (oldStructure == null || !structuresEqual(oldStructure, newStructure)) {
                LOGGER.info("Gate structure changed at {}: {} gate blocks, max travel: {}",
                        pos, newStructure.getGateBlocks().size(), newStructure.getMaxTravel());
                controlBlockEntity.setStructure(newStructure);
            }
        } else if (controlBlockEntity.getStructure() != null) {
            // Structure was removed
            LOGGER.info("Gate structure removed at {}", pos);
            controlBlockEntity.setStructure(null);
        }
    }

    /**
     * Check if two gate structures are equal (same blocks and dimensions)
     */
    private boolean structuresEqual(GateStructure s1, GateStructure s2) {
        if (s1.getGateBlocks().size() != s2.getGateBlocks().size()) {
            return false;
        }
        if (s1.getMaxTravel() != s2.getMaxTravel()) {
            return false;
        }
        // Structures are considered equal if they have the same size and max travel
        // (detailed block comparison would be too expensive)
        return true;
    }

    /**
     * Common method to toggle the gate (used by both manual click and redstone)
     */
    private void toggleGate(ServerLevel level, BlockPos pos, BlockState state, GateControlBlockEntity controlBlockEntity, @Nullable Player player) {
        // DEBUGGING: First, scan the entire area and log everything
        LOGGER.info("============================================");
        LOGGER.info("COMPLETE GATE AREA SCAN");
        LOGGER.info("Control Block at: {}", pos);
        LOGGER.info("Control Block facing: {}", state.getValue(FACING));
        LOGGER.info("============================================");

        scanAndLogArea(level, pos, player);

        LOGGER.info("============================================");
        LOGGER.info("NOW ATTEMPTING NORMAL GATE DETECTION");
        LOGGER.info("============================================");

        // Get current state
        boolean isOpen = controlBlockEntity.isOpen();
        GateStructure structure = controlBlockEntity.getStructure();

        // Only detect structure if not already cached (first use or after neighbor change cleared it)
        if (structure == null) {
            LOGGER.info("No cached structure - detecting gate structure from control block at {}", pos);
            structure = GateDetector.detectGateStructure(level, pos);

            if (structure == null) {
                LOGGER.warn("No valid gate structure found at {}", pos);
                if (player != null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("No valid gate structure found!"), true);
                }
                return;
            }

            LOGGER.info("Gate structure detected: {} gate blocks, max travel: {}", structure.getGateBlocks().size(), structure.getMaxTravel());
            controlBlockEntity.setStructure(structure);
        } else {
            LOGGER.info("Using cached structure: {} gate blocks, max travel: {}", structure.getGateBlocks().size(), structure.getMaxTravel());
        }

        // Toggle gate state
        controlBlockEntity.setOpen(!isOpen);
        controlBlockEntity.startMoving();

        // Update visual state
        level.setBlock(pos, state.setValue(POWERED, true), 3);

        // Play sound
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * Scans a large area around the control block and logs ALL gate-related blocks found.
     * This is for debugging purposes to see the actual structure built by the player.
     */
    private void scanAndLogArea(ServerLevel level, BlockPos controlPos, @Nullable Player player) {
        int scanRadius = 10; // Scan 10 blocks in each direction

        LOGGER.info("Scanning area from {} to {}",
                controlPos.offset(-scanRadius, -scanRadius, -scanRadius),
                controlPos.offset(scanRadius, scanRadius, scanRadius));

        List<BlockInfo> gateBlocks = new ArrayList<>();
        List<BlockInfo> slideBlocks = new ArrayList<>();
        List<BlockInfo> controlBlocks = new ArrayList<>();

        // Scan the area
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos checkPos = controlPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.getBlock() instanceof GateBlock) {
                        Direction facing = state.getValue(GateBlock.FACING);
                        gateBlocks.add(new BlockInfo(checkPos, "GateBlock", facing, null));
                    } else if (state.getBlock() instanceof GateSlideBlock) {
                        Direction facing = state.getValue(GateSlideBlock.FACING);
                        Direction rotation = state.getValue(GateSlideBlock.ROTATION);
                        String boundaryType = GateSlideBlock.isLowerBoundary(state) ? "LOWER_BOUNDARY" :
                                GateSlideBlock.isUpperBoundary(state) ? "UPPER_BOUNDARY" : "NORMAL";
                        slideBlocks.add(new BlockInfo(checkPos, "GateSlideBlock", facing, rotation, boundaryType));
                    } else if (state.getBlock() instanceof GateControlBlock) {
                        Direction facing = state.getValue(GateControlBlock.FACING);
                        controlBlocks.add(new BlockInfo(checkPos, "GateControlBlock", facing, null));
                    }
                }
            }
        }

        // Log summary
        LOGGER.info("SCAN RESULTS:");
        LOGGER.info("  Found {} Gate blocks", gateBlocks.size());
        LOGGER.info("  Found {} Slide blocks", slideBlocks.size());
        LOGGER.info("  Found {} Control blocks", controlBlocks.size());
        LOGGER.info("");

        // Log all Gate blocks
        if (!gateBlocks.isEmpty()) {
            LOGGER.info("GATE BLOCKS:");
            gateBlocks.sort(Comparator.<BlockInfo>comparingInt(b -> b.pos.getY())
                    .thenComparingInt(b -> b.pos.getZ())
                    .thenComparingInt(b -> b.pos.getX()));
            for (BlockInfo info : gateBlocks) {
                LOGGER.info("  {} | FACING={}", info.pos, info.facing);
            }
            LOGGER.info("");
        }

        // Log all Slide blocks
        if (!slideBlocks.isEmpty()) {
            LOGGER.info("SLIDE BLOCKS:");
            slideBlocks.sort(Comparator.<BlockInfo>comparingInt(b -> b.pos.getY())
                    .thenComparingInt(b -> b.pos.getZ())
                    .thenComparingInt(b -> b.pos.getX()));
            for (BlockInfo info : slideBlocks) {
                LOGGER.info("  {} | FACING={}, ROTATION={}, TYPE={}",
                        info.pos, info.facing, info.rotation, info.extra);
            }
            LOGGER.info("");
        }

        // Log all Control blocks
        if (!controlBlocks.isEmpty()) {
            LOGGER.info("CONTROL BLOCKS:");
            for (BlockInfo info : controlBlocks) {
                LOGGER.info("  {} | FACING={}", info.pos, info.facing);
            }
            LOGGER.info("");
        }

        // Calculate and log gate dimensions
        if (!gateBlocks.isEmpty()) {
            int minX = gateBlocks.stream().mapToInt(b -> b.pos.getX()).min().orElse(0);
            int maxX = gateBlocks.stream().mapToInt(b -> b.pos.getX()).max().orElse(0);
            int minY = gateBlocks.stream().mapToInt(b -> b.pos.getY()).min().orElse(0);
            int maxY = gateBlocks.stream().mapToInt(b -> b.pos.getY()).max().orElse(0);
            int minZ = gateBlocks.stream().mapToInt(b -> b.pos.getZ()).min().orElse(0);
            int maxZ = gateBlocks.stream().mapToInt(b -> b.pos.getZ()).max().orElse(0);

            LOGGER.info("GATE DIMENSIONS:");
            LOGGER.info("  X range: {} to {} (width: {})", minX, maxX, maxX - minX + 1);
            LOGGER.info("  Y range: {} to {} (height: {})", minY, maxY, maxY - minY + 1);
            LOGGER.info("  Z range: {} to {} (depth: {})", minZ, maxZ, maxZ - minZ + 1);
            LOGGER.info("");
        }

        // Calculate and log slide positions relative to gates
        if (!gateBlocks.isEmpty() && !slideBlocks.isEmpty()) {
            int gateMinX = gateBlocks.stream().mapToInt(b -> b.pos.getX()).min().orElse(0);
            int gateMaxX = gateBlocks.stream().mapToInt(b -> b.pos.getX()).max().orElse(0);
            int gateMinZ = gateBlocks.stream().mapToInt(b -> b.pos.getZ()).min().orElse(0);
            int gateMaxZ = gateBlocks.stream().mapToInt(b -> b.pos.getZ()).max().orElse(0);

            LOGGER.info("SLIDE POSITIONS RELATIVE TO GATE:");
            for (BlockInfo slide : slideBlocks) {
                String position = "";
                if (slide.pos.getX() < gateMinX) position += "WEST of gate, ";
                else if (slide.pos.getX() > gateMaxX) position += "EAST of gate, ";
                else position += "WITHIN gate X, ";

                if (slide.pos.getZ() < gateMinZ) position += "NORTH of gate";
                else if (slide.pos.getZ() > gateMaxZ) position += "SOUTH of gate";
                else position += "WITHIN gate Z";

                LOGGER.info("  {} -> {}", slide.pos, position);
            }
            LOGGER.info("");
        }

        // Send message to player with summary
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    String.format("Scan complete: %d gate blocks, %d slide blocks, %d control blocks (see server log)",
                            gateBlocks.size(), slideBlocks.size(), controlBlocks.size())), false);
        }
    }

    /**
     * Helper class to store block information for scanning
     */
    private static class BlockInfo {
        final BlockPos pos;
        final String type;
        final Direction facing;
        final Direction rotation;
        final String extra;

        BlockInfo(BlockPos pos, String type, Direction facing, Direction rotation) {
            this(pos, type, facing, rotation, null);
        }

        BlockInfo(BlockPos pos, String type, Direction facing, Direction rotation, String extra) {
            this.pos = pos;
            this.type = type;
            this.facing = facing;
            this.rotation = rotation;
            this.extra = extra;
        }
    }

    /**
     * Called when the block is placed - sets the initial state based on the player's facing direction.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);

        if (state != null) {
            // Set the facing direction based on player placement
            Direction facing = context.getHorizontalDirection().getOpposite();
            state = state.setValue(FACING, facing);
        }

        return state;
    }
}
