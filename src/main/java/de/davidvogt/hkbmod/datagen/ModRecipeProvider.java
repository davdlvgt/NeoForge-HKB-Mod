package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
        super(provider, recipeOutput);
    }

    @Override
    protected void buildRecipes() {

        this.shaped(RecipeCategory.TOOLS, ModItems.EMERALD_AXE.get())
                .pattern(" EE")
                .pattern(" SE")
                .pattern(" S ")
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(this.output);

        this.shaped(RecipeCategory.TOOLS, ModItems.EMERALD_PICKAXE.get())
                .pattern("EEE")
                .pattern(" S ")
                .pattern(" S ")
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(this.output);

        this.shaped(RecipeCategory.COMBAT, ModItems.EMERALD_SWORD.get())
                .pattern(" E ")
                .pattern(" E ")
                .pattern(" S ")
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(this.output);

        this.shaped(RecipeCategory.TOOLS, ModItems.EMERALD_SHOVEL.get())
                .pattern(" E ")
                .pattern(" S ")
                .pattern(" S ")
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(this.output);

        this.shaped(RecipeCategory.TOOLS, ModItems.EMERALD_HOE.get())
                .pattern(" EE")
                .pattern(" S ")
                .pattern(" S ")
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(this.output);

        this.shaped(RecipeCategory.TOOLS, ModItems.MAGIC_PICKAXE.get())
                .pattern("DED")
                .pattern(" S ")
                .pattern(" S ")
                .define('D', Items.DIAMOND)
                .define('E', Items.ENDER_EYE)
                .define('S', Items.STICK)
                .unlockedBy("has_diamond", has(Items.ENDER_EYE))
                .save(this.output);


        // BLOCK RECIPES
        shaped(RecipeCategory.MISC, ModBlocks.TEST_BLOCK.get())
                .pattern("DDD")
                .pattern("DDD")
                .pattern("DDD")
                .define('D', Items.DIAMOND_BLOCK)
                .unlockedBy("has_diamond", has(Items.DIAMOND)).save(output);

        shaped(RecipeCategory.MISC, ModBlocks.CUSTOM_TEST_BLOCK.get())
                .pattern("EEE")
                .pattern("EEE")
                .pattern("EEE")
                .define('E', Items.EMERALD_BLOCK)
                .unlockedBy("has_diamond", has(Items.EMERALD)).save(output);
    }

    public static class Generator extends RecipeProvider.Runner {
        public Generator(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(packOutput, lookupProvider);
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider provider, @NotNull RecipeOutput output) {
            return new ModRecipeProvider(provider, output);
        }

        @Override
        public @NotNull String getName() {
            return "My Recipes";
        }
    }
}