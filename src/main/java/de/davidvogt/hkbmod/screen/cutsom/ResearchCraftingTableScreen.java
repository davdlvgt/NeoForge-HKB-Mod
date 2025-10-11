package de.davidvogt.hkbmod.screen.cutsom;

import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Research Crafting Table.
 * Displays a 3x3 crafting grid with result slot and player inventory.
 */
public class ResearchCraftingTableScreen extends AbstractContainerScreen<ResearchCraftingTableMenu> {
    // Use vanilla crafting table texture as a base
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/crafting_table.png");

    public ResearchCraftingTableScreen(ResearchCraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
