package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.radiance.client.texture.AuxiliaryTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extra PBR folders fix.
 * Radiance only loads _s, _n and _f maps for textures in textures/block, item and entity.
 * Block entity models (TARDIS Refined consoles and many others), connected textures and some
 * mods' fluids keep their textures in other folders, so their maps were ignored.
 */
@Mixin(value = AuxiliaryTextures.class, remap = false)
public abstract class AuxiliaryTexturesMixin {
    private static final String[] EXTRA_TEXTURE_FOLDERS = {
        "textures/ctm/", "textures/liquid/", "textures/fluid/", "textures/blockentity/", "textures/block_entity/"
    };
    private static final String[] EXTRA_FLAG_FOLDERS = {
        "textures/flag/ctm/", "textures/flag/liquid/", "textures/flag/fluid/",
        "textures/flag/blockentity/", "textures/flag/block_entity/"
    };

    /** Which textures get their maps uploaded at all. ("textures/block" already covers blockentity.) */
    @Inject(method = "isUploadCandidatePath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$extraUploadFolders(String path, CallbackInfoReturnable<Boolean> cir) {
        if (path.contains("textures/ctm") || path.contains("textures/liquid") || path.contains("textures/fluid")) {
            cir.setReturnValue(true);
        }
    }

    /** Where _s and _n maps are looked for. */
    @Inject(method = "isTrackedTexturePath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$extraTextureFolders(String path, CallbackInfoReturnable<Boolean> cir) {
        for (String folder : EXTRA_TEXTURE_FOLDERS) {
            if (path.startsWith(folder)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /** Where _f flag maps are looked for. */
    @Inject(method = "isTrackedFlagPath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$extraFlagFolders(String path, CallbackInfoReturnable<Boolean> cir) {
        for (String folder : EXTRA_FLAG_FOLDERS) {
            if (path.startsWith(folder)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
