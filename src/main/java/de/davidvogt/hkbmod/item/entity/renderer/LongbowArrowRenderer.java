package de.davidvogt.hkbmod.item.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import de.davidvogt.hkbmod.HKBMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.ResourceLocation;

public class LongbowArrowRenderer extends ArrowRenderer {
    public static final ResourceLocation LONGBOW_ARROW_LOCATION = ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "textures/entity/projectiles/longbow_arrow.png");

    public LongbowArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }

    @Override
    protected ResourceLocation getTextureLocation(ArrowRenderState arrowRenderState) {
        return LONGBOW_ARROW_LOCATION;
    }

    @Override
    public void render(ArrowRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        float scale = 1.125F;
        poseStack.scale(scale, scale, 1.0F);

        super.render(renderState, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
