package de.davidvogt.hkbmod.screen.cutsom.recipebook;

import com.mojang.blaze3d.systems.RenderSystem;
import de.davidvogt.hkbmod.recipe.ResearchCraftingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * A button that displays a single research recipe in the recipe book.
 * Shows the recipe's result item and highlights when selected.
 */
public class ResearchRecipeButton extends AbstractWidget {
    private static final ResourceLocation RECIPE_BOOK_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");

    private static final int BUTTON_WIDTH = 25;
    private static final int BUTTON_HEIGHT = 25;

    private RecipeHolder<ResearchCraftingRecipe> recipe;
    private boolean selected = false;

    public ResearchRecipeButton() {
        super(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty());
    }

    /**
     * Sets the recipe this button represents
     */
    public void setRecipe(RecipeHolder<ResearchCraftingRecipe> recipe) {
        this.recipe = recipe;
        this.visible = recipe != null;
    }

    /**
     * Gets the recipe this button represents
     */
    public RecipeHolder<ResearchCraftingRecipe> getRecipe() {
        return recipe;
    }

    /**
     * Sets whether this button is selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Checks if this button is selected
     */
    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || recipe == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        // Render button background
        int u = 0;
        int v = 166;

        if (this.selected) {
            u += 25; // Selected texture offset
        } else if (this.isHovered) {
            u += 25; // Hovered texture offset
        }

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE,
            this.getX(), this.getY(), u, v, BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);

        // Render the result item
        ItemStack result = recipe.value().getResultItem();
        guiGraphics.renderItem(result, this.getX() + 5, this.getY() + 5);
        guiGraphics.renderItemDecorations(minecraft.font, result, this.getX() + 5, this.getY() + 5);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // Will be handled by the parent component
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        if (recipe != null) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }

    @Override
    public boolean isHoveredOrFocused() {
        return this.visible && super.isHoveredOrFocused();
    }
}
