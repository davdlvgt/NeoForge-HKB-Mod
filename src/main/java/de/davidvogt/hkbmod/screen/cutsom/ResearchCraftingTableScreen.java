package de.davidvogt.hkbmod.screen.cutsom;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.screen.cutsom.recipebook.ResearchRecipeBookComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

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
        int buttonX = this.leftPos + 5;
        int buttonY = this.topPos + 5;

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
            this.recipeBookButton.setPosition(this.leftPos + 5, this.topPos + 5);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Update recipe book collections periodically
        if (this.recipeBook.isVisible()) {
            this.recipeBook.updateRecipeCollections();
        }
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
            return true;
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
}
