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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

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
        itemModels.generateFlatItem(ModItems.TIME_SETTER.get(), ModelTemplates.FLAT_ITEM);

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

        blockModels.createTrivialCube(ModBlocks.ROBINIA_PLANKS.get());

        blockModels.createTintedLeaves(ModBlocks.ROBINIA_LEAVES.get(), TexturedModel.LEAVES, -12012264);

        blockModels.createCrossBlock(ModBlocks.ROBINIA_SAPLING.get(), BlockModelGenerators.PlantType.TINTED);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().filter(x -> !x.is(ModBlocks.RESEARCH_TABLE));
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return ModItems.ITEMS.getEntries().stream();
    }
}