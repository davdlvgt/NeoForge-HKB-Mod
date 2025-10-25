package de.davidvogt.hkbmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.client.model.DragonModel;
import de.davidvogt.hkbmod.item.entity.custom.DragonConstants;
import de.davidvogt.hkbmod.item.entity.custom.DragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer for the custom Dragon entity.
 * Uses the Ender Dragon model with custom scaling.
 */
public class DragonRenderer extends EntityRenderer<DragonEntity, EnderDragonRenderState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DragonRenderer.class);
    private static final ResourceLocation ENDER_DRAGON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "textures/entity/dragon.png");

    private final DragonModel model;
    private float dragonYaw;
    // current scale read from the entity in extractRenderState
    private float currentScale = DragonConstants.DRAGON_SCALE;
    // current variant color tint read from the entity in extractRenderState
    private float colorRed = 1.0f;
    private float colorGreen = 1.0f;
    private float colorBlue = 1.0f;

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
        float movementSpeed = (float) Math.sqrt(entity.getDeltaMovement().x * entity.getDeltaMovement().x +
                entity.getDeltaMovement().z * entity.getDeltaMovement().z);
        this.model.setMovementSpeed(movementSpeed);

        // Pass resting state to the model (for wild dragons resting on nests)
        this.model.setResting(entity.isResting());

        // Pass sitting state to the model (for tamed dragons that are commanded to sit)
        this.model.setSitting(entity.isSitting());

        // Set the flap time for wing animation based on entity age
        // Only fold wings when ACTUALLY LANDED, not during landing approach
        if (entity.isLanded()) {
            // Dragon is on the ground - fold wings
            renderState.flapTime = 0.0F;
        } else {
            // Dragon is flying (includes landing approach) - keep wings flapping
            renderState.flapTime = (entity.tickCount + partialTick) / 10.0F;
        }

        this.dragonYaw = entity.getYRot();

        // Read per-entity scale so we can render variations
        this.currentScale = entity.getDragonScale();

        // Read variant color for tinting
        de.davidvogt.hkbmod.item.entity.custom.DragonVariant variant = entity.getVariant();
        this.colorRed = variant.getRed();
        this.colorGreen = variant.getGreen();
        this.colorBlue = variant.getBlue();
    }

    @Override
    public void render(@NotNull EnderDragonRenderState renderState, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Rotate 180 degrees around the X-axis to flip the dragon right-side up
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));

        // Move the dragon down so it stands on the ground properly after rotation
        poseStack.translate(0.0D, -1.5D, 0.0D);

        // Scale using per-entity value (fallback already set in currentScale)
        float scale = this.currentScale;
        poseStack.scale(scale, scale, scale);

        // Rotate the dragon around the current yaw rotation
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.dragonYaw));

        // Setup the model animation
        this.model.setupAnim(renderState);

        // Get the render type
        RenderType renderType = this.model.renderType(ENDER_DRAGON_TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        // Apply color multiplier using OverlayVertexConsumer wrapper for tinting
        int colorARGB = 0xFF000000 |
                        (((int)(this.colorRed * 255) & 0xFF) << 16) |
                        (((int)(this.colorGreen * 255) & 0xFF) << 8) |
                        ((int)(this.colorBlue * 255) & 0xFF);

        // Render the model with color tinting via a wrapper
        this.model.renderToBuffer(poseStack, new ColoredVertexConsumer(vertexConsumer, this.colorRed, this.colorGreen, this.colorBlue),
                packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(renderState, poseStack, buffer, packedLight);
    }

    /**
     * Wrapper class that multiplies vertex colors with a tint color.
     * Used to apply color variants to the dragon model.
     */
    private static class ColoredVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float red, green, blue;

        public ColoredVertexConsumer(VertexConsumer delegate, float red, float green, float blue) {
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return delegate.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            // Multiply the texture color with the tint color
            return delegate.setColor(
                    (int)(r * red),
                    (int)(g * green),
                    (int)(b * blue),
                    a
            );
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return delegate.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return delegate.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return delegate.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return delegate.setNormal(x, y, z);
        }
    }
}
