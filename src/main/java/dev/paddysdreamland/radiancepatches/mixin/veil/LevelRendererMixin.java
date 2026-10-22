package dev.paddysdreamland.radiancepatches.mixin.veil;

import dev.paddysdreamland.radiancepatches.RadiancePatches;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Veil fix.
 * Radiance replaces vanilla chunk rendering, so vanilla's per-section vertex buffers are never
 * created. Anything that still calls vanilla renderSectionLayer (for example Veil drawing the
 * extra render layer used by Create Aeronautics' Levitite) hits a missing buffer and crashes.
 * With Radiance present that call can never succeed, so it is skipped.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderSectionLayer", at = @At("HEAD"), cancellable = true)
    private void radiancePatches$skipVanillaSectionLayer(RenderType renderType, double x, double y, double z,
                                                       Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                                       CallbackInfo ci) {
        if (RadiancePatches.isRadianceLoaded()) {
            ci.cancel();
        }
    }
}
