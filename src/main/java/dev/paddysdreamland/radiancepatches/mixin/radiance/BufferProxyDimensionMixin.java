package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.radiance.client.proxy.vulkan.BufferProxy;
import dev.paddysdreamland.radiancepatches.dimension.DimensionProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Dimension profiles: passes the current dimension's profile number to the shader pack.
 * Radiance sends Minecraft's fog shape (0 or 1) to the shader, which Advanced never reads; the profile number
 * goes into its upper bits, so the shader can give TARDIS interiors (and later other dimensions) their own sky.
 */
@Mixin(value = BufferProxy.class, remap = false)
public abstract class BufferProxyDimensionMixin {
    @ModifyVariable(method = "updateWorldUniform", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static int radiancePatches$addDimensionProfile(int fogShape) {
        return (fogShape & 0xFF) | (DimensionProfiles.current() << 8);
    }
}
