package de.davidvogt.hkbmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CustomDragonEggBlock extends Block {
    // Definiere die Shape basierend auf den Modellabmessungen
    // Das Modell geht ungefähr von (5.88, 0, 6) bis (10.45, 5, 11)
    // Gerundet für bessere Hitbox: von (5.5, 0, 5.5) bis (10.5, 5, 10.5)
    private static final VoxelShape SHAPE = Block.box(5.5, 0, 5.5, 10.5, 5, 10.5);

    public CustomDragonEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

