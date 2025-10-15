package de.davidvogt.hkbmod.datagen;

import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.item.ModItems;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.util.ModTags;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
        super(provider, recipeOutput);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
            return new ModRecipeProvider(provider, recipeOutput);
        }

        @Override
        public String getName() {
            return "My Recipes";
        }
    }

    /**
     * Creates a RecipeOutput wrapper that converts ShapedRecipes to ResearchCraftingRecipes
     */
    private RecipeOutput researchRecipeOutput() {
        return new RecipeOutput() {
            @Override
            public void accept(net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeKey, net.minecraft.world.item.crafting.Recipe<?> recipe, @javax.annotation.Nullable net.minecraft.advancements.AdvancementHolder advancement, net.neoforged.neoforge.common.conditions.ICondition... conditions) {
                if (recipe instanceof ShapedRecipe shapedRecipe) {
                    // Wrap the shaped recipe in a ResearchCraftingRecipe
                    ResearchCraftingRecipe researchRecipe = new ResearchCraftingRecipe(shapedRecipe);
                    output.accept(recipeKey, researchRecipe, advancement, conditions);
                } else {
                    // Pass through non-shaped recipes unchanged
                    output.accept(recipeKey, recipe, advancement, conditions);
                }
            }

            @Override
            public net.minecraft.advancements.Advancement.Builder advancement() {
                return output.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                output.includeRootAdvancement();
            }
        };
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

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.TOOLS, ModItems.EMERALD_PICKAXE.get())
                .pattern("EEE")
                .pattern(" S ")
                .pattern(" S ")
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.COMBAT, ModItems.EMERALD_SWORD.get())
                .pattern(" E ")
                .pattern(" E ")
                .pattern(" S ")
                .define('E', Items.EMERALD)
                .define('S', Items.STICK)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

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

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.TOOLS, ModItems.MAGIC_PICKAXE.get())
                .pattern("DED")
                .pattern("TST")
                .pattern(" S ")
                .define('D', Items.DIAMOND)
                .define('E', Items.ENDER_EYE)
                .define('S', Items.STICK)
                .define('T', Items.TNT)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.TOOLS, ModItems.TIME_SETTER.get())
                .pattern(" E ")
                .pattern("ECE")
                .pattern(" E ")
                .define('E', Items.ENDER_PEARL)
                .define('C', Items.CLOCK)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.COMBAT, ModItems.LONGBOW.get())
                .pattern("S ")
                .pattern("SL")
                .pattern("S ")
                .define('L', ModItems.LONGBOW_STICK.get())
                .define('S', Items.STRING)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.TOOLS, ModItems.LONGBOW_STICK.get())
                .pattern("L ")
                .pattern(" L")
                .pattern("L ")
                .define('L', ModItems.LONG_STICK.get())
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.COMBAT, ModItems.LONGBOW_ARROW.get())
                .pattern(" A ")
                .pattern(" L ")
                .pattern(" F ")
                .define('A', Items.FLINT)
                .define('L', ModItems.LONG_STICK)
                .define('F', Items.FEATHER)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.COMBAT, ModItems.LONG_STICK.get())
                .pattern("S")
                .pattern("S")
                .pattern("S")
                .define('S', Items.STICK)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.COMBAT, ModItems.LONG_STRING.get())
                .pattern("S")
                .pattern("S")
                .pattern("S")
                .define('S', Items.STRING)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.TOOLS, ModItems.DRAGON_MATERIAL.get())
                .pattern("DD")
                .pattern("DD")
                .define('D', ModItems.DRAGON_SKIN.get())
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        // Research recipe - only works in Research Crafting Table
        this.shaped(RecipeCategory.TOOLS, ModItems.DRAGON_SADDLE.get())
                .pattern("DDD")
                .pattern("D D")
                .define('D', ModItems.DRAGON_MATERIAL.get())
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());


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

        shaped(RecipeCategory.MISC, ModBlocks.RESEARCH_TABLE.get())
                .pattern("BBB")
                .pattern("LPL")
                .pattern("L L")
                .define('B', Items.BOOK)
                .define('L', Items.OAK_LOG)
                .define('P', Items.PAPER)
                .unlockedBy("has_book", has(Items.BOOK)).save(output);

        // Research recipe - only works in Research Crafting Table
        shaped(RecipeCategory.TOOLS, ModBlocks.ELASTIC_WOOD.get())
                .pattern("RSR")
                .pattern("SRS")
                .pattern("RSR")
                .define('R', ModBlocks.ROBINIA_LOG.get())
                .define('S', Items.STRING)
                .unlockedBy("never", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BARRIER))
                .save(researchRecipeOutput());

        shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ROBINIA_PLANKS.get(), 4)
                .requires(ModTags.Items.ROBINIA_LOG)
                .unlockedBy("has_robinia_log", has(ModBlocks.ROBINIA_LOG.get()))
                .save(output);

        stairBuilder(ModBlocks.ROBINIA_STAIRS.get(), Ingredient.of(ModBlocks.ROBINIA_PLANKS.get())).group("robinia_planks")
                .unlockedBy("has_robinia_planks", has(ModBlocks.ROBINIA_PLANKS.get())).save(output);
        slab(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ROBINIA_SLAB.get(), ModBlocks.ROBINIA_PLANKS.get());

        buttonBuilder(ModBlocks.ROBINIA_BUTTON.get(), Ingredient.of(ModBlocks.ROBINIA_PLANKS.get())).group("robinia_planks")
                .unlockedBy("has_robinia_planks", has(ModBlocks.ROBINIA_PLANKS.get())).save(output);
        pressurePlate(ModBlocks.ROBINIA_PRESSURE_PLATE.get(), ModBlocks.ROBINIA_PLANKS.get());

        fenceBuilder(ModBlocks.ROBINIA_FENCE.get(), Ingredient.of(ModBlocks.ROBINIA_PLANKS.get())).group("robinia_planks")
                .unlockedBy("has_robinia_planks", has(ModBlocks.ROBINIA_PLANKS.get())).save(output);
        fenceGateBuilder(ModBlocks.ROBINIA_FENCE_GATE.get(), Ingredient.of(ModBlocks.ROBINIA_PLANKS.get())).group("robinia_planks")
                .unlockedBy("has_robinia_planks", has(ModBlocks.ROBINIA_PLANKS.get())).save(output);

        doorBuilder(ModBlocks.ROBINIA_DOOR.get(), Ingredient.of(ModBlocks.ROBINIA_PLANKS.get())).group("robinia_planks")
                .unlockedBy("has_robinia_planks", has(ModBlocks.ROBINIA_PLANKS.get())).save(output);
        trapdoorBuilder(ModBlocks.ROBINIA_TRAPDOOR.get(), Ingredient.of(ModBlocks.ROBINIA_PLANKS.get())).group("robinia_planks")
                .unlockedBy("has_robinia_planks", has(ModBlocks.ROBINIA_PLANKS.get())).save(output);
    }
}