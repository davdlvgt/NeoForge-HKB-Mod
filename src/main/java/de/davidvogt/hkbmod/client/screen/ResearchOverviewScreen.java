package de.davidvogt.hkbmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.attachment.ModAttachments;
import de.davidvogt.hkbmod.research.PlayerResearchData;
import de.davidvogt.hkbmod.research.Research;
import de.davidvogt.hkbmod.research.ResearchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResearchOverviewScreen extends Screen {
    private static final int NODE_WIDTH = 60;
    private static final int NODE_HEIGHT = 15;
    private static final int NODE_SPACING_X = 10;
    private static final int NODE_SPACING_Y = 30;
    private static final int CLASS_COLUMN_WIDTH = 75;
    private static final int LEFT_MARGIN = 10;
    private static final int TOP_MARGIN = 10;
    private static final int BORDER_SIZE = 1;

    private final List<String> researchClasses;
    private final PlayerResearchData playerResearchData;
    private ResearchNode hoveredNode = null;

    // Scroll/pan offsets
    private double scrollX = 0;
    private double scrollY = 0;
    private boolean isDragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public ResearchOverviewScreen() {
        super(Component.translatable("screen.hkbmod.research_overview"));
        this.researchClasses = ResearchManager.getAllClasses();
        this.researchClasses.sort(String::compareTo);

        // Get player research data from the client player
        if (Minecraft.getInstance().player != null) {
            this.playerResearchData = Minecraft.getInstance().player.getData(ModAttachments.PLAYER_RESEARCH);
        } else {
            this.playerResearchData = new PlayerResearchData();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left mouse button
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            double deltaX = mouseX - lastMouseX;
            double deltaY = mouseY - lastMouseY;

            scrollX += deltaX;
            scrollY += deltaY;

            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Vertical scrolling
        scrollY += deltaY * 10;

        // Horizontal scrolling with shift key
        if (Screen.hasShiftDown()) {
            scrollX += deltaY * 10;
        }

        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw border around the screen
        drawBorder(guiGraphics);

        // Render title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // Enable scissor test to clip content to border area
        guiGraphics.enableScissor(BORDER_SIZE, TOP_MARGIN, this.width - BORDER_SIZE, this.height - BORDER_SIZE);

        hoveredNode = null;

        // Render each research class
        for (int classIndex = 0; classIndex < researchClasses.size(); classIndex++) {
            String researchClass = researchClasses.get(classIndex);
            List<Research> researches = ResearchManager.getResearchForClass(researchClass);

            int classY = (int)(TOP_MARGIN + classIndex * NODE_SPACING_Y + scrollY);

            // Render class name on the left
            int classNameX = (int)(LEFT_MARGIN + scrollX);
            guiGraphics.drawString(this.font, formatClassName(researchClass), classNameX, classY + 10, 0xFFFFFF);

            // Render research nodes for this class
            for (int i = 0; i < researches.size(); i++) {
                Research research = researches.get(i);
                int nodeX = (int)(LEFT_MARGIN + CLASS_COLUMN_WIDTH + i * (NODE_WIDTH + NODE_SPACING_X) + scrollX);
                int nodeY = classY;

                // Draw connections to prerequisites first
                drawConnectionsToPrerequisites(guiGraphics, research, nodeX, nodeY, classIndex);

                // Determine node state
                ResearchNodeState state = getNodeState(research);

                // Draw the node
                drawResearchNode(guiGraphics, research, nodeX, nodeY, state, mouseX, mouseY);
            }
        }

        // Disable scissor test
        guiGraphics.disableScissor();

        // Render tooltip if hovering over a node (after scissor is disabled)
        if (hoveredNode != null) {
            renderNodeTooltip(guiGraphics, hoveredNode, mouseX, mouseY);
        }
    }

    private void drawBorder(GuiGraphics guiGraphics) {
        int borderColor = 0xFF8B8B8B;
        int backgroundColor = 0xFF2B2B2B;

        // Draw outer border
        guiGraphics.fill(0, 0, this.width, BORDER_SIZE, borderColor); // Top
        guiGraphics.fill(0, this.height - BORDER_SIZE, this.width, this.height, borderColor); // Bottom
        guiGraphics.fill(0, 0, BORDER_SIZE, this.height, borderColor); // Left
        guiGraphics.fill(this.width - BORDER_SIZE, 0, this.width, this.height, borderColor); // Right

        // Draw inner darker area for title bar
        guiGraphics.fill(BORDER_SIZE, BORDER_SIZE, this.width - BORDER_SIZE, TOP_MARGIN - 5, backgroundColor);
    }

    private void drawResearchNode(GuiGraphics guiGraphics, Research research, int x, int y,
                                   ResearchNodeState state, int mouseX, int mouseY) {
        // Check if mouse is hovering over this node
        boolean isHovered = mouseX >= x && mouseX <= x + NODE_WIDTH &&
                           mouseY >= y && mouseY <= y + NODE_HEIGHT;

        if (isHovered) {
            hoveredNode = new ResearchNode(research, x, y);
        }

        // Draw node background based on state
        int backgroundColor = switch (state) {
            case RESEARCHED -> 0xFF006600; // Dark green
            case RESEARCHABLE -> 0xFF0066CC; // Blue
            case NOT_RESEARCHABLE -> 0xFF666666; // Gray
        };

        // Draw background
        guiGraphics.fill(x, y, x + NODE_WIDTH, y + NODE_HEIGHT, backgroundColor);

        // Draw border (brighter when hovered)
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF333333;
        guiGraphics.fill(x, y, x + NODE_WIDTH, y + 1, borderColor); // Top
        guiGraphics.fill(x, y + NODE_HEIGHT - 1, x + NODE_WIDTH, y + NODE_HEIGHT, borderColor); // Bottom
        guiGraphics.fill(x, y, x + 1, y + NODE_HEIGHT, borderColor); // Left
        guiGraphics.fill(x + NODE_WIDTH - 1, y, x + NODE_WIDTH, y + NODE_HEIGHT, borderColor); // Right

        // Draw node text (remove "Level X:" prefix)
        String displayText = removeLevePrefix(research.displayName());
        guiGraphics.drawString(this.font, displayText, x + 5, y + 10, 0xFFFFFF);
    }

    private void drawConnectionsToPrerequisites(GuiGraphics guiGraphics, Research research,
                                                 int nodeX, int nodeY, int classIndex) {
        if (research.prerequisites() == null || research.prerequisites().isEmpty()) {
            return;
        }

        for (Research.ResearchPrerequisite prereq : research.prerequisites()) {
            // Find the prerequisite node position
            int prereqClassIndex = researchClasses.indexOf(prereq.classType());
            if (prereqClassIndex == -1) continue;

            Research prereqResearch = ResearchManager.getResearch(prereq.classType(), prereq.level());
            if (prereqResearch == null) continue;

            List<Research> prereqClassResearches = ResearchManager.getResearchForClass(prereq.classType());
            int prereqIndex = prereqClassResearches.indexOf(prereqResearch);
            if (prereqIndex == -1) continue;

            int prereqX = (int)(LEFT_MARGIN + CLASS_COLUMN_WIDTH + prereqIndex * (NODE_WIDTH + NODE_SPACING_X) + scrollX);
            int prereqY = (int)(TOP_MARGIN + prereqClassIndex * NODE_SPACING_Y + scrollY);

            // Calculate connection points
            int startX = prereqX + NODE_WIDTH;
            int startY = prereqY + NODE_HEIGHT / 2;
            int endX = nodeX;
            int endY = nodeY + NODE_HEIGHT / 2;

            // Draw line
            int lineColor = playerResearchData.isLevelCompleted(prereq.classType(), prereq.level())
                           ? 0xFF00FF00 // Green if prerequisite is completed
                           : 0xFF888888; // Gray if not completed

            drawLine(guiGraphics, startX, startY, endX, endY, lineColor);
        }
    }

    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // Simple line drawing using fill for horizontal/vertical lines
        // For diagonal lines, we'll draw a simple approximation
        if (y1 == y2) {
            // Horizontal line
            guiGraphics.fill(x1, y1, x2, y1 + 1, color);
        } else if (x1 == x2) {
            // Vertical line
            guiGraphics.fill(x1, y1, x1 + 1, y2, color);
        } else {
            // Diagonal line - draw using small segments
            int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
            for (int i = 0; i <= steps; i++) {
                int x = x1 + (x2 - x1) * i / steps;
                int y = y1 + (y2 - y1) * i / steps;
                guiGraphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private ResearchNodeState getNodeState(Research research) {
        if (playerResearchData.isLevelCompleted(research.classType(), research.level())) {
            return ResearchNodeState.RESEARCHED;
        }

        // Check if all prerequisites are met
        boolean canResearch = playerResearchData.canResearch(research.classType(), research.level(), research);
        return canResearch ? ResearchNodeState.RESEARCHABLE : ResearchNodeState.NOT_RESEARCHABLE;
    }

    private void renderNodeTooltip(GuiGraphics guiGraphics, ResearchNode node, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        Research research = node.research;

        // Title
        tooltip.add(Component.literal(research.displayName()).withStyle(style -> style.withBold(true)));

        // Description
        if (research.description() != null && !research.description().isEmpty()) {
            tooltip.add(Component.literal(research.description()).withStyle(style -> style.withItalic(true)));
        }

        // Requirements
        if (!research.requirements().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Requirements:").withStyle(style -> style.withBold(true)));
            for (Research.ItemRequirement req : research.requirements()) {
                tooltip.add(Component.literal("  • " + req.count() + "x " + req.item()));
            }
        }

        // Prerequisites
        if (research.prerequisites() != null && !research.prerequisites().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Prerequisites:").withStyle(style -> style.withBold(true)));
            for (Research.ResearchPrerequisite prereq : research.prerequisites()) {
                Research prereqResearch = ResearchManager.getResearch(prereq.classType(), prereq.level());
                if (prereqResearch != null) {
                    boolean completed = playerResearchData.isLevelCompleted(prereq.classType(), prereq.level());
                    String status = completed ? "✓" : "✗";
                    tooltip.add(Component.literal("  " + status + " " + prereqResearch.displayName()));
                }
            }
        }

        // Unlocks
        if (research.unlocks() != null && !research.unlocks().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Unlocks:").withStyle(style -> style.withBold(true)));
            for (String unlock : research.unlocks()) {
                tooltip.add(Component.literal("  • " + unlock));
            }
        }

        // Convert Components to FormattedCharSequence
        List<net.minecraft.util.FormattedCharSequence> tooltipLines = tooltip.stream()
                .map(component -> component.getVisualOrderText())
                .toList();
        guiGraphics.setTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
    }

    private String removeLevePrefix(String displayName) {
        // Remove "Level X: " from the display name
        if (displayName.matches("Level \\d+: .*")) {
            return displayName.substring(displayName.indexOf(": ") + 2);
        }
        return displayName;
    }

    private String formatClassName(String className) {
        // Capitalize first letter
        return className.substring(0, 1).toUpperCase() + className.substring(1);
    }

    private enum ResearchNodeState {
        RESEARCHED,
        RESEARCHABLE,
        NOT_RESEARCHABLE
    }

    private record ResearchNode(Research research, int x, int y) {}
}
