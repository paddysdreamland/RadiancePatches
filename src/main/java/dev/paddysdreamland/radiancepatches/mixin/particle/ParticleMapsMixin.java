package dev.paddysdreamland.radiancepatches.mixin.particle;

import com.radiance.client.texture.AuxiliaryTextures;
import dev.paddysdreamland.radiancepatches.particle.ParticleGlow;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Particle maps fix.
 * Radiance only loads _s, _n and _f maps for textures in textures/block, item and entity, so maps made for particles
 * (textures/particle, or textures/particles that some mods use) were ignored.
 */
@Mixin(value = AuxiliaryTextures.class, remap = false)
public abstract class ParticleMapsMixin {
    @Unique
    private static final String[] RADIANCE_PATCHES$MAP_SUFFIXES = {"_s", "_n", "_f"};

    /** Which textures get their maps uploaded at all. */
    @Inject(method = "isUploadCandidatePath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$particleUploads(String path, CallbackInfoReturnable<Boolean> cir) {
        if (path.contains("textures/particle")) {
            cir.setReturnValue(true);
        }
    }

    /** Where _s and _n maps are looked for. */
    @Inject(method = "isTrackedTexturePath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$particleMaps(String path, CallbackInfoReturnable<Boolean> cir) {
        if (ParticleGlow.isParticlePath(path)) {
            cir.setReturnValue(true);
        }
    }

    /** Where _f flag maps are looked for. */
    @Inject(method = "isTrackedFlagPath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$particleFlags(String path, CallbackInfoReturnable<Boolean> cir) {
        if (path.startsWith("textures/flag/particle/") || path.startsWith("textures/flag/particles/")) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "decodeImagesAsync", at = @At("HEAD"))
    private static void radiancePatches$forgetMaps(ResourceManager manager, Executor executor,
                                                   CallbackInfoReturnable<CompletableFuture<?>> cir) {
        ParticleGlow.clearMaps();
    }

    /** Remember which particles have maps, so they can be ray traced (the flat painted particles ignore most maps). */
    @Inject(method = "classifyAuxiliaryResource", at = @At("RETURN"))
    private static void radiancePatches$recordMap(ResourceLocation file, CallbackInfoReturnable<AuxiliaryTextures> cir) {
        if (cir.getReturnValue() != null) {
            ParticleGlow.recordMap(file);
        }
    }

    /**
     * Keep the maps themselves out of the particle atlas. Radiance looks for "flame" next to "flame_s" as
     * textures/flame.png, but particle sprites are named without their folder, so it never found it and stitched
     * every particle map into the atlas as an extra particle texture.
     */
    @Inject(method = "shouldSkipAtlasSprite", at = @At("RETURN"), cancellable = true)
    private static void radiancePatches$skipParticleMaps(ResourceManager manager, ResourceLocation sprite,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        String path = sprite.getPath();
        for (String suffix : RADIANCE_PATCHES$MAP_SUFFIXES) {
            if (path.endsWith(suffix) && path.length() > suffix.length()) {
                String base = path.substring(0, path.length() - suffix.length());
                if (manager.getResource(sprite.withPath("textures/particle/" + base + ".png")).isPresent()) {
                    cir.setReturnValue(true);
                }
                return;
            }
        }
    }
}
