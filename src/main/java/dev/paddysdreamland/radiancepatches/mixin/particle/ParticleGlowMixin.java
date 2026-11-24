package dev.paddysdreamland.radiancepatches.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radiance.client.constant.Constants;
import com.radiance.client.proxy.world.EntityProxy;
import com.radiance.client.vertex.StorageVertexConsumerProvider;
import dev.paddysdreamland.radiancepatches.particle.ParticleGlow;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particle glow and particle maps fixes: moves self-lit particles, and particles with maps, out of the flat
 * painted-on-top particles into ray traced ones. See ParticleGlow.
 */
@Mixin(value = EntityProxy.class, remap = false)
public abstract class ParticleGlowMixin {
    @Inject(method = "queueParticleRebuild", at = @At("HEAD"))
    private static void radiancePatches$begin(Camera camera, float tickDelta, Frustum frustum, CallbackInfo ci) {
        ParticleGlow.begin();
    }

    /** Each ordinary particle (the first render call; the second one is for particles with their own render code). */
    @WrapOperation(method = "queueParticleRebuild",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V",
                            ordinal = 0))
    private static void radiancePatches$route(Particle particle, VertexConsumer painted, Camera camera, float partialTick,
                                              Operation<Void> original, @Local RenderType layer) {
        original.call(particle, ParticleGlow.bufferFor(particle, partialTick, layer, painted), camera, partialTick);
        ParticleGlow.afterRender();
    }

    /** Hand the ray traced particles to Radiance together with the rest. */
    @WrapOperation(method = "queueParticleRebuild",
                   at = @At(value = "INVOKE",
                            target = "Lcom/radiance/client/proxy/world/EntityProxy;queueBuild(Ljava/util/List;Lcom/radiance/client/proxy/world/EntityProxy$EntityRenderDataList;FLcom/radiance/client/constant/Constants$Coordinates;Z)V"))
    private static void radiancePatches$submit(List<StorageVertexConsumerProvider> providers,
                                               EntityProxy.EntityRenderDataList renderData, float lineWidth,
                                               Constants.Coordinates coordinate, boolean normalOffset,
                                               Operation<Void> original) {
        ParticleGlow.submit(providers, renderData);
        original.call(providers, renderData, lineWidth, coordinate, normalOffset);
    }
}
