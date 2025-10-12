package de.davidvogt.hkbmod.network;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.recipe.ModRecipeTypes;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.screen.cutsom.ResearchCraftingTableMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server to place a recipe in the crafting grid.
 * This is used when the player clicks a recipe in the recipe book.
 */
public record PlaceRecipePacket(ResourceLocation recipeId, boolean placeAll) implements CustomPacketPayload {

    public static final Type<PlaceRecipePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("hkbmod", "place_recipe"));

    public static final StreamCodec<ByteBuf, PlaceRecipePacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, PlaceRecipePacket::recipeId,
        StreamCodec.of((buf, val) -> buf.writeBoolean(val), ByteBuf::readBoolean), PlaceRecipePacket::placeAll,
        PlaceRecipePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle the packet on the server side
     */
    public static void handle(PlaceRecipePacket packet, IPayloadContext context) {
        HKBMod.LOGGER.info("PlaceRecipePacket received on server: recipeId={}, placeAll={}", packet.recipeId(), packet.placeAll());
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                HKBMod.LOGGER.info("Player: {}", serverPlayer.getName().getString());
                AbstractContainerMenu menu = serverPlayer.containerMenu;

                if (!(menu instanceof ResearchCraftingTableMenu craftingMenu)) {
                    HKBMod.LOGGER.warn("Player {} tried to place recipe but is not in Research Crafting Table menu", serverPlayer.getName().getString());
                    return;
                }

                HKBMod.LOGGER.info("Player is in Research Crafting Table menu");

                // Look up the recipe
                ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, packet.recipeId());
                var recipeOptional = serverPlayer.getServer().getRecipeManager().byKey(recipeKey);

                if (recipeOptional.isEmpty()) {
                    HKBMod.LOGGER.warn("Recipe {} not found", packet.recipeId());
                    return;
                }

                HKBMod.LOGGER.info("Recipe found in recipe manager");

                RecipeHolder<?> holder = recipeOptional.get();
                if (holder.value().getType() != ModRecipeTypes.RESEARCH_CRAFTING_TYPE.get()) {
                    HKBMod.LOGGER.warn("Recipe {} is not a research crafting recipe", packet.recipeId());
                    return;
                }

                HKBMod.LOGGER.info("Recipe is a research crafting recipe");

                @SuppressWarnings("unchecked")
                RecipeHolder<ResearchCraftingRecipe> researchHolder = (RecipeHolder<ResearchCraftingRecipe>) holder;
                ResearchCraftingRecipe recipe = researchHolder.value();
                ShapedRecipe shapedRecipe = recipe.getInternalRecipe();

                HKBMod.LOGGER.info("Calling placeRecipeInGrid");
                // Place the recipe in the crafting grid
                placeRecipeInGrid(craftingMenu, shapedRecipe, serverPlayer, packet.placeAll());
                HKBMod.LOGGER.info("placeRecipeInGrid completed");
            }
        });
    }

    /**
     * Places a recipe's ingredients into the crafting grid
     */
    private static void placeRecipeInGrid(ResearchCraftingTableMenu menu, ShapedRecipe recipe,
                                         ServerPlayer player, boolean placeAll) {
        HKBMod.LOGGER.info("Starting placeRecipeInGrid");

        // Clear the crafting grid first
        for (int i = 0; i < 9; i++) {
            ItemStack existing = menu.getBlockEntity().inventory.getStackInSlot(i);
            if (!existing.isEmpty()) {
                HKBMod.LOGGER.info("Clearing slot {}: {}", i, existing);
                // Try to return items to player inventory
                if (!player.getInventory().add(existing)) {
                    player.drop(existing, false);
                }
                menu.getBlockEntity().inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        // Get recipe dimensions and ingredients
        int width = recipe.getWidth();
        int height = recipe.getHeight();
        var placementInfo = recipe.placementInfo();
        var ingredientsList = placementInfo.ingredients();

        HKBMod.LOGGER.info("Recipe dimensions: width={}, height={}, placementInfo ingredients={}", width, height, ingredientsList.size());

        // Build two lists:
        // 1. availableItems - items the player actually has
        // 2. ghostItems - items from recipe (for display), falling back to first matching item type
        List<ItemStack> availableItems = new ArrayList<>();
        List<ItemStack> ghostItems = new ArrayList<>();

        for (Ingredient ingredient : ingredientsList) {
            ItemStack playerItem = ItemStack.EMPTY;
            ItemStack ghostItem = ItemStack.EMPTY;

            // Try to find what the player has for this ingredient
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    playerItem = stack.copy();
                    playerItem.setCount(1);
                    break;
                }
            }

            // For ghost display, use player's item if they have it, otherwise use first possible item from ingredient
            if (!playerItem.isEmpty()) {
                ghostItem = playerItem.copy();
            } else {
                // Player doesn't have it - show first matching item from ingredient as ghost
                var itemHolders = ingredient.items().toList();
                if (!itemHolders.isEmpty()) {
                    ghostItem = new ItemStack(itemHolders.get(0));
                    ghostItem.setCount(1);
                }
            }

            availableItems.add(playerItem);
            ghostItems.add(ghostItem);
            HKBMod.LOGGER.info("Ingredient - Available: {}, Ghost: {}", playerItem, ghostItem);
        }

        // Build a test grid by trying different placements until the recipe matches
        // Use ghostItems (not availableItems) so we can determine correct layout
        List<ItemStack> testGrid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            testGrid.add(ItemStack.EMPTY);
        }

        // Now try to place each ingredient in each slot until the recipe matches
        boolean foundValidPlacement = placeIngredientsRecursive(recipe, player, testGrid, ingredientsList, ghostItems, 0);

        if (!foundValidPlacement) {
            HKBMod.LOGGER.warn("Could not find valid placement for recipe");
        }

        // Build ghost items list - ALWAYS show all recipe items as ghosts based on testGrid
        List<SetGhostRecipePacket.GhostSlot> ghostSlots = new ArrayList<>();

        // Add ALL recipe items as ghost items (they will show through empty slots)
        for (int slot = 0; slot < 9; slot++) {
            ItemStack testStack = testGrid.get(slot);
            if (!testStack.isEmpty()) {
                ghostSlots.add(new SetGhostRecipePacket.GhostSlot(slot, testStack.copy()));
                HKBMod.LOGGER.info("Adding ghost item {} in slot {}", testStack, slot);
            }
        }

        // Now place the actual items on top based on the test grid
        // Only place items that the player actually has
        for (int slot = 0; slot < 9; slot++) {
            ItemStack testStack = testGrid.get(slot);
            if (!testStack.isEmpty()) {
                // Find the matching ingredient
                Ingredient matchingIngredient = null;
                for (Ingredient ing : ingredientsList) {
                    if (ing.test(testStack)) {
                        matchingIngredient = ing;
                        break;
                    }
                }

                if (matchingIngredient != null) {
                    // Only place if player actually has this item
                    ItemStack taken = findMatchingItem(player, matchingIngredient, placeAll);
                    if (!taken.isEmpty()) {
                        menu.getBlockEntity().inventory.setStackInSlot(slot, taken.copy());
                        HKBMod.LOGGER.info("Placed {} in slot {}", taken, slot);
                    } else {
                        HKBMod.LOGGER.info("Player doesn't have item for slot {}, leaving empty (ghost will show)", slot);
                    }
                }
            }
        }

        // Send ghost items to client (will render under actual items)
        PacketDistributor.sendToPlayer(player, new SetGhostRecipePacket(ghostSlots));

        // Mark block entity as changed
        menu.getBlockEntity().setChanged();

        // Trigger slot change notification to update the result slot
        // We need to call this on each crafting slot to ensure the menu detects the change
        for (int i = 0; i < 9; i++) {
            if (i < menu.slots.size()) {
                menu.slots.get(i).setChanged();
            }
        }

        // Broadcast changes to client
        menu.broadcastChanges();
        HKBMod.LOGGER.info("placeRecipeInGrid finished - triggered slot updates");
    }

    /**
     * Recursively tries to place ingredients in the grid until recipe matches
     * Note: ghostItems is used (not availableItems) so layout is determined regardless of what player has
     */
    private static boolean placeIngredientsRecursive(ShapedRecipe recipe, ServerPlayer player,
                                                     List<ItemStack> testGrid, List<Ingredient> ingredients,
                                                     List<ItemStack> ghostItems, int ingredientIndex) {
        // Base case: all ingredients placed
        if (ingredientIndex >= ingredients.size()) {
            // Test if the recipe matches
            CraftingInput input = CraftingInput.of(3, 3, testGrid);
            boolean matches = recipe.matches(input, player.level());
            HKBMod.LOGGER.info("Testing complete grid, matches: {}", matches);
            return matches;
        }

        ItemStack itemToPlace = ghostItems.get(ingredientIndex);
        if (itemToPlace.isEmpty()) {
            // This ingredient has no representative item (shouldn't happen)
            // Skip to next ingredient
            return placeIngredientsRecursive(recipe, player, testGrid, ingredients, ghostItems, ingredientIndex + 1);
        }

        // Try placing in each empty slot
        for (int slot = 0; slot < 9; slot++) {
            if (!testGrid.get(slot).isEmpty()) {
                continue; // Slot already occupied
            }

            // Try placing here
            testGrid.set(slot, itemToPlace.copy());
            HKBMod.LOGGER.info("Trying ingredient {} in slot {}", ingredientIndex, slot);

            // Recurse to place next ingredient
            if (placeIngredientsRecursive(recipe, player, testGrid, ingredients, ghostItems, ingredientIndex + 1)) {
                return true; // Found a valid placement!
            }

            // Didn't work, try next slot
            testGrid.set(slot, ItemStack.EMPTY);
        }

        return false; // No valid placement found
    }

    /**
     * Finds a matching item in the player's inventory for the given ingredient
     */
    private static ItemStack findMatchingItem(ServerPlayer player, Ingredient ingredient, boolean placeAll) {
        // Search player inventory for matching items
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                // Take items from player inventory
                int amount = placeAll ? stack.getCount() : 1;
                ItemStack taken = player.getInventory().removeItem(i, amount);
                return taken;
            }
        }

        return ItemStack.EMPTY;
    }
}
