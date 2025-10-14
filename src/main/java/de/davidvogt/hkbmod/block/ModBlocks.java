package de.davidvogt.hkbmod.block;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.custom.*;
import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.worldgen.tree.ModTreeGrowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
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

    public static final DeferredBlock<Block> TEST_LAMP = registerBlock("test_lamp",
            (properties) -> new TestLampBlock(properties.strength(2f).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(TestLampBlock.CLICKED) ? 15 : 0)));

    public static final DeferredBlock<Block> RESEARCH_TABLE = registerBlock("research_table",
            (properties) -> new ResearchTableBlock(properties.noOcclusion()));

    public static final DeferredBlock<Block> RESEARCH_CRAFTING_TABLE = registerBlock("research_crafting_table",
            (properties) -> new ResearchCraftingTableBlock(properties.noOcclusion().requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DRAGON_NEST = registerBlock("dragon_nest",
            (properties) -> new DragonNestBlock(properties
                    .strength(50.0f, 1200.0f)  // Härte 50 (Obsidian hat 50), Explosionswiderstand 1200 (Obsidian hat 1200, Bedrock hat 3.6M)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> ELASTIC_WOOD = registerBlock(
            "elastic_wood",
            Block::new
    );

    public static final DeferredBlock<Block> DRAGON_EGG = registerBlock(
            "dragon_egg",
            (properties) -> new CustomDragonEggBlock(properties.noOcclusion())
    );

    /*    public static final DeferredBlock<Block> ROBINIA_LOG = registerBlock("robinia_log",
                (properties) -> new ModFlammableRotatedPillarBlock(
                        BlockBehaviour
                                .Properties
                                .ofFullCopy(Blocks.OAK_LOG)
                ));*/
    /*
    public static final DeferredBlock<Block> ROBINIA_WOOD = registerBlock("robinia_wood",
            (properties) -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<Block> STRIPPED_ROBINIA_LOG = registerBlock("stripped_robinia_log",
            (properties) -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredBlock<Block> STRIPPED_ROBINIA_WOOD = registerBlock("stripped_robinia_wood",
            (properties) -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));
*/
    public static final DeferredBlock<Block> ROBINIA_LOG = registerBlock("robinia_log",
            (properties) -> new ModFlammableRotatedPillarBlock(
                    properties.instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava()));
    public static final DeferredBlock<Block> ROBINIA_WOOD = registerBlock("robinia_wood",
            (properties) -> new ModFlammableRotatedPillarBlock(
                    properties.instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava()));
    public static final DeferredBlock<Block> STRIPPED_ROBINIA_LOG = registerBlock("stripped_robinia_log",
            (properties) -> new ModFlammableRotatedPillarBlock(
                    properties.instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava()));
    public static final DeferredBlock<Block> STRIPPED_ROBINIA_WOOD = registerBlock("stripped_robinia_wood",
            (properties) -> new ModFlammableRotatedPillarBlock(
                    properties.instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava()));


    public static final DeferredBlock<Block> ROBINIA_PLANKS = registerBlock("robinia_planks",
            (properties) -> new Block(properties) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 20;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 5;
                }
            });

    public static final DeferredBlock<Block> ROBINIA_LEAVES = registerBlock("robinia_leaves",
            (properties) -> new UntintedParticleLeavesBlock(0.01f, ParticleTypes.CHERRY_LEAVES,
                    properties.mapColor(MapColor.PLANT).strength(0.2F).randomTicks().sound(SoundType.CHERRY_LEAVES)
                            .noOcclusion().isValidSpawn(Blocks::ocelotOrParrot).ignitedByLava().pushReaction(PushReaction.DESTROY)) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 60;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 30;
                }
            });

    public static final DeferredBlock<Block> ROBINIA_SAPLING = registerBlock("robinia_sapling",
            (properties) -> new SaplingBlock(ModTreeGrowers.ROBINIA,
                    properties.mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                            .sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<StairBlock> ROBINIA_STAIRS = registerBlock("robinia_stairs",
            (properties) -> new StairBlock(ModBlocks.ROBINIA_WOOD.get().defaultBlockState(),
                    properties.strength(2f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<SlabBlock> ROBINIA_SLAB = registerBlock("robinia_slab",
            (properties) -> new SlabBlock(properties.strength(2f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<PressurePlateBlock> ROBINIA_PRESSURE_PLATE = registerBlock("robinia_pressure_plate",
            (properties) -> new PressurePlateBlock(BlockSetType.OAK, properties.strength(2f).requiresCorrectToolForDrops())); // ToDo: BlockSetType.ROBINIA
    public static final DeferredBlock<ButtonBlock> ROBINIA_BUTTON = registerBlock("robinia_button",
            (properties) -> new ButtonBlock(BlockSetType.OAK, 20, properties.strength(2f).requiresCorrectToolForDrops().noCollission()));  // ToDo: BlockSetType.ROBINIA

    public static final DeferredBlock<FenceBlock> ROBINIA_FENCE = registerBlock("robinia_fence",
            (properties) -> new FenceBlock(properties.strength(2f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<FenceGateBlock> ROBINIA_FENCE_GATE = registerBlock("robinia_fence_gate",
            (properties) -> new FenceGateBlock(WoodType.OAK, properties.strength(2f).requiresCorrectToolForDrops())); // ToDo: WoodType.ROBINIA

    public static final DeferredBlock<DoorBlock> ROBINIA_DOOR = registerBlock("robinia_door",
            (properties) -> new DoorBlock(BlockSetType.OAK, properties.strength(2f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> ROBINIA_TRAPDOOR = registerBlock("robinia_trapdoor",
            (properties) -> new TrapDoorBlock(BlockSetType.OAK, properties.strength(2f).requiresCorrectToolForDrops().noOcclusion()));

    /*public static final DeferredBlock<Block> ROBINIA_PLANKS = registerBlock("robinia_planks",
            (properties) -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 20;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 5;
                }
            });
    public static final DeferredBlock<Block> ROBINIA_LEAVES = registerBlock("robinia_leaves",
            (properties) -> new UntintedParticleLeavesBlock(0.01f, ParticleTypes.CHERRY_LEAVES,
                    properties.mapColor(MapColor.PLANT).strength(0.2F).randomTicks().sound(SoundType.CHERRY_LEAVES)
                            .noOcclusion().isValidSpawn(Blocks::ocelotOrParrot).ignitedByLava().pushReaction(PushReaction.DESTROY)) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 60;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 30;
                }
            });

    public static final DeferredBlock<Block> ROBINIA_SAPLING = registerBlock("robinia_sapling",
            (properties) -> new SaplingBlock(ModTreeGrowers.ROBINIA, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
*/
/*
    public static final DeferredBlock<Block> ROBINIA_SAPLING = registerBlock("robinia_sapling",
            (properties) -> new ModSaplingBlock(ModTreeGrowers.ROBINIA,
                    properties.mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                            .sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY), () -> Blocks.NETHERRACK));
*/

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
