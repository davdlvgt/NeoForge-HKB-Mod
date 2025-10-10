package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, HKBMod.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(Items.COAL);

        this.tag(ItemTags.LOGS_THAT_BURN)
                .add(ModBlocks.ROBINIA_LOG.get().asItem())
                .add(ModBlocks.ROBINIA_WOOD.get().asItem())
                .add(ModBlocks.STRIPPED_ROBINIA_LOG.get().asItem())
                .add(ModBlocks.STRIPPED_ROBINIA_WOOD.get().asItem());

        this.tag(ItemTags.PLANKS)
                .add(ModBlocks.ROBINIA_PLANKS.get().asItem());

        this.tag(ModTags.Items.PLANKS)
                .add(ModBlocks.ROBINIA_PLANKS.get().asItem())
                .add(Blocks.ACACIA_PLANKS.asItem())
                .add(Blocks.BIRCH_PLANKS.asItem())
                .add(Blocks.BAMBOO_PLANKS.asItem())
                .add(Blocks.CHERRY_PLANKS.asItem())
                .add(Blocks.DARK_OAK_PLANKS.asItem())
                .add(Blocks.JUNGLE_PLANKS.asItem())
                .add(Blocks.MANGROVE_PLANKS.asItem())
                .add(Blocks.OAK_PLANKS.asItem())
                .add(Blocks.PALE_OAK_PLANKS.asItem())
                .add(Blocks.SPRUCE_PLANKS.asItem());

        this.tag(ModTags.Items.ROBINIA_LOG)
                .add(ModBlocks.ROBINIA_LOG.get().asItem())
                .add(ModBlocks.ROBINIA_WOOD.get().asItem())
                .add(ModBlocks.STRIPPED_ROBINIA_LOG.get().asItem())
                .add(ModBlocks.STRIPPED_ROBINIA_WOOD.get().asItem());
    }
}