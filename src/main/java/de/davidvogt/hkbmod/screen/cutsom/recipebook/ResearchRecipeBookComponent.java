package de.davidvogt.hkbmod.screen.cutsom.recipebook;

import com.google.common.collect.Lists;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.network.PlaceRecipePacket;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import de.davidvogt.hkbmod.screen.cutsom.ResearchCraftingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;

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
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int RECIPES_PER_PAGE = 20; // 4x5 grid
    private final Map<ResearchRecipeCategory, ResearchRecipeCollection> recipeCollections = new EnumMap<>(ResearchRecipeCategory.class);
    private final List<ResearchRecipeButton> recipeButtons = Lists.newArrayList();
    private final List<RecipeHolder<ResearchCraftingRecipe>> cachedRecipes = new ArrayList<>();
    private final Map<ResearchRecipeCategory, CategoryTab> categoryTabs = new EnumMap<>(ResearchRecipeCategory.class);
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
    private boolean recipesLoaded = false;

    public ResearchRecipeBookComponent() {
        // Initialize recipe collections for each category
        for (ResearchRecipeCategory category : ResearchRecipeCategory.values()) {
            recipeCollections.put(category, new ResearchRecipeCollection(category));
        }

        // Initialize recipe buttons
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            recipeButtons.add(new ResearchRecipeButton());
        }

        // Initialize category tabs
        ResearchRecipeCategory[] categories = ResearchRecipeCategory.values();
        for (int i = 0; i < categories.length; i++) {
            categoryTabs.put(categories[i], new CategoryTab(categories[i], i));
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

        // Position category tabs at the top
        int tabStartX = bookX;
        int tabY = bookY;
        for (CategoryTab tab : categoryTabs.values()) {
            tab.updatePosition(tabStartX, tabY);
        }
    }

    /**
     * Sets the available recipes (called from screen after recipes are loaded)
     */
    public void setRecipes(List<RecipeHolder<ResearchCraftingRecipe>> recipes) {
        this.cachedRecipes.clear();
        this.cachedRecipes.addAll(recipes);
        this.recipesLoaded = true;
        updateRecipeCollections();
    }

    /**
     * Updates all recipe collections with current unlocked recipes
     */
    public void updateRecipeCollections() {
        if (player == null || !recipesLoaded) {
            return;
        }

        // Update each category collection with cached recipes
        for (ResearchRecipeCollection collection : recipeCollections.values()) {
            collection.updateUnlockedRecipes(cachedRecipes, player);
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

    /**
     * Sets whether the recipe book is visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            updateRecipeCollections();
        }
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

        // Render category tabs
        for (CategoryTab tab : categoryTabs.values()) {
            tab.render(guiGraphics, mouseX, mouseY, currentCategory);
        }

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

        // Check if any category tab was clicked
        for (CategoryTab tab : categoryTabs.values()) {
            if (tab.isMouseOver(mouseX, mouseY)) {
                if (currentCategory != tab.getCategory()) {
                    currentCategory = tab.getCategory();
                    currentPage = 0;
                    selectedRecipe = null;
                    updateRecipeButtons();
                }
                return true;
            }
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

        // Check if shift is held for "place all" behavior
        boolean placeAll = minecraft.options.keyShift.isDown();

        // Send packet to server to place recipe items in crafting grid
        ResourceLocation recipeId = recipe.id().location();
        HKBMod.LOGGER.info("Recipe clicked: {}, placeAll: {}", recipeId, placeAll);
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new PlaceRecipePacket(recipeId, placeAll));
            HKBMod.LOGGER.info("Sent PlaceRecipePacket to server");
        } else {
            HKBMod.LOGGER.warn("Cannot send packet - connection is null");
        }
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
    public boolean isFocused() {
        return visible;
    }

    @Override
    public void setFocused(boolean focused) {
        // Not needed for now
    }

    /**
     * Inner class representing a category tab
     */
    private static class CategoryTab {
        private final ResearchRecipeCategory category;
        private final int index;
        private int x;
        private int y;

        public CategoryTab(ResearchRecipeCategory category, int index) {
            this.category = category;
            this.index = index;
        }

        public void updatePosition(int bookX, int bookY) {
            // Tabs are positioned horizontally at the top of the book
            this.x = bookX + (index * TAB_WIDTH);
            this.y = bookY - TAB_HEIGHT + 3; // Slightly overlap with book
        }

        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, ResearchRecipeCategory currentCategory) {
            boolean isSelected = category == currentCategory;
            boolean isHovered = isMouseOver(mouseX, mouseY);

            // Texture coordinates for tabs
            int u = isSelected ? 153 : 181; // Selected tab has different texture
            int v = isHovered ? 32 : 0;

            // Render tab background
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE,
                    x, y, u, v, TAB_WIDTH, TAB_HEIGHT, 256, 256);

            // Render category icon
            guiGraphics.renderItem(category.getIcon(), x + 6, y + 9);
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + TAB_WIDTH &&
                    mouseY >= y && mouseY < y + TAB_HEIGHT;
        }

        public ResearchRecipeCategory getCategory() {
            return category;
        }
    }
}
