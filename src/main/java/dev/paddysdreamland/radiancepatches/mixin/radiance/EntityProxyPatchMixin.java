package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.radiance.client.proxy.world.EntityProxy;
import dev.paddysdreamland.radiancepatches.RadiancePatchesConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Two small fixes inside Radiance's entity code.
 * Both are marked "require = 0" so they also load quietly on a Radiance jar that was already
 * patched by the old patch script (where they simply find nothing to change).
 */
@Mixin(value = EntityProxy.class, remap = false)
public abstract class EntityProxyPatchMixin {

    /**
     * Block entity crash fix.
     * Radiance walks the game's list of always-visible block entities while the game can still
     * change it, which crashes with a ConcurrentModificationException (for example in tree rooms).
     * Radiance now gets its own copy, taken the same safe way Minecraft reads that list.
     */
    @ModifyVariable(method = "queueBlockEntitiesRebuild", at = @At("HEAD"), argsOnly = true, require = 0)
    private static Set<BlockEntity> radiancePatches$copyBlockEntities(Set<BlockEntity> blockEntities) {
        if (blockEntities == null || !RadiancePatchesConfig.isEnabled(RadiancePatchesConfig.BLOCK_ENTITY_CRASH)) {
            return blockEntities;
        }
        synchronized (blockEntities) {
            return new HashSet<>(blockEntities);
        }
    }

    /**
     * Modded villager layers fix.
     * Radiance lifts villager profession and level layers slightly off the skin so they don't
     * flicker or hide under it, but only for Minecraft's own textures. This applies the same
     * lift to villager layer textures from every mod.
     */
    @Redirect(method = "getGeometryNormalOffset",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"), require = 0)
    private static boolean radiancePatches$anyNamespace(String minecraft, Object namespace) {
        if (RadiancePatchesConfig.isEnabled(RadiancePatchesConfig.MODDED_VILLAGER_LAYERS)) {
            return true;
        }
        return minecraft.equals(namespace);
    }
}
