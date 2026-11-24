package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radiance.client.proxy.world.EntityProxy;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Particle culling fix.
 * NeoForge skips particles outside the view, but Radiance draws every particle every frame, including the ones
 * behind you, and rebuilds and uploads all of them each frame. Breaking lots of blocks at once (Vein Miner, tree
 * felling) makes thousands of particles, which lagged the game. This skips particles outside the view the same way
 * NeoForge does. Particles are only seen directly by the camera (they cast no shadows and show up in no
 * reflections), so nothing visible changes.
 */
@Mixin(value = EntityProxy.class, remap = false)
public abstract class ParticleCullingMixin {
    /** Both of Radiance's particle loops: ordinary particles and particles with their own render code. */
    @WrapOperation(method = "queueParticleRebuild",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
    private static void radiancePatches$skipOutOfView(Particle particle, VertexConsumer consumer, Camera camera,
                                                      float partialTick, Operation<Void> original,
                                                      @Local(argsOnly = true) Frustum frustum) {
        if (frustum != null && !frustum.isVisible(particle.getRenderBoundingBox(partialTick))) {
            return;
        }
        original.call(particle, consumer, camera, partialTick);
    }
}
