package de.davidvogt.hkbmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Gate Slide Block - Rail/guide blocks that gate slides along.
 * Must be placed on left and right sides of the gate.
 * Can be rotated to mark upper or lower boundary:
 * - FACING = UP: This block is the LOWER boundary of the gate
 * - FACING = DOWN: This block is the UPPER boundary of the gate
 * - FACING = NORTH/SOUTH/EAST/WEST: Regular slide block (no boundary)
 * - ROTATION: Horizontal rotation for texture orientation (all modes)
 */
public class GateSlideBlock extends Block {
    public static final MapCodec<GateSlideBlock> CODEC = simpleCodec(GateSlideBlock::new);
    // Use FACING to support all 6 directions (including UP and DOWN)
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
    // Separate rotation property for horizontal texture orientation
    public static final EnumProperty<Direction> ROTATION = EnumProperty.create("rotation", Direction.class,
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    public GateSlideBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTATION, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Verwende die angeklickte Fläche für FACING (unterstützt UP und DOWN)
        Direction direction = context.getClickedFace();

        // Bestimme die Rotation basierend auf der Spielerrichtung
        Direction rotation = context.getHorizontalDirection();

        return this.defaultBlockState()
                .setValue(FACING, direction)
                .setValue(ROTATION, rotation);
    }

    /**
     * Check if this slide block is a lower boundary marker
     */
    public static boolean isLowerBoundary(BlockState state) {
        if (!(state.getBlock() instanceof GateSlideBlock)) {
            return false;
        }
        return state.getValue(FACING) == Direction.UP;
    }

    /**
     * Check if this slide block is an upper boundary marker
     */
    public static boolean isUpperBoundary(BlockState state) {
        if (!(state.getBlock() instanceof GateSlideBlock)) {
            return false;
        }
        return state.getValue(FACING) == Direction.DOWN;
    }
}
