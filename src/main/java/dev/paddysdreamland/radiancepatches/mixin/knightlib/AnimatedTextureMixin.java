package dev.paddysdreamland.radiancepatches.mixin.knightlib;

import dev.paddysdreamland.radiancepatches.knightlib.AnimatedTextureTarget;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Knight Lib textures fix: remembers which texture Knight Lib is uploading a frame into. See AnimatedTextureTarget. */
@Mixin(targets = "dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures$KnightLibAnimatedTexture", remap = false)
public abstract class AnimatedTextureMixin {
    @Inject(method = {"uploadFirstFrame", "tickOnRenderThread"}, at = @At("HEAD"))
    private void radiancePatches$begin(CallbackInfo ci) {
        AnimatedTextureTarget.begin(((AbstractTexture) (Object) this).getId());
    }

    @Inject(method = {"uploadFirstFrame", "tickOnRenderThread"}, at = @At("RETURN"))
    private void radiancePatches$end(CallbackInfo ci) {
        AnimatedTextureTarget.end();
    }
}
