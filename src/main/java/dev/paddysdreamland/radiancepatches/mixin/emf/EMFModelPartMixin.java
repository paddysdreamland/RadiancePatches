package dev.paddysdreamland.radiancepatches.mixin.emf;

import dev.paddysdreamland.radiancepatches.emf.CurrentBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import traben.entity_texture_features.utils.URenderTypeToVertexConsumer;

/**
 * EMF fix.
 * Entity Model Features draws parts with their own texture (skeleton quivers, sheep wool, ...)
 * through a fresh buffer of its own. Under Radiance that buffer isn't tied to the mob, so the
 * parts end up floating at the camera. This swaps in the mob's own buffer source instead.
 * Does nothing when Entity Model Features isn't installed.
 */
@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPart", remap = false)
public abstract class EMFModelPartMixin {
    @ModifyVariable(method = "renderWithTextureOverride", at = @At("STORE"), index = 8, require = 0, remap = false)
    private URenderTypeToVertexConsumer radiancePatches$useMobBuffer(URenderTypeToVertexConsumer original) {
        MultiBufferSource buffers = CurrentBuffers.current();
        if (buffers == null) {
            return original;
        }
        CurrentBuffers.announceOnce();
        return new URenderTypeToVertexConsumer(buffers);
    }
}
