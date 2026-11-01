package dev.paddysdreamland.radiancepatches.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * First-person arm marker.
 * The first-person arm and the held item end up in the same layer in Radiance, so a shader pack can't tell them
 * apart. This draws the arm (and its sleeve) with an alpha of 254 instead of 255, which is invisible but lets
 * Advanced Patches recognise the arm and give it its own opacity ("Hand Opacity" vs "Held Item Opacity").
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerHandMarkerMixin {
    /** White with alpha 254: the marker colour. */
    @Unique
    private static final int RADIANCE_PATCHES$ARM_MARKER = 0xFEFFFFFF;

    @WrapOperation(method = "renderHand",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void radiancePatches$markArm(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int light,
                                         int overlay, Operation<Void> original) {
        part.render(poseStack, consumer, light, overlay, RADIANCE_PATCHES$ARM_MARKER);
    }
}
