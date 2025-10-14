package de.davidvogt.hkbmod.block.custom;

import com.mojang.serialization.MapCodec;
import de.davidvogt.hkbmod.block.entity.DragonNestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Dragon Nest Block - A special block that serves as home for dragons.
 * This block spawns and maintains 1-2 dragons that consider it their nest.
 * Dragons will return to this location periodically.
 */
public class DragonNestBlock extends BaseEntityBlock {
    public static final MapCodec<DragonNestBlock> CODEC = simpleCodec(DragonNestBlock::new);

    public DragonNestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DragonNestBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // Only tick on server side
        return level.isClientSide() ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof DragonNestBlockEntity dragonNestBlockEntity) {
                DragonNestBlockEntity.tick((ServerLevel) level1, pos, state1, dragonNestBlockEntity);
            }
        };
    }
}
