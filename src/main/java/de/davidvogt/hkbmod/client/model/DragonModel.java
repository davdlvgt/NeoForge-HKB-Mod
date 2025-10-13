package de.davidvogt.hkbmod.client.model;

import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;

public class DragonModel extends EntityModel<EnderDragonRenderState> {

    private static final int NECK_PART_COUNT = 5;
    private static final int TAIL_PART_COUNT = 12;

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "dragon"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] neckParts = new ModelPart[NECK_PART_COUNT];
    private final ModelPart[] tailParts = new ModelPart[TAIL_PART_COUNT];
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart leftFrontLeg;
    private final ModelPart leftFrontLegTip;
    private final ModelPart leftFrontFoot;
    private final ModelPart leftRearLeg;
    private final ModelPart leftRearLegTip;
    private final ModelPart leftRearFoot;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;
    private final ModelPart rightFrontLeg;
    private final ModelPart rightFrontLegTip;
    private final ModelPart rightFrontFoot;
    private final ModelPart rightRearLeg;
    private final ModelPart rightRearLegTip;
    private final ModelPart rightRearFoot;

    // Track movement speed for walking animation
    private float movementSpeed = 0.0F;

    /**
     * Sets the movement speed for the dragon, used to control walking animation
     */
    public void setMovementSpeed(float speed) {
        this.movementSpeed = speed;
    }

    public DragonModel(ModelPart root) {
        super(root);
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");

        for (int i = 0; i < NECK_PART_COUNT; i++) {
            neckParts[i] = root.getChild("neck" + i);
        }

        for (int i = 0; i < TAIL_PART_COUNT; i++) {
            tailParts[i] = root.getChild("tail" + i);
        }

        this.body = root.getChild("body");
        this.leftWing = body.getChild("left_wing");
        this.leftWingTip = leftWing.getChild("left_wing_tip");
        this.leftFrontLeg = body.getChild("left_front_leg");
        this.leftFrontLegTip = leftFrontLeg.getChild("left_front_leg_tip");
        this.leftFrontFoot = leftFrontLegTip.getChild("left_front_foot");
        this.leftRearLeg = body.getChild("left_hind_leg");
        this.leftRearLegTip = leftRearLeg.getChild("left_hind_leg_tip");
        this.leftRearFoot = leftRearLegTip.getChild("left_hind_foot");

        this.rightWing = body.getChild("right_wing");
        this.rightWingTip = rightWing.getChild("right_wing_tip");
        this.rightFrontLeg = body.getChild("right_front_leg");
        this.rightFrontLegTip = rightFrontLeg.getChild("right_front_leg_tip");
        this.rightFrontFoot = rightFrontLegTip.getChild("right_front_foot");
        this.rightRearLeg = body.getChild("right_hind_leg");
        this.rightRearLegTip = rightRearLeg.getChild("right_hind_leg_tip");
        this.rightRearFoot = rightRearLegTip.getChild("right_hind_foot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder neckTailCube = CubeListBuilder.create()
                .addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10)
                .addBox(-1.0F, -9.0F, -3.0F, 2, 4, 6);

        // Head
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .addBox("upperlip", -6.0F, -1.0F, -24.0F, 12, 5, 16)
                        .addBox("upperhead", -8.0F, -8.0F, -10.0F, 16, 16, 16)
                        .addBox("scale", -5.0F, -12.0F, -4.0F, 2, 4, 6)
                        .addBox("nostril", -5.0F, -3.0F, -22.0F, 2, 2, 4)
                        .addBox("scale", 3.0F, -12.0F, -4.0F, 2, 4, 6)
                        .addBox("nostril", 3.0F, -3.0F, -22.0F, 2, 2, 4),
                PartPose.offset(0.0F, 20.0F, -62.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .addBox("jaw", -6.0F, 0.0F, -16.0F, 12, 4, 16),
                PartPose.offset(0.0F, 4.0F, -8.0F));

        // Neck
        for (int i = 0; i < NECK_PART_COUNT; i++) {
            root.addOrReplaceChild("neck" + i, neckTailCube, PartPose.offset(0.0F, 20.0F, -12.0F - i * 10.0F));
        }

        // Tail
        for (int i = 0; i < TAIL_PART_COUNT; i++) {
            root.addOrReplaceChild("tail" + i, neckTailCube, PartPose.offset(0.0F, 10.0F, 60.0F + i * 10.0F));
        }

        // Body
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .addBox("body", -12.0F, 1.0F, -16.0F, 24, 24, 64),
                PartPose.offset(0.0F, 3.0F, 8.0F));

        // Wings
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().mirror()
                        .addBox("bone", 0.0F, -4.0F, -4.0F, 56, 8, 8)
                        .addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56),
                PartPose.offset(12.0F, 2.0F, -6.0F));
        leftWing.addOrReplaceChild("left_wing_tip",
                CubeListBuilder.create().mirror()
                        .addBox("bone", 0.0F, -2.0F, -2.0F, 56, 4, 4)
                        .addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56),
                PartPose.offset(56.0F, 0.0F, 0.0F));

        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .addBox("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8)
                        .addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56),
                PartPose.offset(-12.0F, 2.0F, -6.0F));
        rightWing.addOrReplaceChild("right_wing_tip",
                CubeListBuilder.create()
                        .addBox("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4)
                        .addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56),
                PartPose.offset(-56.0F, 0.0F, 0.0F));

        // Front Legs
        PartDefinition leftFrontLeg = body.addOrReplaceChild("left_front_leg",
                CubeListBuilder.create().mirror()
                        .addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8),
                PartPose.offset(12.0F, 16.0F, 8.0F));
        PartDefinition leftFrontLegTip = leftFrontLeg.addOrReplaceChild("left_front_leg_tip",
                CubeListBuilder.create().mirror()
                        .addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        leftFrontLegTip.addOrReplaceChild("left_front_foot",
                CubeListBuilder.create().mirror()
                        .addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition rightFrontLeg = body.addOrReplaceChild("right_front_leg",
                CubeListBuilder.create()
                        .addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8),
                PartPose.offset(-12.0F, 16.0F, 8.0F));
        PartDefinition rightFrontLegTip = rightFrontLeg.addOrReplaceChild("right_front_leg_tip",
                CubeListBuilder.create()
                        .addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        rightFrontLegTip.addOrReplaceChild("right_front_foot",
                CubeListBuilder.create()
                        .addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // Hind Legs
        PartDefinition leftHindLeg = body.addOrReplaceChild("left_hind_leg",
                CubeListBuilder.create().mirror()
                        .addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16),
                PartPose.offset(16.0F, 16.0F, 42.0F));
        PartDefinition leftHindLegTip = leftHindLeg.addOrReplaceChild("left_hind_leg_tip",
                CubeListBuilder.create().mirror()
                        .addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12),
                PartPose.offset(0.0F, 32.0F, 0.0F));
        leftHindLegTip.addOrReplaceChild("left_hind_foot",
                CubeListBuilder.create().mirror()
                        .addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24),
                PartPose.offset(0.0F, 32.0F, 0.0F));

        PartDefinition rightHindLeg = body.addOrReplaceChild("right_hind_leg",
                CubeListBuilder.create()
                        .addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16),
                PartPose.offset(-16.0F, 16.0F, 42.0F));
        PartDefinition rightHindLegTip = rightHindLeg.addOrReplaceChild("right_hind_leg_tip",
                CubeListBuilder.create()
                        .addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12),
                PartPose.offset(0.0F, 32.0F, 0.0F));
        rightHindLegTip.addOrReplaceChild("right_hind_foot",
                CubeListBuilder.create()
                        .addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24),
                PartPose.offset(0.0F, 32.0F, 0.0F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(EnderDragonRenderState state) {
        super.setupAnim(state);
        float flap = state.flapTime * ((float)Math.PI * 2F);
        this.jaw.xRot = (Mth.sin(flap) + 1.0F) * 0.2F;

        // Check if dragon is landed (flapTime is 0 or very small when landed)
        boolean isLanded = state.flapTime < 0.01F;

        if (isLanded) {
            // Wings folded tightly against body when landed
            // The wings should fold back along the dragon's sides
            this.leftWing.xRot = 0.2F;      // Slight upward tilt
            this.leftWing.yRot = -0.5F;      // No forward/back rotation
            this.leftWing.zRot = 0.6F;      // Fold IN toward body (positive for left wing)

            this.leftWingTip.xRot = 0.0F;
            this.leftWingTip.yRot = -0.5F;
            this.leftWingTip.zRot = 0.8F;   // Fold the tip even more inward

            this.rightWing.xRot = 0.2F;     // Slight upward tilt (same as left)
            this.rightWing.yRot = 0.5F;     // No forward/back rotation
            this.rightWing.zRot = -0.6F;    // Fold IN toward body (negative for right wing)

            this.rightWingTip.xRot = 0.0F;
            this.rightWingTip.yRot = 0.5F;
            this.rightWingTip.zRot = -0.8F; // Fold the tip even more inward

            // Log once every 40 ticks (2 seconds) when landed
            if (state.ageInTicks % 40 == 0) {
                System.out.println("[MODEL-CLIENT] Applying LANDED pose - wings folded, flapTime=" +
                    String.format("%.4f", state.flapTime));
            }
        } else {
            // Flügelbewegung (flying animation)
            this.leftWing.xRot = 0.125F - Mth.cos(flap) * 0.2F;
            this.leftWing.zRot = -(Mth.sin(flap) + 0.125F) * 0.8F;
            this.leftWingTip.zRot = (Mth.sin(flap + 2.0F) + 0.5F) * 0.75F;
            this.rightWing.xRot = leftWing.xRot;
            this.rightWing.zRot = -leftWing.zRot;
            this.rightWingTip.zRot = -leftWingTip.zRot;
        }
// Positionierung und Animation von Hals
        DragonFlightHistory.Sample neckBase = state.getHistoricalPos(6);
        float headYawOffset = Mth.wrapDegrees(state.getHistoricalPos(5).yRot() - state.getHistoricalPos(10).yRot());
        float headYawAvg = Mth.wrapDegrees(state.getHistoricalPos(5).yRot() + headYawOffset / 2.0F);

        float x = neckParts[0].x;
        float y = neckParts[0].y;
        float z = neckParts[0].z;
        float flapOffset;

        // Add head bobbing when landed and walking
        float headBob = 0.0F;
        if (isLanded && this.movementSpeed > 0.005F) {
            // Head bobs up and down while walking
            headBob = Mth.sin(state.ageInTicks * 0.2F) * 0.15F;
        }

        for (int i = 0; i < NECK_PART_COUNT; i++) {
            ModelPart neck = neckParts[i];
            DragonFlightHistory.Sample sample = state.getHistoricalPos(5 - i);
            flapOffset = Mth.cos(i * 0.45F + flap) * 0.15F;
            neck.yRot = Mth.wrapDegrees(sample.yRot() - neckBase.yRot()) * ((float)Math.PI / 180F) * 1.5F;
            neck.xRot = flapOffset + headBob + state.getHeadPartYOffset(i, neckBase, sample) * ((float)Math.PI / 180F) * 1.5F * 5.0F;
            neck.zRot = -Mth.wrapDegrees(sample.yRot() - headYawAvg) * ((float)Math.PI / 180F) * 1.5F;
            neck.x = x;
            neck.y = y;
            neck.z = z;

            x -= Mth.sin(neck.yRot) * Mth.cos(neck.xRot) * 10.0F;
            y += Mth.sin(neck.xRot) * 10.0F;
            z -= Mth.cos(neck.yRot) * Mth.cos(neck.xRot) * 10.0F;
        }

        head.x = x;
        head.y = y;
        head.z = z;
        DragonFlightHistory.Sample headSample = state.getHistoricalPos(0);
        head.yRot = Mth.wrapDegrees(headSample.yRot() - neckBase.yRot()) * ((float)Math.PI / 180F);
        head.xRot = headBob + Mth.wrapDegrees(state.getHeadPartYOffset(6, neckBase, headSample)) * ((float)Math.PI / 180F) * 1.5F * 5.0F;
        head.zRot = -Mth.wrapDegrees(headSample.yRot() - headYawAvg) * ((float)Math.PI / 180F);

// Drehung des Körpers
        body.zRot = -headYawOffset * 1.5F * ((float)Math.PI / 180F);

// Beine posieren - each side needs separate animation
        poseLimbsLeft(state, flap, leftFrontLeg, leftFrontLegTip, leftFrontFoot, leftRearLeg, leftRearLegTip, leftRearFoot);
        poseLimbsRight(state, flap, rightFrontLeg, rightFrontLegTip, rightFrontFoot, rightRearLeg, rightRearLegTip, rightRearFoot);

// Schwanzanimation
        float tailSwing = 0.0F;
        y = tailParts[0].y;
        z = tailParts[0].z;
        x = tailParts[0].x;
        neckBase = state.getHistoricalPos(11);

        // Add tail swaying left-right when landed
        float tailSway = 0.0F;
        if (isLanded) {
            // Tail sways side to side when standing or walking
            tailSway = Mth.sin(state.ageInTicks * 0.08F) * 0.3F;
        }

        for (int j = 0; j < TAIL_PART_COUNT; j++) {
            DragonFlightHistory.Sample sample = state.getHistoricalPos(12 + j);
            tailSwing += Mth.sin(j * 0.45F + flap) * 0.05F;
            ModelPart tail = tailParts[j];

            if (isLanded) {
                // When landed, use swaying animation instead of flight history
                // Progressive sway - each segment sways more than the previous one
                float segmentSway = tailSway * (j + 1) / (float)TAIL_PART_COUNT;
                tail.yRot = segmentSway + 180.0F * ((float)Math.PI / 180F);
                tail.xRot = tailSwing * 0.2F; // Minimal vertical movement
                tail.zRot = 0.0F;
            } else {
                // Flying animation (original)
                tail.yRot = (Mth.wrapDegrees(sample.yRot() - neckBase.yRot()) * 1.5F + 180.0F) * ((float)Math.PI / 180F);
                tail.xRot = tailSwing + (float)(sample.y() - neckBase.y()) * ((float)Math.PI / 180F) * 1.5F * 5.0F;
                tail.zRot = Mth.wrapDegrees(sample.yRot() - headYawAvg) * ((float)Math.PI / 180F) * 1.5F;
            }

            tail.x = x;
            tail.y = y;
            tail.z = z;

            y += Mth.sin(tail.xRot) * 10.0F;
            z -= Mth.cos(tail.yRot) * Mth.cos(tail.xRot) * 10.0F;
            x -= Mth.sin(tail.yRot) * Mth.cos(tail.xRot) * 10.0F;
        }

        // Hals- und Schwanzbewegung nach EnderDragonLogik
        // (Hier könntest du die restlichen Animationen genau wie im Original übernehmen)
    }

    /**
     * Animates the left side legs (left front and left rear)
     * Uses diagonal gait: left front moves with right rear
     */
    private void poseLimbsLeft(EnderDragonRenderState state, float flapAngle, ModelPart frontLeg, ModelPart frontLegTip, ModelPart frontFoot, ModelPart rearLeg, ModelPart rearLegTip, ModelPart rearFoot) {
        boolean isLanded = state.flapTime < 0.01F;

        if (isLanded) {
            // Use the movement speed passed from renderer
            // Only animate legs if actually moving (speed threshold of 0.005 blocks/tick)
            if (this.movementSpeed > 0.005F) {
                // Diagonal walking gait - left front moves with right rear
                float walkCycle = state.ageInTicks * 0.5F; // Slightly faster cycle

                // Scale animation by actual movement speed
                float animationScale = Math.min(this.movementSpeed * 3.0F, 1.0F);

                // Left front leg movement - increased swing amplitude
                float leftFrontSwing = Mth.cos(walkCycle) * 1.2F * animationScale;
                frontLeg.xRot = leftFrontSwing * 0.7F;
                frontLegTip.xRot = Math.max(0.0F, leftFrontSwing * 0.7F);
                frontFoot.xRot = Math.min(0.0F, -leftFrontSwing * 0.5F);

                // Left rear leg movement (SAME phase as left front for diagonal gait) - increased amplitude
                float leftRearSwing = Mth.cos(walkCycle) * 0.9F * animationScale;
                rearLeg.xRot = -0.15F + leftRearSwing * 0.5F;
                rearLegTip.xRot = 0.25F + Math.max(0.0F, leftRearSwing * 0.6F);
                rearFoot.xRot = -0.1F + Math.min(0.0F, -leftRearSwing * 0.4F);

                if (state.ageInTicks % 40 == 0) {
                    System.out.println("[MODEL-CLIENT] LEFT SIDE walking - speed=" + String.format("%.3f", this.movementSpeed) +
                        ", animScale=" + String.format("%.2f", animationScale));
                }
            } else {
                // Standing still - neutral standing pose
                frontLeg.xRot = 0.0F;
                frontLegTip.xRot = 0.0F;
                frontFoot.xRot = 0.0F;

                rearLeg.xRot = -0.15F;
                rearLegTip.xRot = 0.25F;
                rearFoot.xRot = -0.1F;

                if (state.ageInTicks % 40 == 0) {
                    System.out.println("[MODEL-CLIENT] LEFT SIDE standing still - speed=" + String.format("%.3f", this.movementSpeed));
                }
            }
        } else {
            // Flying pose - legs tucked back
            frontLeg.xRot = 1.0F;
            frontLegTip.xRot = 0.9F;
            frontFoot.xRot = 0.6F;
            rearLeg.xRot = 1.0F;
            rearLegTip.xRot = 0.9F;
            rearFoot.xRot = 0.7F;
        }
    }

    /**
     * Animates the right side legs (right front and right rear)
     * Uses diagonal gait: right front moves with left rear (opposite phase from left side)
     */
    private void poseLimbsRight(EnderDragonRenderState state, float flapAngle, ModelPart frontLeg, ModelPart frontLegTip, ModelPart frontFoot, ModelPart rearLeg, ModelPart rearLegTip, ModelPart rearFoot) {
        boolean isLanded = state.flapTime < 0.01F;

        if (isLanded) {
            // Use the movement speed passed from renderer
            // Only animate legs if actually moving (speed threshold of 0.005 blocks/tick)
            if (this.movementSpeed > 0.005F) {
                // Diagonal walking gait - right front moves with left rear
                float walkCycle = state.ageInTicks * 0.5F; // Slightly faster cycle

                // Scale animation by actual movement speed
                float animationScale = Math.min(this.movementSpeed * 3.0F, 1.0F);

                // Right front leg movement (OPPOSITE phase from left front) - increased amplitude
                float rightFrontSwing = Mth.cos(walkCycle + (float)Math.PI) * 1.2F * animationScale;
                frontLeg.xRot = rightFrontSwing * 0.7F;
                frontLegTip.xRot = Math.max(0.0F, rightFrontSwing * 0.7F);
                frontFoot.xRot = Math.min(0.0F, -rightFrontSwing * 0.5F);

                // Right rear leg movement (OPPOSITE phase from left rear, SAME as right front) - increased amplitude
                float rightRearSwing = Mth.cos(walkCycle + (float)Math.PI) * 0.9F * animationScale;
                rearLeg.xRot = -0.15F + rightRearSwing * 0.5F;
                rearLegTip.xRot = 0.25F + Math.max(0.0F, rightRearSwing * 0.6F);
                rearFoot.xRot = -0.1F + Math.min(0.0F, -rightRearSwing * 0.4F);

                if (state.ageInTicks % 40 == 0) {
                    System.out.println("[MODEL-CLIENT] RIGHT SIDE walking - speed=" + String.format("%.3f", this.movementSpeed) +
                        ", animScale=" + String.format("%.2f", animationScale));
                }
            } else {
                // Standing still - neutral standing pose
                frontLeg.xRot = 0.0F;
                frontLegTip.xRot = 0.0F;
                frontFoot.xRot = 0.0F;

                rearLeg.xRot = -0.15F;
                rearLegTip.xRot = 0.25F;
                rearFoot.xRot = -0.1F;

                if (state.ageInTicks % 40 == 0) {
                    System.out.println("[MODEL-CLIENT] RIGHT SIDE standing still - speed=" + String.format("%.3f", this.movementSpeed));
                }
            }
        } else {
            // Flying pose - legs tucked back
            frontLeg.xRot = 1.0F;
            frontLegTip.xRot = 0.9F;
            frontFoot.xRot = 0.6F;
            rearLeg.xRot = 1.0F;
            rearLegTip.xRot = 0.9F;
            rearFoot.xRot = 0.7F;
        }
    }
}
