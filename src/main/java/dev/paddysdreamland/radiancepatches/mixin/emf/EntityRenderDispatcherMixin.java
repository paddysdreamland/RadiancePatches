package dev.paddysdreamland.radiancepatches.mixin.emf;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.paddysdreamland.radiancepatches.emf.CurrentBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EMF fix: remembers the buffer source while a mob is being drawn. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    private static final String RENDER =
        "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void radiancePatches$push(Entity entity, double x, double y, double z, float yaw, float partialTick,
                                    PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        CurrentBuffers.push(buffers);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void radiancePatches$pop(Entity entity, double x, double y, double z, float yaw, float partialTick,
                                   PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        CurrentBuffers.pop();
    }
}
