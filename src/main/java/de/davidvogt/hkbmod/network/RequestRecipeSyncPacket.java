package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.recipe.ModRecipeTypes;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.research.PlayerResearchHelper;
import de.davidvogt.hkbmod.util.RecipeHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server to request research recipe sync
 * Server responds with ResearchRecipeSyncPacket containing available recipes
 */
public record RequestRecipeSyncPacket() implements CustomPacketPayload {

    public static final Type<RequestRecipeSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("hkbmod", "request_recipe_sync"));

    // Empty codec since packet has no data
    public static final StreamCodec<ByteBuf, RequestRecipeSyncPacket> STREAM_CODEC = StreamCodec.unit(new RequestRecipeSyncPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle the packet on the server side
     */
    public static void handle(RequestRecipeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Get all research recipes from the recipe manager
                List<RecipeHolder<ResearchCraftingRecipe>> availableRecipes = new ArrayList<>();

                // Get all whitelisted recipe IDs
                for (String recipeId : RecipeHelper.getAllowedRecipeIds()) {
                    ResourceLocation recipeLocation = ResourceLocation.fromNamespaceAndPath("hkbmod", recipeId);

                    // Check if player has unlocked this recipe
                    if (!PlayerResearchHelper.hasRecipeUnlocked(serverPlayer, recipeLocation)) {
                        continue; // Skip locked recipes
                    }

                    // Look up the recipe
                    ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeLocation);
                    var recipeOptional = serverPlayer.getServer().getRecipeManager().byKey(recipeKey);

                    if (recipeOptional.isPresent()) {
                        RecipeHolder<?> holder = recipeOptional.get();
                        // Check if it's our custom research recipe type
                        if (holder.value().getType() == ModRecipeTypes.RESEARCH_CRAFTING_TYPE.get()) {
                            @SuppressWarnings("unchecked")
                            RecipeHolder<ResearchCraftingRecipe> researchHolder = (RecipeHolder<ResearchCraftingRecipe>) holder;
                            availableRecipes.add(researchHolder);
                        }
                    }
                }

                // Convert to RecipeData for network transmission
                List<ResearchRecipeSyncPacket.RecipeData> recipeDataList = availableRecipes.stream()
                    .map(ResearchRecipeSyncPacket.RecipeData::fromRecipeHolder)
                    .toList();

                // Send packet back to client
                PacketDistributor.sendToPlayer(serverPlayer, new ResearchRecipeSyncPacket(recipeDataList));
            }
        });
    }
}
