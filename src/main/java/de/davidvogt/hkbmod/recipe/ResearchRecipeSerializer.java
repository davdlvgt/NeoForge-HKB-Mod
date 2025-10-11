package de.davidvogt.hkbmod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.*;

/**
 * Serializer for ResearchCraftingRecipe.
 * Handles reading/writing recipes from JSON and network packets.
 */
public class ResearchRecipeSerializer implements RecipeSerializer<ResearchCraftingRecipe> {

    // Codec for reading from JSON
    private static final MapCodec<ResearchCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ResearchCraftingRecipe::category),
                    ShapedRecipe.Serializer.CODEC.forGetter(ResearchCraftingRecipe::getInternalRecipe)
            ).apply(instance, ResearchCraftingRecipe::new)
    );

    // StreamCodec for network synchronization
    private static final StreamCodec<RegistryFriendlyByteBuf, ResearchCraftingRecipe> STREAM_CODEC =
            StreamCodec.of(
                    ResearchRecipeSerializer::toNetwork,
                    ResearchRecipeSerializer::fromNetwork
            );

    @Override
    public MapCodec<ResearchCraftingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ResearchCraftingRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    /**
     * Writes the recipe to the network buffer (server -> client)
     */
    private static void toNetwork(RegistryFriendlyByteBuf buffer, ResearchCraftingRecipe recipe) {
        // Write category
        buffer.writeEnum(recipe.category());

        // Write the internal shaped recipe using its serializer
        ShapedRecipe.Serializer.SHAPED_RECIPE.streamCodec().encode(buffer, recipe.getInternalRecipe());
    }

    /**
     * Reads the recipe from the network buffer (client <- server)
     */
    private static ResearchCraftingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        // Read category
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);

        // Read the internal shaped recipe using its serializer
        ShapedRecipe shapedRecipe = ShapedRecipe.Serializer.SHAPED_RECIPE.streamCodec().decode(buffer);

        return new ResearchCraftingRecipe(category, shapedRecipe);
    }
}
