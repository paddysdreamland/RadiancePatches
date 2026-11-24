package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.radiance.client.constant.Constants;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unknown transparency fix.
 * Radiance sorts translucent layers by comparing their transparency setting against Minecraft's own ones, and crashes
 * ("Invalid render layer") when a layer uses a setting it doesn't know. Some mods make their own copy of a vanilla
 * setting under the same name: GeckoLib does this for glowmask layers ("geo_glowing_layer"), so every GeckoLib mob or
 * effect with a _glowmask texture (Companions' antlions, magic ray circles and others) crashed the game.
 * Every transparency Radiance does know is drawn as a transparent layer, so unknown ones are drawn the same way.
 */
@Mixin(value = Constants.GeometryTypes.class, remap = false)
public abstract class GeometryTypeFallbackMixin {
    /** Render type names already reported in the log, so each one is only mentioned once. */
    @Unique
    private static final Set<String> radiancePatches$reported = ConcurrentHashMap.newKeySet();

    @Inject(method = "getGeometryType", at = @At(value = "NEW", target = "java/lang/IllegalArgumentException"), cancellable = true)
    private static void radiancePatches$unknownTransparency(RenderType renderLayer, boolean reflect,
                                                            CallbackInfoReturnable<Constants.GeometryTypes> cir) {
        // Read the name field directly; asking some mods' render types to describe themselves isn't safe here.
        if (radiancePatches$reported.add(renderLayer.name)) {
            System.out.println("[Radiance Patches] Render type \"" + renderLayer.name
                + "\" uses a transparency Radiance doesn't know, drawing it as a transparent layer.");
        }
        cir.setReturnValue(Constants.GeometryTypes.WORLD_TRANSPARENT);
    }
}
