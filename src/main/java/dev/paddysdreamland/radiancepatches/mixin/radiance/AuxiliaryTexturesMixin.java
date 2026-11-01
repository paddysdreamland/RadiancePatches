package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.radiance.client.texture.AuxiliaryTextures;
import dev.paddysdreamland.radiancepatches.RadiancePatchesConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extra PBR folders fix.
 * Radiance only loads _s, _n and _f maps for textures in textures/block, item and entity.
 * Block entity models (TARDIS Refined consoles and many others), connected textures and some
 * mods' fluids keep their textures in other folders, so their maps were ignored.
 * Some mods even keep textures outside the textures folder (Mekanism's energy core in assets/mekanism/render/).
 * Radiance never searched there at all; the folders listed in extra_map_roots are searched too.
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

    private static String[] radiancePatches$roots;

    /** Extra top-level folders next to "textures" that can hold textures with maps ("render/" and so on). */
    private static String[] radiancePatches$extraRoots() {
        String[] roots = radiancePatches$roots;
        if (roots == null) {
            roots = java.util.Arrays.stream(RadiancePatchesConfig.getString(RadiancePatchesConfig.EXTRA_MAP_ROOTS).split(","))
                .map(String::trim)
                .map(root -> root.endsWith("/") ? root.substring(0, root.length() - 1) : root)
                .filter(root -> !root.isEmpty() && !root.equals("textures"))
                .toArray(String[]::new);
            radiancePatches$roots = roots;
        }
        return roots;
    }

    private static boolean radiancePatches$inExtraRoot(String path) {
        for (String root : radiancePatches$extraRoots()) {
            if (path.startsWith(root + "/")) {
                return true;
            }
        }
        return false;
    }

    /** Radiance only reads maps from the textures folder; also read them from the extra folders. */
    @Redirect(method = "decodeImagesAsync",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/packs/resources/ResourceManager;listResources(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;"))
    private static Map<ResourceLocation, Resource> radiancePatches$listExtraRoots(ResourceManager manager, String folder,
                                                                                 Predicate<ResourceLocation> filter) {
        Map<ResourceLocation, Resource> found = new HashMap<>(manager.listResources(folder, filter));
        for (String root : radiancePatches$extraRoots()) {
            found.putAll(manager.listResources(root, filter));
        }
        return found;
    }

    /** Which textures get their maps uploaded at all. ("textures/block" already covers blockentity.) */
    @Inject(method = "isUploadCandidatePath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$extraUploadFolders(String path, CallbackInfoReturnable<Boolean> cir) {
        if (path.contains("textures/ctm") || path.contains("textures/liquid") || path.contains("textures/fluid")
            || radiancePatches$inExtraRoot(path)) {
            cir.setReturnValue(true);
        }
    }

    /** Where _s and _n maps are looked for. */
    @Inject(method = "isTrackedTexturePath", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$extraTextureFolders(String path, CallbackInfoReturnable<Boolean> cir) {
        if (radiancePatches$inExtraRoot(path)) {
            cir.setReturnValue(true);
            return;
        }
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
