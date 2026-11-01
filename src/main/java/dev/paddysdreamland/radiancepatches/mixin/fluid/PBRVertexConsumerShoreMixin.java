package dev.paddysdreamland.radiancepatches.mixin.fluid;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radiance.client.vertex.PBRVertexConsumer;
import dev.paddysdreamland.radiancepatches.fluid.LavaShore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lava shore edges: stores the lava block's wall bits in each of its vertices. See LavaShore. */
@Mixin(value = PBRVertexConsumer.class, remap = false)
public abstract class PBRVertexConsumerShoreMixin {
    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("TAIL"))
    private void radiancePatches$shoreVertex(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        float value = LavaShore.current();
        if (value > 0.0f) {
            ((PBRVertexConsumer) (Object) this).albedoEmission(value);
        }
    }
}
