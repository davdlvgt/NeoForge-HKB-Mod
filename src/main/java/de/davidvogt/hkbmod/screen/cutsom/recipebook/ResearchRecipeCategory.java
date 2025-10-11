package de.davidvogt.hkbmod.screen.cutsom.recipebook;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;

/**
 * Categories for organizing research recipes in the recipe book.
 * Each category corresponds to vanilla CraftingBookCategory for filtering.
 */
public enum ResearchRecipeCategory {
    EQUIPMENT(CraftingBookCategory.EQUIPMENT, new ItemStack(Items.IRON_PICKAXE)),
    BUILDING(CraftingBookCategory.BUILDING, new ItemStack(Items.OAK_PLANKS)),
    MISC(CraftingBookCategory.MISC, new ItemStack(Items.CRAFTING_TABLE)),
    REDSTONE(CraftingBookCategory.REDSTONE, new ItemStack(Items.REDSTONE));

    private final CraftingBookCategory craftingCategory;
    private final ItemStack icon;

    ResearchRecipeCategory(CraftingBookCategory craftingCategory, ItemStack icon) {
        this.craftingCategory = craftingCategory;
        this.icon = icon;
    }

    /**
     * Gets the vanilla crafting category this corresponds to
     */
    public CraftingBookCategory getCraftingCategory() {
        return craftingCategory;
    }

    /**
     * Gets the icon item stack for this category
     */
    public ItemStack getIcon() {
        return icon;
    }

}
