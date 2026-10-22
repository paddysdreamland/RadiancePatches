package dev.paddysdreamland.radiancepatches.mixin.texid;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.paddysdreamland.radiancepatches.texid.TexIdPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Texture id fix: deleting a texture with a reserved id puts the id back into the pool. */
@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    @Inject(method = "_deleteTexture", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$recycle(int id, CallbackInfo ci) {
        if (TexIdPool.recycle(id)) {
            ci.cancel();
        }
    }

    @Inject(method = "_deleteTextures", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$recycleMany(int[] ids, CallbackInfo ci) {
        if (!TexIdPool.anyOwned(ids)) {
            return;
        }
        for (int id : ids) {
            if (!TexIdPool.recycle(id)) {
                GlStateManager._deleteTexture(id);
            }
        }
        ci.cancel();
    }
}
