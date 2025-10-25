package de.davidvogt.hkbmod.client.model;

import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 3D Model for the Deer entity
 * Defines the geometry and animations for the deer
 */
public class DeerModel extends EntityModel<LivingEntityRenderState> {

    // Model layer location for registration
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "deer"), "main");

    // Model parts
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;

    public DeerModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.legBackRight = root.getChild("leg_back_right");
    }

    /**
     * Creates the layer definition (model geometry)
     * This defines the shape and structure of the deer
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        // Body - slightly longer and narrower than a cow
        PartDefinition body = partDefinition.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -7.0F, 8.0F, 8.0F, 14.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));

        // Head - positioned at the front of the body
        PartDefinition head = partDefinition.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 22)
                        .addBox(-3.0F, -4.0F, -6.0F, 6.0F, 6.0F, 6.0F)
                        // Snout/nose
                        .texOffs(24, 22)
                        .addBox(-2.0F, -1.0F, -8.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 7.0F, -7.0F));

        // Left Antler - Complex branching structure
        PartDefinition antlerLeft = head.addOrReplaceChild("antler_left",
                CubeListBuilder.create()
                        // Main beam (vertical)
                        .texOffs(0, 34)
                        .addBox(0.0F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F)
                        // First tine (lower, angled forward)
                        .texOffs(0, 34)
                        .addBox(0.0F, -3.0F, -2.5F, 1.0F, 2.0F, 1.0F)
                        // Second tine (middle, angled back)
                        .texOffs(0, 34)
                        .addBox(0.0F, -4.5F, 0.5F, 1.0F, 2.0F, 1.0F)
                        // Third tine (upper, angled forward-up)
                        .texOffs(0, 34)
                        .addBox(0.0F, -5.5F, -1.5F, 1.0F, 1.5F, 1.0F)
                        // Top fork point
                        .texOffs(0, 34)
                        .addBox(-0.5F, -7.0F, -0.5F, 1.5F, 1.0F, 1.0F),
                PartPose.offset(2.0F, -4.0F, -2.0F));

        // Right Antler - Mirror of left with complex branching
        PartDefinition antlerRight = head.addOrReplaceChild("antler_right",
                CubeListBuilder.create()
                        // Main beam (vertical)
                        .texOffs(4, 34)
                        .addBox(-1.0F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F)
                        // First tine (lower, angled forward)
                        .texOffs(4, 34)
                        .addBox(-1.0F, -3.0F, -2.5F, 1.0F, 2.0F, 1.0F)
                        // Second tine (middle, angled back)
                        .texOffs(4, 34)
                        .addBox(-1.0F, -4.5F, 0.5F, 1.0F, 2.0F, 1.0F)
                        // Third tine (upper, angled forward-up)
                        .texOffs(4, 34)
                        .addBox(-1.0F, -5.5F, -1.5F, 1.0F, 1.5F, 1.0F)
                        // Top fork point
                        .texOffs(4, 34)
                        .addBox(-0.5F, -7.0F, -0.5F, 1.5F, 1.0F, 1.0F),
                PartPose.offset(-2.0F, -4.0F, -2.0F));

        // Front left leg - thin and tall
        PartDefinition legFrontLeft = partDefinition.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create()
                        .texOffs(30, 0)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offset(2.5F, 11.0F, -5.0F));

        // Front right leg
        PartDefinition legFrontRight = partDefinition.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create()
                        .texOffs(42, 0)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offset(-2.5F, 11.0F, -5.0F));

        // Back left leg
        PartDefinition legBackLeft = partDefinition.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create()
                        .texOffs(30, 16)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offset(2.5F, 11.0F, 5.0F));

        // Back right leg
        PartDefinition legBackRight = partDefinition.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create()
                        .texOffs(42, 16)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offset(-2.5F, 11.0F, 5.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    /**
     * Setup animations based on the render state
     * This animates the legs when walking
     */
    @Override
    public void setupAnim(LivingEntityRenderState state) {
        // Get the limb swing values for walking animation
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;

        // Animate the head to bob slightly when walking
        this.head.xRot = 0.0F;

        // Animate legs - swing back and forth when walking
        // Front legs swing opposite to back legs for natural walking motion
        this.legFrontLeft.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.legFrontRight.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.legBackLeft.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.legBackRight.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
    }
}
