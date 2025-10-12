package de.davidvogt.hkbmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import de.davidvogt.hkbmod.HKBMod;
import de.davidvogt.hkbmod.client.model.DeerModel;
import de.davidvogt.hkbmod.item.entity.custom.DeerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Deer entity
 * Handles the visual representation of the deer in the game world
 */
public class DeerRenderer extends MobRenderer<DeerEntity, LivingEntityRenderState, DeerModel> {

    // Texture location for the deer
    private static final ResourceLocation DEER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HKBMod.MODID, "textures/entity/deer.png");

    public DeerRenderer(EntityRendererProvider.Context context) {
        super(context, new DeerModel(context.bakeLayer(DeerModel.LAYER_LOCATION)), 0.5F);
        // 0.5F is the shadow radius (how big the shadow under the deer is)
    }

    /**
     * Get the texture location to use for rendering this deer
     */
    @Override
    public ResourceLocation getTextureLocation(LivingEntityRenderState renderState) {
        return DEER_TEXTURE;
    }

    /**
     * Create the render state for this entity
     */
    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    /**
     * Optional: Scale the deer model
     * Uncomment and modify if you want to adjust the deer's size
     */
    @Override
    protected void scale(LivingEntityRenderState renderState, PoseStack poseStack) {
        super.scale(renderState, poseStack);
        // Example: scale the deer to be slightly smaller
        // poseStack.scale(0.9F, 0.9F, 0.9F);
    }
}
