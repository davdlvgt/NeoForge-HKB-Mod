package de.davidvogt.hkbmod.screen.cutsom;

import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.network.ResearchActionPacket;
import de.davidvogt.hkbmod.network.SetSelectedLevelPacket;
import de.davidvogt.hkbmod.research.PlayerResearchData;
import de.davidvogt.hkbmod.research.Research;
import de.davidvogt.hkbmod.research.ResearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.ArrayList;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    public static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "textures/gui/research_table/research_table_gui.png");

    public ResearchTableScreen(ResearchTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 170;
    }

    // Scrollbare Button-Liste für den gewünschten Bereich
    private final List<Research> currentResearches = new ArrayList<>();
    private String selectedClass = "knight"; // Default class
    private int scrollOffset = 0;
    private static final int SCROLL_BUTTON_HEIGHT = 13;
    private static final int SCROLL_BUTTON_WIDTH = 106; // Platz für Scrollbar rechts lassen
    private static final int SCROLL_AREA_X = 119;
    private static final int SCROLL_AREA_Y = 15;
    private static final int SCROLL_AREA_WIDTH = 130;
    private static final int SCROLL_AREA_HEIGHT = 56;
    private static final int SCROLL_MAX_VISIBLE = 4; // Genau 4 Buttons anzeigen

    private Button scrollUpButton;
    private Button scrollDownButton;
    private Button startResearchButton;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE,
                x, y,          // Position im Fenster
                0, 0,          // Start in der Textur
                imageWidth, imageHeight, // Wieviel aus der Textur
                256, 170);     // Texturgröße

        // Neuer Bereich für Scrollbar und Buttons
        int rectX1 = x + SCROLL_AREA_X;
        int rectY1 = y + SCROLL_AREA_Y;
        int rectX2 = rectX1 + SCROLL_AREA_WIDTH;
        int rectY2 = rectY1 + SCROLL_AREA_HEIGHT;
        guiGraphics.fill(rectX1, rectY1, rectX2, rectY2, 0xFF000000); // ganz schwarz, volle Deckkraft

        int rectX3 = x + 177;
        int rectY3 = y + 85;
        int rectX4 = x + 249;
        int rectY4 = y + 138;
        guiGraphics.fill(rectX3, rectY3, rectX4, rectY4, 0x88000000); // ganz schwarz, volle Deckkraft


        // Progressbar für Research
         int barX = x + 177;
         int barY = y + 142;
         int barWidth = 72;
         int barHeight = 2;

         // Progress aus BlockEntity holen
         float progress = menu.blockEntity.getResearchProgress();

         int filled = (int)(progress * barWidth);
         guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF222222);
         if (filled > 0) {
             guiGraphics.fill(barX, barY, barX + filled, barY + barHeight, 0xFF00FF00);
         }
    }

    @Override
    protected void init() {
        super.init();
        // Beispiel-Button: Position und Größe ggf. anpassen
        this.addRenderableWidget(Button.builder(
                        Component.literal("Knight"),
                        btn -> loadClassResearches("knight")
                )
                .bounds(this.leftPos + 69, this.topPos + 16, 42, 12)
                .build()
        );
        this.addRenderableWidget(Button.builder(
                        Component.literal("Archer"),
                        btn -> loadClassResearches("archer")
                )
                .bounds(this.leftPos + 69, this.topPos + 30, 42, 12)
                .build()
        );
        this.addRenderableWidget(Button.builder(
                        Component.literal("Cavalier"),
                        btn -> loadClassResearches("cavalier")
                )
                .bounds(this.leftPos + 69, this.topPos + 44, 42, 12)
                .build()
        );
        this.addRenderableWidget(Button.builder(
                        Component.literal("Magician"),
                        btn -> loadClassResearches("magician")
                )
                .bounds(this.leftPos + 69, this.topPos + 58, 42, 12)
                .build()
        );
        startResearchButton = Button.builder(
                        Component.literal("Start Research"),
                        btn -> {
                            if (menu.blockEntity.isResearching()) {
                                // Send cancel packet to server
                                HKBMod.LOGGER.info("CLIENT: Sending cancel research packet");
                                Minecraft.getInstance().getConnection().send(new ResearchActionPacket(
                                    menu.blockEntity.getBlockPos(),
                                    ResearchActionPacket.Action.CANCEL
                                ));
                            } else if (canStartResearch()) {
                                // Send start packet to server
                                HKBMod.LOGGER.info("CLIENT: Sending start research packet for level {} class {}",
                                    menu.blockEntity.getSelectedLevelIndex(), menu.blockEntity.getSelectedClass());
                                Minecraft.getInstance().getConnection().send(new ResearchActionPacket(
                                    menu.blockEntity.getBlockPos(),
                                    ResearchActionPacket.Action.START
                                ));
                            } else {
                                HKBMod.LOGGER.info("CLIENT: Cannot start research - requirements not met");
                            }
                        }
                )
                .bounds(this.leftPos + 177, this.topPos + 144, 72, 15)
                .build();
        this.addRenderableWidget(startResearchButton);

        // Load initial class researches
        loadClassResearches(selectedClass);
        // Scroll-Buttons hinzufügen (rechts im scrollbaren Bereich)
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int areaX1 = x + SCROLL_AREA_X;
        int areaY1 = y + SCROLL_AREA_Y;
        int btnW = 10;
        int btnH = 10;
        // Buttons rechts positionieren, damit sie die Liste nicht überlappen
        int scrollButtonX = areaX1 + SCROLL_AREA_WIDTH - btnW - 4;
        scrollUpButton = Button.builder(Component.literal("▲"), btn -> {
            scrollOffset--;
            if (scrollOffset < 0) scrollOffset = 0;
        }).bounds(scrollButtonX, areaY1 + 2, btnW, btnH).build();
        scrollDownButton = Button.builder(Component.literal("▼"), btn -> {
            int maxOffset = Math.max(0, currentResearches.size() - SCROLL_MAX_VISIBLE);
            scrollOffset++;
            if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        }).bounds(scrollButtonX, areaY1 + SCROLL_AREA_HEIGHT - btnH - 2, btnW, btnH).build();
        this.addRenderableWidget(scrollUpButton);
        this.addRenderableWidget(scrollDownButton);
    }

    private void loadClassResearches(String classType) {
        selectedClass = classType;
        currentResearches.clear();
        currentResearches.addAll(ResearchManager.getResearchForClass(classType));
        scrollOffset = 0;
        System.out.println("Loaded " + currentResearches.size() + " researches for class: " + classType);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        System.out.println("mouseScrolled ausgelöst: " + mouseX + ", " + mouseY + ", deltaY=" + deltaY);
        int guiX = (width - imageWidth) / 2;
        int guiY = (height - imageHeight) / 2;
        int areaX1 = guiX + SCROLL_AREA_X;
        int areaY1 = guiY + SCROLL_AREA_Y;
        int areaX2 = areaX1 + SCROLL_AREA_WIDTH;
        int areaY2 = areaY1 + SCROLL_AREA_HEIGHT;
        if (mouseX >= areaX1 && mouseX <= areaX2 && mouseY >= areaY1 && mouseY <= areaY2) {
            int maxOffset = Math.max(0, currentResearches.size() - SCROLL_MAX_VISIBLE);
            scrollOffset -= (int) Math.signum(deltaY);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxOffset) scrollOffset = maxOffset;
            System.out.println("ScrollOffset: " + scrollOffset + "/" + maxOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int areaX1 = x + SCROLL_AREA_X;
        int areaY1 = y + SCROLL_AREA_Y;
        for (int i = 0; i < SCROLL_MAX_VISIBLE; i++) {
            int idx = i + scrollOffset;
            if (idx >= currentResearches.size()) break;
            int btnX1 = areaX1 + 4;
            int btnY1 = areaY1 + i * SCROLL_BUTTON_HEIGHT + 4;
            int btnX2 = btnX1 + SCROLL_BUTTON_WIDTH;
            int btnY2 = btnY1 + SCROLL_BUTTON_HEIGHT - 2;
            if (mouseX >= btnX1 && mouseX <= btnX2 && mouseY >= btnY1 && mouseY <= btnY2) {
                Research research = currentResearches.get(idx);

                // Only allow selection of available (not completed, unlocked) research
                PlayerResearchData researchData = menu.getPlayerResearchData();
                if (!researchData.canResearch(research.classType(), research.level())) {
                    return true; // Prevent selection but consume click
                }

                // Update client-side
                menu.blockEntity.setSelectedLevel(research.level(), research.classType());
                // Send to server
                Minecraft.getInstance().getConnection().send(new SetSelectedLevelPacket(
                    menu.blockEntity.getBlockPos(),
                    research.level(),
                    research.classType()
                ));
                System.out.println("Button geklickt: " + research.displayName());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Update Start Research Button
        updateStartResearchButton();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int areaX1 = x + SCROLL_AREA_X;
        int areaY1 = y + SCROLL_AREA_Y;

        // Buttons zeichnen mit hover-Effekt
        for (int i = 0; i < SCROLL_MAX_VISIBLE; i++) {
            int idx = i + scrollOffset;
            if (idx >= currentResearches.size()) break;

            Research research = currentResearches.get(idx);
            int btnX = areaX1 + 4;
            int btnY = areaY1 + i * SCROLL_BUTTON_HEIGHT + 4;
            int btnW = SCROLL_BUTTON_WIDTH;
            int btnH = SCROLL_BUTTON_HEIGHT - 2;

            // Get player research data
            PlayerResearchData researchData = menu.getPlayerResearchData();
            boolean isCompleted = researchData.isLevelCompleted(research.classType(), research.level());
            boolean canResearch = researchData.canResearch(research.classType(), research.level());

            // Prüfen ob Maus über Button ist
            boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnW &&
                               mouseY >= btnY && mouseY <= btnY + btnH;

            // Button Hintergrund - different colors based on state
            boolean isSelected = (research.level() == menu.blockEntity.getSelectedLevelIndex());
            int bgColor;
            if (isCompleted) {
                bgColor = 0xFF447744; // Green for completed
            } else if (!canResearch) {
                bgColor = 0xFF444444; // Gray for locked
            } else if (isSelected) {
                bgColor = 0xFF557755; // Light green for selected
            } else if (isHovered) {
                bgColor = 0xFF666666; // Light gray for hovered
            } else {
                bgColor = 0xFF444444; // Dark gray for default
            }
            guiGraphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, bgColor);

            // Button Rand
            guiGraphics.fill(btnX, btnY, btnX + btnW, btnY + 1, 0xFF888888); // oben
            guiGraphics.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0xFF222222); // unten

            // Text zeichnen - linksbündig statt zentriert für bessere Lesbarkeit
            String text = research.displayName();
            if (isCompleted) {
                text = "✓ " + text; // Add checkmark for completed
            } else if (!canResearch) {
                text = "🔒 " + text; // Add lock for unavailable
            }
            int textX = btnX + 4; // 4 Pixel Abstand vom linken Rand
            int textY = btnY + (btnH - this.font.lineHeight) / 2 + 1;
            int textColor = canResearch ? 0xFFFFFFFF : 0xFF888888;
            guiGraphics.drawString(this.font, text, textX, textY, textColor, false);
        }

        // Scrollbar zeichnen, falls nötig (zwischen den Scroll-Buttons)
        if (currentResearches.size() > SCROLL_MAX_VISIBLE) {
            int scrollbarX = areaX1 + SCROLL_AREA_WIDTH - 8;
            int scrollbarWidth = 4;
            int scrollbarTop = areaY1 + 14; // Nach dem Up-Button
            int scrollbarBottom = areaY1 + SCROLL_AREA_HEIGHT - 14; // Vor dem Down-Button
            int scrollbarHeight = scrollbarBottom - scrollbarTop;

            // Hintergrund der Scrollbar
            guiGraphics.fill(scrollbarX, scrollbarTop, scrollbarX + scrollbarWidth, scrollbarBottom, 0xFF222222);

            // Scrollbar-Thumb
            int barHeight = Math.max(10, (int) (scrollbarHeight * (SCROLL_MAX_VISIBLE / (float) currentResearches.size())));
            int maxOffset = currentResearches.size() - SCROLL_MAX_VISIBLE;
            int barY = scrollbarTop + (int) (scrollOffset * (scrollbarHeight - barHeight) / (float) maxOffset);
            guiGraphics.fill(scrollbarX, barY, scrollbarX + scrollbarWidth, barY + barHeight, 0xFF888888);
        }

        // Requirements-Text anzeigen, wenn ein Level ausgewählt ist
        int selectedLevelIndex = menu.blockEntity.getSelectedLevelIndex();
        Research selectedResearch = currentResearches.stream()
                .filter(r -> r.level() == selectedLevelIndex)
                .findFirst()
                .orElse(null);

        if (selectedResearch != null) {
            int reqX = x + 177 + 4;
            int reqY = y + 85 + 4;
            int lineHeight = this.font.lineHeight;

            // Display requirements
            guiGraphics.drawString(this.font, "Requirements:", reqX, reqY, 0xFFFFFFFF, false);
            int yOffset = lineHeight;
            for (Research.ItemRequirement req : selectedResearch.requirements()) {
                String text = "- " + req.count() + "x " + req.item();
                guiGraphics.drawString(this.font, text, reqX, reqY + yOffset, 0xFFFFFFFF, false);
                yOffset += lineHeight;
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }


    private boolean canStartResearch() {
        // Level muss ausgewählt sein
        if (menu.blockEntity.getSelectedLevelIndex() < 0) {
            HKBMod.LOGGER.info("CLIENT: Cannot start - no level selected");
            return false;
        }

        // Darf nicht bereits am Forschen sein
        if (menu.blockEntity.isResearching()) {
            HKBMod.LOGGER.info("CLIENT: Cannot start - already researching");
            return false;
        }

        // Prüfe ob benötigte Materialien in den Slots vorhanden sind
        boolean hasMaterials = menu.blockEntity.hasRequiredMaterials(menu.blockEntity.getSelectedLevelIndex());
        HKBMod.LOGGER.info("CLIENT: canStartResearch - level: {}, class: {}, hasMaterials: {}",
            menu.blockEntity.getSelectedLevelIndex(),
            menu.blockEntity.getSelectedClass(),
            hasMaterials);
        return hasMaterials;
    }

    private void updateStartResearchButton() {
        if (startResearchButton == null) return;

        if (menu.blockEntity.isResearching()) {
            startResearchButton.setMessage(Component.literal("Cancel Research"));
            startResearchButton.active = true;
        } else {
            startResearchButton.setMessage(Component.literal("Start Research"));
            startResearchButton.active = canStartResearch();
        }
    }
}
