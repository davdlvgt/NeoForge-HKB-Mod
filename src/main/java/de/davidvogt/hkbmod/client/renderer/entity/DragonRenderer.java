package de.davidvogt.hkbmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.davidvogt.hkbmod.client.model.DragonModel;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the custom Dragon entity.
 * Uses the Ender Dragon model with custom scaling.
 */
public class DragonRenderer extends EntityRenderer<DragonEntity, EnderDragonRenderState> {

    private static final ResourceLocation ENDER_DRAGON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png");

    private final DragonModel model;
    private float dragonYaw;

    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DragonModel(context.bakeLayer(DragonModel.LAYER_LOCATION));
        this.shadowRadius = 0.75F;
    }

    @Override
    public @NotNull EnderDragonRenderState createRenderState() {
        return new EnderDragonRenderState();
    }

    @Override
    public void extractRenderState(@NotNull DragonEntity entity, @NotNull EnderDragonRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        // Update the flight history with the current entity position
        // The DragonFlightHistory tracks position history for smooth neck/tail animations
        renderState.flightHistory.copyFrom(entity.getFlightHistory());

        // Pass movement speed to the model for walking animation
        float movementSpeed = (float)Math.sqrt(entity.getDeltaMovement().x * entity.getDeltaMovement().x +
            entity.getDeltaMovement().z * entity.getDeltaMovement().z);
        this.model.setMovementSpeed(movementSpeed);

        // Debug: Log movement speed every second when landed
        if (entity.isLanded() && entity.tickCount % 20 == 0) {
            System.out.println("[RENDERER] Setting movement speed: " + String.format("%.4f", movementSpeed) +
                ", deltaX=" + String.format("%.4f", entity.getDeltaMovement().x) +
                ", deltaZ=" + String.format("%.4f", entity.getDeltaMovement().z));
        }

        // Set the flap time for wing animation based on entity age
        // Only fold wings when ACTUALLY LANDED, not during landing approach
        if (entity.isLanded()) {
            // Dragon is on the ground - fold wings
            renderState.flapTime = 0.0F;
            // Log every second (20 ticks) on client side
            if (entity.tickCount % 20 == 0) {
                System.out.println("[RENDERER-CLIENT] Dragon animation state: flapTime=0.0 (WINGS FOLDED), " +
                    "isLanded=true, movementSpeed=" + String.format("%.3f", movementSpeed));
            }
        } else {
            // Dragon is flying (includes landing approach) - keep wings flapping
            renderState.flapTime = (entity.tickCount + partialTick) / 10.0F;
            // Log every 5 seconds (100 ticks) during flight
            if (entity.tickCount % 100 == 0) {
                System.out.println("[RENDERER-CLIENT] Dragon animation state: flapTime=" +
                    String.format("%.2f", renderState.flapTime) + " (WINGS FLAPPING), isLandingMode=" + entity.isLandingMode());
            }
        }

        this.dragonYaw = entity.getYRot();
    }

    @Override
    public void render(@NotNull EnderDragonRenderState renderState, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Rotate 180 degrees around the X-axis to flip the dragon right-side up
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));

        // Move the dragon down so it stands on the ground properly after rotation
        poseStack.translate(0.0D, -1.5D, 0.0D);

        // Scale down to 35% of the Ender Dragon's size
        float scale = 0.35F;
        poseStack.scale(scale, scale, scale);

        // Drehe den Drachen um die aktuelle Yaw-Rotation
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.dragonYaw));

        // Setup the model animation
        this.model.setupAnim(renderState);

        // Get the render type
        RenderType renderType = this.model.renderType(ENDER_DRAGON_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        // Render the model
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(renderState, poseStack, buffer, packedLight);
    }
}
