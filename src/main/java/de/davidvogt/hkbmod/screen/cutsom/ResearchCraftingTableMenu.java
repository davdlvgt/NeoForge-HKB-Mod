package de.davidvogt.hkbmod.screen.cutsom;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.block.ModBlocks;
import de.davidvogt.hkbmod.block.entity.ResearchCraftingTableBlockEntity;
import de.davidvogt.hkbmod.research.PlayerResearchHelper;
import de.davidvogt.hkbmod.screen.ModMenuTypes;
import de.davidvogt.hkbmod.util.RecipeHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Research Crafting Table.
 * This crafting table allows crafting with a 3x3 grid stored in the block entity.
 * Only whitelisted recipes from RecipeHelper can be crafted.
 * Recipes must be unlocked by the player via research before they can see the output.
 */
public class ResearchCraftingTableMenu extends AbstractContainerMenu {
    private final ResearchCraftingTableBlockEntity blockEntity;
    private final Level level;
    private final Player player;
    private final ContainerLevelAccess access;

    // The crafting result slot
    private final ResultSlot resultSlot;
    private final CraftingContainer craftMatrix;
    private final ResultContainer resultContainer;

    public ResearchCraftingTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ResearchCraftingTableMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.RESEARCH_CRAFTING_TABLE_MENU.get(), containerId);

        if (!(blockEntity instanceof ResearchCraftingTableBlockEntity)) {
            throw new IllegalStateException("Wrong block entity type for ResearchCraftingTableMenu!");
        }

        this.blockEntity = (ResearchCraftingTableBlockEntity) blockEntity;
        this.level = playerInventory.player.level();
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(level, blockEntity.getBlockPos());

        // Create a crafting container wrapper around the block entity's inventory
        this.craftMatrix = new TransientCraftingContainer(this, 3, 3);
        this.resultContainer = new ResultContainer();

        // Add crafting grid slots (3x3 = 9 slots) - connected to block entity
        int craftingStartX = 30;
        int craftingStartY = 17;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new SlotItemHandler(this.blockEntity.inventory, index,
                    craftingStartX + col * 18, craftingStartY + row * 18) {
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        ResearchCraftingTableMenu.this.slotsChanged(craftMatrix);
                    }
                });
            }
        }

        // Add result slot (slot 9)
        this.resultSlot = new ResultSlot(playerInventory.player, craftMatrix,
            resultContainer, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                // Create CraftingInput from current grid
                CraftingInput craftingInput = createCraftingInput();

                // Find the matching recipe again to get remaining items
                RecipeHolder<CraftingRecipe> matchingRecipe = findMatchingRecipe(craftingInput, level);

                if (matchingRecipe != null) {
                    // Get remaining items (e.g., empty buckets)
                    net.minecraft.core.NonNullList<ItemStack> remainingItems =
                        matchingRecipe.value().getRemainingItems(craftingInput);

                    // Update crafting grid: consume items and place remaining items
                    for (int i = 0; i < 9; i++) {
                        ItemStack slotStack = ResearchCraftingTableMenu.this.blockEntity.inventory.getStackInSlot(i);
                        ItemStack remainingItem = remainingItems.get(i);

                        if (!slotStack.isEmpty()) {
                            // Consume one item from the slot
                            ResearchCraftingTableMenu.this.blockEntity.inventory.extractItem(i, 1, false);
                            slotStack = ResearchCraftingTableMenu.this.blockEntity.inventory.getStackInSlot(i);
                        }

                        // Handle remaining items (container items)
                        if (!remainingItem.isEmpty()) {
                            // If the slot is now empty, place the remaining item
                            if (slotStack.isEmpty()) {
                                ResearchCraftingTableMenu.this.blockEntity.inventory.setStackInSlot(i, remainingItem);
                            }
                            // If the slot has the same item, try to merge
                            else if (ItemStack.isSameItemSameComponents(slotStack, remainingItem)) {
                                remainingItem.grow(slotStack.getCount());
                                ResearchCraftingTableMenu.this.blockEntity.inventory.setStackInSlot(i, remainingItem);
                            }
                            // Otherwise, give it to the player or drop it
                            else {
                                if (!player.getInventory().add(remainingItem)) {
                                    player.drop(remainingItem, false);
                                }
                            }
                        }
                    }

                    // Mark the block entity as changed
                    ResearchCraftingTableMenu.this.blockEntity.setChanged();
                }

                super.onTake(player, stack);
                ResearchCraftingTableMenu.this.slotsChanged(craftMatrix);
            }
        };
        this.addSlot(resultSlot);

        // Add player inventory (slots 10-36)
        int playerInventoryY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    8 + col * 18, playerInventoryY + row * 18));
            }
        }

        // Add player hotbar (slots 37-45)
        int hotbarY = 142;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }

        // Initial recipe check
        this.slotsChanged(craftMatrix);
    }

    /**
     * Called when crafting grid changes - updates the result slot
     */
    @Override
    public void slotsChanged(Container container) {
        this.access.execute((level, pos) -> {
            if (!level.isClientSide()) {
                // Create CraftingInput from block entity inventory
                CraftingInput craftingInput = createCraftingInput();

                // Find matching whitelisted recipe
                RecipeHolder<CraftingRecipe> matchingRecipe = findMatchingRecipe(craftingInput, level);

                if (matchingRecipe != null) {
                    // Assemble the result
                    ItemStack result = matchingRecipe.value().assemble(craftingInput, level.registryAccess());
                    this.resultContainer.setItem(0, result);
                } else {
                    this.resultContainer.setItem(0, ItemStack.EMPTY);
                }
            }
        });

        super.slotsChanged(container);
    }

    /**
     * Creates a CraftingInput from the block entity's inventory
     */
    private CraftingInput createCraftingInput() {
        // Create a list of ItemStacks from the 3x3 grid
        ItemStack[] items = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            items[i] = this.blockEntity.inventory.getStackInSlot(i);
        }

        return CraftingInput.of(3, 3, java.util.List.of(items));
    }

    /**
     * Finds a matching recipe that is allowed in the research crafting table.
     * Only checks whitelisted recipes from RecipeHelper and recipes the player has unlocked.
     */
    @Nullable
    private RecipeHolder<CraftingRecipe> findMatchingRecipe(CraftingInput craftingInput, Level level) {
        // Get all whitelisted recipe IDs
        for (String recipeId : RecipeHelper.getAllowedRecipeIds()) {
            // Create ResourceKey for the recipe
            ResourceLocation recipeLocation = ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, recipeId);

            // Check if the player has unlocked this recipe
            if (!PlayerResearchHelper.hasRecipeUnlocked(this.player, recipeLocation)) {
                continue; // Skip locked recipes - player cannot see output
            }

            ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeLocation);

            // Look up the recipe from the server
            var recipeOptional = level.getServer().getRecipeManager().byKey(recipeKey);

            if (recipeOptional.isPresent()) {
                RecipeHolder<?> recipeHolder = recipeOptional.get();

                // Check if it's a CraftingRecipe
                if (recipeHolder.value() instanceof CraftingRecipe craftingRecipe) {
                    // Check if the recipe matches the current crafting grid
                    if (craftingRecipe.matches(craftingInput, level)) {
                        @SuppressWarnings("unchecked")
                        RecipeHolder<CraftingRecipe> result = (RecipeHolder<CraftingRecipe>) recipeHolder;
                        return result;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // Result slot (index 9)
            if (index == 9) {
                // Try to move result to player inventory
                this.access.execute((level, pos) -> {
                    slotStack.getItem().onCraftedBy(slotStack, player);
                });

                if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(slotStack, itemstack);
            }
            // Crafting grid slots (0-8)
            else if (index < 9) {
                if (!this.moveItemStackTo(slotStack, 10, 46, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory slots (10-45)
            else if (index >= 10 && index < 46) {
                // Try to move to crafting grid
                if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                    // Try to move between inventory and hotbar
                    if (index < 37) {
                        if (!this.moveItemStackTo(slotStack, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(slotStack, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
            if (index == 9) {
                player.drop(slotStack, false);
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.RESEARCH_CRAFTING_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Don't drop items here - the block entity handles it
    }

    public ResearchCraftingTableBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
