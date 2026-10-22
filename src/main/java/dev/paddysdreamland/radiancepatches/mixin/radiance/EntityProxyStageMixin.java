package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.mojang.blaze3d.vertex.MeshData;
import com.radiance.client.proxy.world.EntityProxy;
import dev.paddysdreamland.radiancepatches.stage.StageModelCapture;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Level render models fix: hooks into Radiance's capture of level render events. */
@Mixin(value = EntityProxy.class, remap = false)
public abstract class EntityProxyStageMixin {

    @Inject(method = "captureWorldRender", at = @At("HEAD"))
    private static void radiancePatches$begin(Object stage, Runnable render, CallbackInfo ci) {
        // Only the outermost capture; nested stages run inside it.
        if (!EntityProxy.isWorldRenderCaptureActive()) {
            StageModelCapture.begin();
        }
    }

    @Inject(method = "captureWorldRender", at = @At("RETURN"))
    private static void radiancePatches$end(Object stage, Runnable render, CallbackInfo ci) {
        if (!EntityProxy.isWorldRenderCaptureActive()) {
            StageModelCapture.end(stage);
        }
    }

    @Inject(method = "captureWorldRenderLayer", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$capture(RenderType layer, MeshData mesh, CallbackInfoReturnable<Boolean> cir) {
        if (StageModelCapture.tryCapture(layer, mesh)) {
            cir.setReturnValue(true);
        }
    }
}
