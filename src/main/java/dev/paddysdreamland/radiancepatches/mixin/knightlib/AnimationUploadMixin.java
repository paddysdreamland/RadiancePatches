package dev.paddysdreamland.radiancepatches.mixin.knightlib;

import com.mojang.blaze3d.platform.NativeImage;
import dev.paddysdreamland.radiancepatches.knightlib.AnimatedTextureTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Knight Lib textures fix: tags the images Knight Lib uploads frames from. See AnimatedTextureTarget. */
@Mixin(targets = "dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures$Animation", remap = false)
public abstract class AnimationUploadMixin {
    /** The whole sprite sheet; each frame is uploaded from a part of it. */
    @Shadow
    @Final
    private NativeImage sheet;

    /** A blend of two frames, for textures with "interpolate": true. */
    @Shadow
    @Final
    private NativeImage interpolatedFrame;

    @Inject(method = "uploadFrame", at = @At("HEAD"))
    private void radiancePatches$tagSheet(int index, CallbackInfo ci) {
        AnimatedTextureTarget.tag(sheet);
    }

    @Inject(method = "uploadInterpolatedFrame", at = @At("HEAD"))
    private void radiancePatches$tagBlend(CallbackInfo ci) {
        AnimatedTextureTarget.tag(interpolatedFrame);
    }
}
