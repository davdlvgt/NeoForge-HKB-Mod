package de.davidvogt.hkbmod.block.custom;

import com.mojang.serialization.MapCodec;
import de.davidvogt.hkbmod.block.entity.DragonNestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
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
 *
 * Can only be mined with a Pickaxe enchanted with Silk Touch.
 * Completely immune to explosions.
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

    /**
     * Override to make the nest only harvestable with Silk Touch pickaxe
     */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack tool = player.getMainHandItem();

        // Check if player has a pickaxe with Silk Touch
        if (tool.isCorrectToolForDrops(state)) {
            // Simple check: if the tool has any Silk Touch level
            if (tool.getEnchantmentLevel(player.level().holderOrThrow(Enchantments.SILK_TOUCH)) > 0) {
                // Allow normal mining speed if has Silk Touch
                return super.getDestroyProgress(state, player, level, pos);
            }
        }

        // Without Silk Touch pickaxe, block is unbreakable
        return 0.0F;
    }

    /**
     * Make the nest completely immune to explosions - never drop items
     */
    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }

    /**
     * Prevent the block from being destroyed by explosions
     * By overriding and returning false, we prevent explosion damage
     */
    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}
