package dev.paddysdreamland.radiancepatches.mixin.emissive;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.paddysdreamland.radiancepatches.emissive.EmissiveFaces;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Emissive faces fix for blocks: see EmissiveFaces. */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    @WrapOperation(method = {"renderModelFaceAO", "renderModelFaceFlat"},
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"))
    private void radiancePatches$glowingFace(ModelBlockRenderer renderer, BlockAndTintGetter level, BlockState state,
                                             BlockPos pos, VertexConsumer consumer, PoseStack.Pose pose,
                                             BakedQuad quad, float brightness0, float brightness1, float brightness2,
                                             float brightness3, int light0, int light1, int light2, int light3,
                                             int overlay, Operation<Void> original) {
        float glow = EmissiveFaces.glowOf(quad);
        if (glow <= 0.0f) {
            original.call(renderer, level, state, pos, consumer, pose, quad, brightness0, brightness1, brightness2,
                          brightness3, light0, light1, light2, light3, overlay);
            return;
        }
        EmissiveFaces.begin(glow);
        try {
            original.call(renderer, level, state, pos, consumer, EmissiveFaces.lifted(pose, quad), quad, brightness0,
                          brightness1, brightness2, brightness3, light0, light1, light2, light3, overlay);
        } finally {
            EmissiveFaces.end();
        }
    }
}
