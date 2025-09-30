package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

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

        // Block Models
        blockModels.createGenericCube(ModBlocks.TEST_BLOCK.get());
        blockModels.createGenericCube(ModBlocks.CUSTOM_TEST_BLOCK.get());
    }

    @SubscribeEvent // on the mod event bus
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
    }
}