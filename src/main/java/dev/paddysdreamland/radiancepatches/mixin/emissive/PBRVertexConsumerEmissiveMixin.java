package dev.paddysdreamland.radiancepatches.mixin.emissive;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radiance.client.vertex.PBRVertexConsumer;
import dev.paddysdreamland.radiancepatches.emissive.EmissiveFaces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Emissive faces fix: gives every vertex of a glowing face Radiance's emission. See EmissiveFaces. */
@Mixin(value = PBRVertexConsumer.class, remap = false)
public abstract class PBRVertexConsumerEmissiveMixin {
    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("TAIL"))
    private void radiancePatches$glowVertex(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        float glow = EmissiveFaces.current();
        if (glow > 0.0f) {
            ((PBRVertexConsumer) (Object) this).albedoEmission(glow);
        }
    }

    /** Radiance sets a face's emission after drawing it; keep the glow if it is stronger. */
    @ModifyVariable(method = "albedoEmission(F)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("HEAD"),
                    argsOnly = true)
    private float radiancePatches$keepGlow(float emission) {
        return Math.max(emission, EmissiveFaces.current());
    }
}
