package de.davidvogt.hkbmod.block;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.custom.CustomTestBlock;
import de.davidvogt.hkbmod.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(HKBMod.MODID);


    // Wenn ich cutsom block hab brauch ich das und nicht die Version darunter
    public static final DeferredBlock<Block> TEST_BLOCK = registerBlock(
            "test_block",
            (properties) -> new Block(properties
                    .strength(1.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST))
    );



    // CUSTOM BLOCKS
    public static final DeferredBlock<Block> CUSTOM_TEST_BLOCK = registerBlock("custom_test_block",
            (properties) -> new CustomTestBlock(properties.strength(2f).noLootTable()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
