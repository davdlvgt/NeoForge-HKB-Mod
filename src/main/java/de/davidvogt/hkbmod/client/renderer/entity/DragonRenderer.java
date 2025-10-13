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

        // Set the flap time for wing animation based on entity age
        renderState.flapTime = (entity.tickCount + partialTick) / 10.0F;
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
