package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.screen.cutsom.ResearchCraftingTableScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Packet to sync available research crafting recipes from server to client
 * This allows the recipe book to display recipes that the player has unlocked
 */
public record ResearchRecipeSyncPacket(List<RecipeData> recipes) implements CustomPacketPayload {

    public static final Type<ResearchRecipeSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("hkbmod", "research_recipe_sync"));
    public static final StreamCodec<ByteBuf, ResearchRecipeSyncPacket> STREAM_CODEC = StreamCodec.composite(
            RecipeData.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchRecipeSyncPacket::recipes,
            ResearchRecipeSyncPacket::new
    );

    /**
     * Handle the packet on the client side
     */
    public static void handle(ResearchRecipeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof ResearchCraftingTableScreen screen) {
                screen.handleRecipeSync(packet.recipes());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Simplified recipe data for network transmission
     * Only includes the data needed to display the recipe in the book
     */
    public record RecipeData(
            ResourceLocation recipeId,
            ResourceLocation resultItem,
            int resultCount,
            CraftingBookCategory category
    ) {
        public static final StreamCodec<ByteBuf, RecipeData> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, RecipeData::recipeId,
                ResourceLocation.STREAM_CODEC, RecipeData::resultItem,
                ByteBufCodecs.INT, RecipeData::resultCount,
                CraftingBookCategory.STREAM_CODEC, RecipeData::category,
                RecipeData::new
        );

        /**
         * Convert a RecipeHolder to RecipeData for network transmission
         */
        public static RecipeData fromRecipeHolder(RecipeHolder<ResearchCraftingRecipe> holder) {
            ResearchCraftingRecipe recipe = holder.value();

            // Get result item
            ResourceLocation resultItem = BuiltInRegistries.ITEM.getKey(recipe.getResultItem().getItem());
            int resultCount = recipe.getResultItem().getCount();

            return new RecipeData(
                    holder.id().location(),
                    resultItem,
                    resultCount,
                    recipe.category()
            );
        }
    }
}
