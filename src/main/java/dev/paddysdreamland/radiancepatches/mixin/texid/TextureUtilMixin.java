package dev.paddysdreamland.radiancepatches.mixin.texid;

import com.mojang.blaze3d.platform.TextureUtil;
import dev.paddysdreamland.radiancepatches.texid.TexIdPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Texture id fix: new Minecraft textures get an id from the reserved low pool. */
@Mixin(TextureUtil.class)
public abstract class TextureUtilMixin {
    @Inject(method = "generateTextureId", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$takeReserved(CallbackInfoReturnable<Integer> cir) {
        int id = TexIdPool.take();
        if (id > 0) {
            cir.setReturnValue(id);
        }
    }
}
