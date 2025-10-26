package de.davidvogt.hkbmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Gate Block - The actual gate material that moves up and down.
 * Can be arranged in any rectangular shape.
 * Has an "open" state that determines its position.
 * Thin block (2 pixels thick) like an iron bar gate.
 * Rotates to align with adjacent gate_slide_blocks.
 */
public class GateBlock extends Block {
    public static final MapCodec<GateBlock> CODEC = simpleCodec(GateBlock::new);

    // Blockstate property: whether the gate is in open (raised) position
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    // Facing direction (aligned with gate_slide_blocks)
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Thin gate shapes for different orientations
    private static final VoxelShape SHAPE_NORTH_SOUTH = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
    private static final VoxelShape SHAPE_EAST_WEST = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);

    public GateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OPEN, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Check for adjacent gate_slide_blocks to determine facing
        Direction facing = determineFacingFromNeighbors(level, pos);

        // If no gate_slide_block found, use player's facing direction
        if (facing == null) {
            facing = context.getHorizontalDirection();
        }

        return this.defaultBlockState()
                .setValue(OPEN, false)
                .setValue(FACING, facing);
    }

    /**
     * Determines the facing direction by checking for adjacent gate_slide_blocks.
     * The gate should face perpendicular to the slide blocks (so it can slide between them).
     */
    private Direction determineFacingFromNeighbors(Level level, BlockPos pos) {
        // Check horizontal neighbors for gate_slide_blocks
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (neighbor.getBlock() instanceof GateSlideBlock) {
                // If slide block is to the EAST, gate should face NORTH/SOUTH
                // If slide block is to the NORTH, gate should face EAST/WEST
                if (dir == Direction.EAST || dir == Direction.WEST) {
                    return Direction.NORTH; // Gate runs north-south
                } else {
                    return Direction.EAST; // Gate runs east-west
                }
            }
        }
        return null;
    }

    /**
     * Gate blocks are thin - shape depends on facing direction
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        // If facing NORTH/SOUTH, gate runs along X-axis (thin on Z)
        // If facing EAST/WEST, gate runs along Z-axis (thin on X)
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? SHAPE_NORTH_SOUTH : SHAPE_EAST_WEST;
    }

    /**
     * Not a full block - players can see through the sides
     */
    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }
}
