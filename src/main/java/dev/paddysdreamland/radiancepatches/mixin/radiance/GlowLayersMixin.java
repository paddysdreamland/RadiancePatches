package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.radiance.client.vertex.PBRVertexConsumer;
import dev.paddysdreamland.radiancepatches.RadiancePatchesConfig;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Glow layers fix.
 * Some things are drawn with a render type that always shines at full brightness instead of using a glowing texture:
 * Minecraft's "eyes" (spiders, endermen, phantoms, Undergarden mobs and most modded glowing mobs),
 * "entity_translucent_emissive" (the Warden's pulsing spots, TARDIS Refined's shell lights) and mod types with
 * "glowing" or "emissive" in their name (Create's Wand of Symmetry and Worldshaper cores), and "lightning" (lightning
 * bolts, and glowing boxes some mods draw with it, like TARDIS Refined's screwdriver selection). Radiance doesn't know
 * these and treats them as plain painted layers, so they don't glow. This makes them glow by themselves.
 * The layer's transparency still decides how strong the glow is at each pixel, and where a resource pack's
 * emission map has a glow value, Advanced Patches uses the map instead.
 */
@Mixin(value = PBRVertexConsumer.class, remap = false)
public abstract class GlowLayersMixin {
    /** Glow strength for this consumer's vertices, 0 when it isn't drawing a glow layer. */
    @Unique
    private float radiancePatches$eyesGlow;

    @Unique
    private static float radiancePatches$configuredStrength = -1.0f;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At("TAIL"))
    private void radiancePatches$detectEyes(ByteBufferBuilder allocator, VertexFormat.Mode drawMode, VertexFormat format,
                                            RenderType renderLayer, CallbackInfo ci) {
        if (renderLayer != null && radiancePatches$isGlowLayer(radiancePatches$renderTypeName(renderLayer))) {
            if (radiancePatches$configuredStrength < 0.0f) {
                radiancePatches$configuredStrength =
                    Math.max(RadiancePatchesConfig.getFloat(RadiancePatchesConfig.GLOW_LAYERS_STRENGTH), 0.0f);
            }
            radiancePatches$eyesGlow = radiancePatches$configuredStrength;
        }
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("TAIL"))
    private void radiancePatches$glowVertex(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        radiancePatches$applyGlow();
    }

    @Inject(method = "vertex(FFFI)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("TAIL"))
    private void radiancePatches$glowGlintVertex(float x, float y, float z, int glintTextureID,
                                                 CallbackInfoReturnable<VertexConsumer> cir) {
        radiancePatches$applyGlow();
    }

    @Unique
    private void radiancePatches$applyGlow() {
        if (radiancePatches$eyesGlow > 0.0f) {
            ((PBRVertexConsumer) (Object) this).albedoEmission(radiancePatches$eyesGlow);
        }
    }

    /** Render types that draw at full brightness on purpose. */
    @Unique
    private static boolean radiancePatches$isGlowLayer(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.equals("eyes") || lower.equals("lightning") || lower.contains("emissive") || lower.contains("glowing");
    }

    /** The render type's own name ("eyes", "entity_cutout", ...), read directly. */
    @Unique
    private static String radiancePatches$renderTypeName(RenderType layer) {
        // Read the name field directly. Asking a render type to describe itself (toString) is not safe:
        // some mods' render types touch the graphics system while doing that, which crashes off the render thread.
        return layer.name;
    }
}
