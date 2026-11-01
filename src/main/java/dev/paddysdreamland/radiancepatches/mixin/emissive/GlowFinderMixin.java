package dev.paddysdreamland.radiancepatches.mixin.emissive;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.radiance.client.vertex.PBRVertexConsumer;
import dev.paddysdreamland.radiancepatches.emissive.EmissiveFaces;
import dev.paddysdreamland.radiancepatches.emissive.GlowFinder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Glow finder: watches every vertex Radiance receives for full-brightness light. See GlowFinder. */
@Mixin(value = PBRVertexConsumer.class, remap = false)
public abstract class GlowFinderMixin {
    /** Full brightness: block light 15 and sky light 15, packed the way Minecraft passes light to a vertex. */
    @Unique
    private static final int RADIANCE_PATCHES$FULL = 240;

    @Unique
    private ResourceLocation radiancePatches$finderTexture;

    @Unique
    private String radiancePatches$finderType;

    @Unique
    private boolean radiancePatches$finderGlowLayer;

    @Unique
    private float radiancePatches$lastU;

    @Unique
    private float radiancePatches$lastV;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At("TAIL"))
    private void radiancePatches$finderSetup(ByteBufferBuilder allocator, VertexFormat.Mode drawMode, VertexFormat format,
                                             RenderType renderLayer, CallbackInfo ci) {
        if (renderLayer == null) {
            return;
        }
        radiancePatches$finderTexture = GlowFinder.textureOf(renderLayer);
        radiancePatches$finderType = renderLayer.name;
        String lower = renderLayer.name.toLowerCase(java.util.Locale.ROOT);
        radiancePatches$finderGlowLayer = lower.equals("eyes") || lower.contains("emissive") || lower.contains("glowing");
    }

    @Inject(method = "setUv(FF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("HEAD"))
    private void radiancePatches$finderUv(float u, float v, CallbackInfoReturnable<VertexConsumer> cir) {
        radiancePatches$lastU = u;
        radiancePatches$lastV = v;
    }

    @Inject(method = "setUv2(II)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("HEAD"))
    private void radiancePatches$finderLight(int blockLight, int skyLight, CallbackInfoReturnable<VertexConsumer> cir) {
        if (blockLight < RADIANCE_PATCHES$FULL || skyLight < RADIANCE_PATCHES$FULL || radiancePatches$finderTexture == null) {
            return;
        }
        String status;
        if (radiancePatches$finderGlowLayer) {
            status = "glow layers fix";
        } else if (EmissiveFaces.current() > 0.0f) {
            status = "emissive faces fix";
        } else {
            status = "needs map";
        }
        GlowFinder.record(radiancePatches$finderTexture, radiancePatches$finderType, radiancePatches$lastU,
                          radiancePatches$lastV, status);
    }
}
