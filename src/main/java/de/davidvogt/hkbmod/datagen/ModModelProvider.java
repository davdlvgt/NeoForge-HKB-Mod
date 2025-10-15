package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.block.custom.TestLampBlock;
import de.davidvogt.hkbmod.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.stream.Stream;

public class ModModelProvider extends ModelProvider {

    public ModModelProvider(PackOutput output) {
        super(output, HKBMod.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Item Models
        itemModels.generateFlatItem(ModItems.EMERALD_AXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.EMERALD_PICKAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.EMERALD_SWORD.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.EMERALD_SHOVEL.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.EMERALD_HOE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        itemModels.generateFlatItem(ModItems.MAGIC_PICKAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.TIME_SETTER.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        itemModels.generateFlatItem(ModItems.LONG_STICK.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.LONG_STRING.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LONGBOW_STICK.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        itemModels.createFlatItemModel(ModItems.LONGBOW.get(), ModelTemplates.BOW);
        itemModels.generateBow(ModItems.LONGBOW.get());

        itemModels.generateFlatItem(ModItems.DEER_BEEF.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.COOKED_DEER_BEEF.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DEER_ANTLERS.get(), ModelTemplates.FLAT_ITEM);

        itemModels.generateFlatItem(ModItems.DRAGON_SKIN.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAGON_MATERIAL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRAGON_SADDLE.asItem(), ModelTemplates.FLAT_ITEM);

        itemModels.generateFlatItem(ModItems.LONGBOW_ARROW.get(), ModelTemplates.FLAT_ITEM);

        // Block Models
        blockModels.createGenericCube(ModBlocks.TEST_BLOCK.get());
        blockModels.createGenericCube(ModBlocks.CUSTOM_TEST_BLOCK.get());

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(ModBlocks.TEST_LAMP.get())
                        .with(BlockModelGenerators.createBooleanModelDispatch(TestLampBlock.CLICKED,
                                BlockModelGenerators.plainVariant(blockModels.createSuffixedVariant(ModBlocks.TEST_LAMP.get(), "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube)),
                                BlockModelGenerators.plainVariant(TexturedModel.CUBE.create(ModBlocks.TEST_LAMP.get(), blockModels.modelOutput))))
        );


        blockModels.woodProvider(ModBlocks.ROBINIA_LOG.get()).logWithHorizontal(ModBlocks.ROBINIA_LOG.get()).wood(ModBlocks.ROBINIA_WOOD.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_ROBINIA_LOG.get()).logWithHorizontal(ModBlocks.STRIPPED_ROBINIA_LOG.get()).wood(ModBlocks.STRIPPED_ROBINIA_WOOD.get());

        blockModels.createTintedLeaves(ModBlocks.ROBINIA_LEAVES.get(), TexturedModel.LEAVES, -12012264);

        blockModels.createCrossBlock(ModBlocks.ROBINIA_SAPLING.get(), BlockModelGenerators.PlantType.TINTED);

        blockModels.family(ModBlocks.ROBINIA_PLANKS.get())
                .fence(ModBlocks.ROBINIA_FENCE.get())
                .fenceGate(ModBlocks.ROBINIA_FENCE_GATE.get())
                .stairs(ModBlocks.ROBINIA_STAIRS.get())
                .slab(ModBlocks.ROBINIA_SLAB.get())
                .button(ModBlocks.ROBINIA_BUTTON.get())
                .pressurePlate(ModBlocks.ROBINIA_PRESSURE_PLATE.get())
                .door(ModBlocks.ROBINIA_DOOR.get())
                .trapdoor(ModBlocks.ROBINIA_TRAPDOOR.get());

        blockModels.createTrivialCube(ModBlocks.ELASTIC_WOOD.get()); // One texture for all sides

        blockModels.createTrivialCube(ModBlocks.DRAGON_NEST.get());
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().filter(
                x -> !x.is(ModBlocks.RESEARCH_TABLE)
                        && !x.is(ModBlocks.RESEARCH_CRAFTING_TABLE)
                        && !x.is(ModBlocks.DRAGON_EGG));
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return ModItems.ITEMS.getEntries().stream();
    }
}