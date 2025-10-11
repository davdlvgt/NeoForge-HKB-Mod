package de.davidvogt.hkbmod.screen.cutsom.recipebook;

import com.google.common.collect.Lists;
import de.davidvogt.hkbmod.recipe.ModRecipeTypes;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.screen.cutsom.ResearchCraftingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Main recipe book component for the Research Crafting Table.
 * Displays unlocked research recipes organized by category.
 */
public class ResearchRecipeBookComponent implements Renderable, GuiEventListener {
    private static final ResourceLocation RECIPE_BOOK_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");

    private static final int BOOK_WIDTH = 147;
    private static final int BOOK_HEIGHT = 166;

    private final Map<ResearchRecipeCategory, ResearchRecipeCollection> recipeCollections = new EnumMap<>(ResearchRecipeCategory.class);
    private final List<ResearchRecipeButton> recipeButtons = Lists.newArrayList();

    private ResearchCraftingTableMenu menu;
    private Minecraft minecraft;
    private Player player;

    private int leftPos;
    private int topPos;
    private boolean visible = false;

    private ResearchRecipeCategory currentCategory = ResearchRecipeCategory.EQUIPMENT;
    private RecipeHolder<ResearchCraftingRecipe> selectedRecipe;

    // Pagination
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int RECIPES_PER_PAGE = 20; // 4x5 grid

    public ResearchRecipeBookComponent() {
        // Initialize recipe collections for each category
        for (ResearchRecipeCategory category : ResearchRecipeCategory.values()) {
            recipeCollections.put(category, new ResearchRecipeCollection(category));
        }

        // Initialize recipe buttons
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            recipeButtons.add(new ResearchRecipeButton());
        }
    }

    /**
     * Initializes the recipe book with the menu and minecraft instance
     */
    public void init(ResearchCraftingTableMenu menu, Minecraft minecraft, int leftPos, int topPos) {
        this.menu = menu;
        this.minecraft = minecraft;
        this.player = minecraft.player;
        this.leftPos = leftPos;
        this.topPos = topPos;

        updatePosition();
        updateRecipeCollections();
    }

    /**
     * Updates the position of the recipe book
     */
    private void updatePosition() {
        int bookX = leftPos - BOOK_WIDTH + 4;
        int bookY = topPos;

        // Position recipe buttons in a grid
        int buttonStartX = bookX + 11;
        int buttonStartY = bookY + 31;
        int buttonIndex = 0;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 4; col++) {
                if (buttonIndex < recipeButtons.size()) {
                    ResearchRecipeButton button = recipeButtons.get(buttonIndex);
                    button.setPosition(buttonStartX + col * 25, buttonStartY + row * 25);
                    buttonIndex++;
                }
            }
        }
    }

    /**
     * Updates all recipe collections with current unlocked recipes
     */
    public void updateRecipeCollections() {
        if (player == null || menu == null) {
            return;
        }

        // Get all whitelisted recipe IDs from RecipeHelper
        List<RecipeHolder<ResearchCraftingRecipe>> allResearchRecipes = new ArrayList<>();

        // For now, we'll populate recipes when the menu is updated
        // The client doesn't have easy access to server recipe manager
        // We'll need to pass recipes through the menu or use network sync later

        // TODO: Implement proper recipe syncing from server

        // Update each category collection
        for (ResearchRecipeCollection collection : recipeCollections.values()) {
            collection.updateUnlockedRecipes(allResearchRecipes, player);
        }

        updateRecipeButtons();
    }

    /**
     * Updates the recipe buttons based on current category and page
     */
    private void updateRecipeButtons() {
        ResearchRecipeCollection collection = recipeCollections.get(currentCategory);
        List<RecipeHolder<ResearchCraftingRecipe>> recipes = collection.getRecipes();

        // Calculate pagination
        totalPages = (int) Math.ceil(recipes.size() / (double) RECIPES_PER_PAGE);
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }

        // Update button visibility and recipes
        int startIndex = currentPage * RECIPES_PER_PAGE;
        for (int i = 0; i < recipeButtons.size(); i++) {
            ResearchRecipeButton button = recipeButtons.get(i);
            int recipeIndex = startIndex + i;

            if (recipeIndex < recipes.size()) {
                RecipeHolder<ResearchCraftingRecipe> recipe = recipes.get(recipeIndex);
                button.setRecipe(recipe);
                button.setSelected(recipe.equals(selectedRecipe));
            } else {
                button.setRecipe(null);
            }
        }
    }

    /**
     * Sets whether the recipe book is visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            updateRecipeCollections();
        }
    }

    /**
     * Toggles the recipe book visibility
     */
    public void toggleVisibility() {
        setVisible(!visible);
    }

    /**
     * Checks if the recipe book is visible
     */
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }

        int bookX = leftPos - BOOK_WIDTH + 4;
        int bookY = topPos;

        // Render recipe book background
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE,
            bookX, bookY, 1, 1, BOOK_WIDTH, BOOK_HEIGHT, 256, 256);

        // Render category tabs (simplified for now)
        // TODO: Add proper tab rendering and switching

        // Render recipe buttons
        for (ResearchRecipeButton button : recipeButtons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Render page info if multiple pages
        if (totalPages > 1) {
            Component pageText = Component.literal((currentPage + 1) + "/" + totalPages);
            guiGraphics.drawString(minecraft.font, pageText,
                bookX + BOOK_WIDTH / 2 - minecraft.font.width(pageText) / 2,
                bookY + BOOK_HEIGHT - 15, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }

        // Check if any recipe button was clicked
        for (ResearchRecipeButton recipeButton : recipeButtons) {
            if (recipeButton.isHoveredOrFocused() && recipeButton.getRecipe() != null) {
                onRecipeClicked(recipeButton.getRecipe());
                return true;
            }
        }

        return false;
    }

    /**
     * Called when a recipe button is clicked
     */
    private void onRecipeClicked(RecipeHolder<ResearchCraftingRecipe> recipe) {
        this.selectedRecipe = recipe;
        updateRecipeButtons();

        // TODO: Send packet to server to place recipe items
        // TODO: Show ghost items in crafting grid
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible || totalPages <= 1) {
            return false;
        }

        // Check if mouse is over the recipe book area
        int bookX = leftPos - BOOK_WIDTH + 4;
        int bookY = topPos;
        if (mouseX >= bookX && mouseX < bookX + BOOK_WIDTH &&
            mouseY >= bookY && mouseY < bookY + BOOK_HEIGHT) {

            if (scrollY > 0 && currentPage > 0) {
                currentPage--;
                updateRecipeButtons();
                return true;
            } else if (scrollY < 0 && currentPage < totalPages - 1) {
                currentPage++;
                updateRecipeButtons();
                return true;
            }
        }

        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        // Not needed for now
    }

    @Override
    public boolean isFocused() {
        return visible;
    }
}
