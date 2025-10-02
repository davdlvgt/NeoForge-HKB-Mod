package de.davidvogt.hkbmod.util;

import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Blocks {
        // public static final TagKey<Block> NEEDS_BISMUTH_TOOL = createTag("needs_bismuth_tool");
        // public static final TagKey<Block> INCORRECT_FOR_BISMUTH_TOOL = createTag("incorrect_for_bismuth_tool");

        public static final TagKey<Block> INCORRECT_FOR_EMERALD_TOOL =
                TagKey.create(
                        BuiltInRegistries.BLOCK.key(),
                        ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "emerald_tool")
                );

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, name));
        }

    }

    public static class Items {
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items");
        // public static final TagKey<Item> BISMUTH_REPAIRABLE = createTag("bismuth_repairable");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, name));
        }
    }
}