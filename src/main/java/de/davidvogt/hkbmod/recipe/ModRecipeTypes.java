package de.davidvogt.hkbmod.recipe;

import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for custom recipe types and serializers.
 * Contains the ResearchCrafting recipe type that can only be used in the Research Crafting Table.
 */
public class ModRecipeTypes {

    // Recipe Type Registry
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, HKBMod.MODID);

    // Recipe Serializer Registry
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, HKBMod.MODID);

    // Custom Recipe Type - Only works in Research Crafting Table
    public static final DeferredHolder<RecipeType<?>, RecipeType<ResearchCraftingRecipe>> RESEARCH_CRAFTING_TYPE =
            RECIPE_TYPES.register("research_crafting", () -> new RecipeType<ResearchCraftingRecipe>() {
                @Override
                public String toString() {
                    return "research_crafting";
                }
            });

    // Custom Recipe Serializer
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ResearchCraftingRecipe>> RESEARCH_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("research_crafting", ResearchRecipeSerializer::new);

    /**
     * Register recipe types and serializers to the mod event bus
     */
    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        HKBMod.LOGGER.info("Registered custom recipe types for {}", HKBMod.MODID);
    }
}
