package dev.paddysdreamland.radiancepatches.mixin.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.paddysdreamland.radiancepatches.emf.CurrentBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EMF fix: remembers the buffer source while a block entity is being drawn. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    private static final String RENDER =
        "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V";

    @Inject(method = RENDER, at = @At("HEAD"), require = 0)
    private void radiancePatches$push(BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffers, CallbackInfo ci) {
        CurrentBuffers.push(buffers);
    }

    @Inject(method = RENDER, at = @At("RETURN"), require = 0)
    private void radiancePatches$pop(BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                   MultiBufferSource buffers, CallbackInfo ci) {
        CurrentBuffers.pop();
    }
}
