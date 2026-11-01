package dev.paddysdreamland.radiancepatches.mixin.emissive;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.paddysdreamland.radiancepatches.emissive.EmissiveFaces;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Emissive faces fix for items (held, dropped, in item frames): see EmissiveFaces. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @WrapOperation(method = "renderQuadList",
                   at = @At(value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"))
    private void radiancePatches$glowingItemFace(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                                 float red, float green, float blue, float alpha, int light,
                                                 int overlay, boolean readExistingColor, Operation<Void> original) {
        float glow = EmissiveFaces.glowOf(quad);
        if (glow <= 0.0f) {
            original.call(consumer, pose, quad, red, green, blue, alpha, light, overlay, readExistingColor);
            return;
        }
        EmissiveFaces.begin(glow);
        try {
            original.call(consumer, EmissiveFaces.lifted(pose, quad), quad, red, green, blue, alpha, light, overlay,
                          readExistingColor);
        } finally {
            EmissiveFaces.end();
        }
    }
}
