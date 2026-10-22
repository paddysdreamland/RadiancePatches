package dev.paddysdreamland.radiancepatches;

import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

/**
 * Radiance Patches: a bundle of fixes for Radiance, the Vulkan ray tracing renderer.
 * All the work happens in the mixins (see radiance_patches.mixins.json); each fix can be
 * switched off in config/radiance_patches.properties.
 */
@Mod(RadiancePatches.MOD_ID)
public final class RadiancePatches {
    public static final String MOD_ID = "radiance_patches";

    private static Boolean radianceLoaded;

    public RadiancePatches() {
    }

    /** True when Radiance is installed. Checked once, then remembered. */
    public static boolean isRadianceLoaded() {
        if (radianceLoaded == null) {
            ModList list = ModList.get();
            radianceLoaded = list != null && list.isLoaded("radiance");
        }
        return radianceLoaded;
    }
}
