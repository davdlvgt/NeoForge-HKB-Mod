package de.davidvogt.hkbmod.screen.cutsom;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.network.RequestRecipeSyncPacket;
import de.davidvogt.hkbmod.network.ResearchRecipeSyncPacket;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.screen.cutsom.recipebook.ResearchRecipeBookComponent;
import de.davidvogt.hkbmod.util.RecipeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for the Research Crafting Table.
 * Displays a 3x3 crafting grid with result slot, player inventory, and recipe book.
 */
public class ResearchCraftingTableScreen extends AbstractContainerScreen<ResearchCraftingTableMenu> {
    // Use vanilla crafting table texture as a base
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/crafting_table.png");

    // Recipe book button sprites (reuse vanilla's)
    private static final WidgetSprites RECIPE_BUTTON_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/button"),
        ResourceLocation.withDefaultNamespace("recipe_book/button_highlighted")
    );

    private final ResearchRecipeBookComponent recipeBook = new ResearchRecipeBookComponent();
    private ImageButton recipeBookButton;
    private boolean widthTooNarrow;
    private boolean recipesLoaded = false;
    private List<ItemStack> ghostRecipe = new ArrayList<>();

    public ResearchCraftingTableScreen(ResearchCraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.widthTooNarrow = this.width < 379;

        // Initialize recipe book
        this.recipeBook.init(this.menu, this.minecraft, this.leftPos, this.topPos);

        // Calculate position for recipe book button
        // Position at same height as middle row of crafting grid (row 2)
        int buttonX = this.leftPos + 5;
        int buttonY = this.topPos + 34; // Crafting grid starts at 17, middle row at 17+18=35, button centered at 31

        // Create and add recipe book button
        this.recipeBookButton = new ImageButton(
            buttonX, buttonY,
            20, 18,
            RECIPE_BUTTON_SPRITES,
            button -> {
                this.recipeBook.toggleVisibility();
                this.updateScreenPosition();
            }
        );
        this.addRenderableWidget(this.recipeBookButton);

        // Adjust GUI position if recipe book is open
        this.updateScreenPosition();
    }

    /**
     * Updates the screen position based on recipe book visibility
     */
    private void updateScreenPosition() {
        if (this.recipeBook.isVisible() && !this.widthTooNarrow) {
            // Shift the GUI to the right when recipe book is open
            this.leftPos = (this.width - this.imageWidth) / 2 + 86;
        } else {
            // Center the GUI normally
            this.leftPos = (this.width - this.imageWidth) / 2;
        }

        // Update recipe book position
        this.recipeBook.init(this.menu, this.minecraft, this.leftPos, this.topPos);

        // Update recipe book button position
        if (this.recipeBookButton != null) {
            this.recipeBookButton.setPosition(this.leftPos + 5, this.topPos + 34);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Load recipes on first tick
        loadRecipes();

        // Update recipe book collections periodically
        if (this.recipeBook.isVisible()) {
            this.recipeBook.updateRecipeCollections();
        }
    }

    /**
     * Loads all research recipes into the recipe book
     * Recipes are sent from server via ResearchRecipeSyncPacket
     */
    private void loadRecipes() {
        // Only request once when recipe book becomes visible
        if (!this.recipeBook.isVisible() || recipesLoaded) {
            return;
        }

        // Request recipes from server
        // Server will send ResearchRecipeSyncPacket in response
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(new RequestRecipeSyncPacket());
        }
        recipesLoaded = true;
    }

    /**
     * Handle incoming recipe sync packet from server
     * Converts RecipeData to simplified RecipeHolder objects for display
     */
    public void handleRecipeSync(List<ResearchRecipeSyncPacket.RecipeData> recipeDataList) {
        List<RecipeHolder<ResearchCraftingRecipe>> recipes = new ArrayList<>();

        for (ResearchRecipeSyncPacket.RecipeData data : recipeDataList) {
            try {
                // Get the item from registry
                var itemOptional = BuiltInRegistries.ITEM.getOptional(data.resultItem());
                if (itemOptional.isEmpty()) {
                    HKBMod.LOGGER.warn("Could not find item {} for recipe {}", data.resultItem(), data.recipeId());
                    continue;
                }

                // Reconstruct ItemStack for result
                ItemStack resultStack = new ItemStack(itemOptional.get(), data.resultCount());

                // Create a simplified shaped recipe for display purposes
                // We use an empty pattern since we only need the result item for the recipe book
                List<java.util.Optional<Ingredient>> emptyIngredients = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    emptyIngredients.add(java.util.Optional.empty());
                }

                ShapedRecipePattern pattern = new ShapedRecipePattern(
                    3,
                    3,
                    emptyIngredients,
                    java.util.Optional.empty()
                );

                // Create ShapedRecipe
                ShapedRecipe shapedRecipe = new ShapedRecipe(
                    "", // group
                    data.category(),
                    pattern,
                    resultStack,
                    false // show notification
                );

                // Wrap in ResearchCraftingRecipe
                ResearchCraftingRecipe researchRecipe = new ResearchCraftingRecipe(shapedRecipe);

                // Create RecipeHolder
                ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(
                    Registries.RECIPE,
                    data.recipeId()
                );
                RecipeHolder<ResearchCraftingRecipe> holder = new RecipeHolder<>(recipeKey, researchRecipe);

                recipes.add(holder);
            } catch (Exception e) {
                HKBMod.LOGGER.error("Failed to reconstruct recipe from sync data: {}", data.recipeId(), e);
            }
        }

        // Update recipe book with reconstructed recipes
        this.recipeBook.setRecipes(recipes);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Render main GUI texture
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render recipe book
        this.recipeBook.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render tooltips
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if recipe book handled the click
        if (this.recipeBook.isVisible() && this.recipeBook.mouseClicked(mouseX, mouseY, button)) {
            // Don't clear ghost recipe when clicking in recipe book
            return true;
        }

        // Clear ghost recipe when clicking outside recipe book (e.g., on crafting slots or inventory)
        if (button == 0) { // Left click
            clearGhostRecipe();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Check if recipe book handled the scroll
        if (this.recipeBook.isVisible() && this.recipeBook.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        // Close recipe book when screen is closed
        this.recipeBook.setVisible(false);
        super.removed();
    }

    /**
     * Sets the ghost recipe items to display in the crafting grid
     */
    public void setGhostRecipe(List<ItemStack> ghostItems) {
        this.ghostRecipe = ghostItems;
        HKBMod.LOGGER.info("Ghost recipe set with {} items", ghostItems.size());
        for (int i = 0; i < ghostItems.size(); i++) {
            if (!ghostItems.get(i).isEmpty()) {
                HKBMod.LOGGER.info("  Slot {}: {}", i, ghostItems.get(i));
            }
        }
    }

    /**
     * Clears the ghost recipe display
     */
    public void clearGhostRecipe() {
        this.ghostRecipe.clear();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // Render ghost items
        if (!ghostRecipe.isEmpty()) {
            int craftingStartX = 30;
            int craftingStartY = 17;

            for (int i = 0; i < Math.min(9, ghostRecipe.size()); i++) {
                ItemStack ghostStack = ghostRecipe.get(i);
                if (!ghostStack.isEmpty()) {
                    int row = i / 3;
                    int col = i % 3;
                    int x = craftingStartX + col * 18;
                    int y = craftingStartY + row * 18;

                    // Check if the actual slot is empty
                    ItemStack slotStack = this.menu.getBlockEntity().inventory.getStackInSlot(i);
                    if (slotStack.isEmpty()) {
                        // Render ghost item (semi-transparent with red background)
                        HKBMod.LOGGER.info("Rendering ghost item in slot {}: {}", i, ghostStack);
                        renderGhostItem(guiGraphics, ghostStack, x, y);
                    } else {
                        HKBMod.LOGGER.info("Slot {} not empty, skipping ghost: {}", i, slotStack);
                    }
                }
            }
        } else {
            // Log once per second to avoid spam
            if (minecraft.level.getGameTime() % 20 == 0) {
                HKBMod.LOGGER.info("Ghost recipe is empty, not rendering");
            }
        }
    }

    /**
     * Renders a ghost item (semi-transparent with red background)
     */
    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        // Draw red background
        guiGraphics.fill(x, y, x + 16, y + 16, 0x44FF0000); // Semi-transparent red

        // Render the item itself
        guiGraphics.renderItem(stack, x, y);

        // Apply white transparency overlay to make it look ghostly
        guiGraphics.fill(x, y, x + 16, y + 16, 0xAA808080); // Semi-transparent gray overlay for ghost effect
    }
}
