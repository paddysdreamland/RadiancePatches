package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.radiance.client.proxy.world.EntityProxy;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlay layers fix.
 * Many mobs are drawn as a base skin plus extra layers on top (glowing eyes, emissive patterns, clothes),
 * all on exactly the same surface. A ray can't tell which of two surfaces at the same spot is "on top",
 * so it picks one at random: glowing layers end up under the skin and bleed through, or flicker.
 * Radiance already lifts villager layers a hair off the skin to solve this, but only for villager textures.
 * This lifts every layer after a mob's first one by a tiny step, in the order the game draws them,
 * so later layers always sit on top. Layers Radiance already lifts (villagers) keep Radiance's own values.
 */
@Mixin(value = EntityProxy.class, remap = false)
public abstract class EntityLayerOrderMixin {
    /** Lift per layer, the same step Radiance uses for villager layers (in blocks). */
    @Unique
    private static final float RADIANCE_PATCHES$LAYER_STEP = 6.25E-5f;

    /** Which layer of the current mob is being prepared. Only used on the render thread. */
    @Unique
    private static int radiancePatches$layerIndex;

    @Shadow
    private static float getGeometryNormalOffset(ResourceLocation identifier) {
        throw new AssertionError();
    }

    /** A new mob's list of layers starts here. */
    @Inject(method = "queueBuildInternal",
            at = @At(value = "INVOKE",
                     target = "Lcom/radiance/client/proxy/world/EntityProxy$EntityRenderData;iterator()Ljava/util/Iterator;"))
    private static void radiancePatches$startLayers(CallbackInfo ci) {
        radiancePatches$layerIndex = 0;
    }

    @Redirect(method = "queueBuildInternal",
              at = @At(value = "INVOKE",
                       target = "Lcom/radiance/client/proxy/world/EntityProxy;getGeometryNormalOffset(Lnet/minecraft/resources/ResourceLocation;)F"))
    private static float radiancePatches$layerOffset(ResourceLocation identifier) {
        float radianceOffset = getGeometryNormalOffset(identifier);
        int layer = radiancePatches$layerIndex++;
        if (radianceOffset != 0.0f) {
            return radianceOffset;
        }
        return layer * RADIANCE_PATCHES$LAYER_STEP;
    }
}
