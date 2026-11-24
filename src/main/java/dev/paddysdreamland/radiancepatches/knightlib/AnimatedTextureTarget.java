package dev.paddysdreamland.radiancepatches.knightlib;

import com.mojang.blaze3d.platform.NativeImage;
import com.radiance.client.texture.TextureTracker;
import com.radiance.mixin_related.extensions.vanilla_resource_tracker.INativeImageExt;

/**
 * Shared state for the Knight Lib textures fix.
 * Knight Lib plays animated textures (Companions' Great Chalice and others) through its own texture class: it reads
 * the whole sprite sheet and uploads one frame of it at a time. Radiance only copies an upload into its own textures
 * when the image is tagged with the texture it belongs to, which vanilla's loading code does and Knight Lib's doesn't.
 * The texture was created but never filled, so it showed whatever was in that GPU memory (random coloured squares).
 * This tags the sheet with its texture right before each upload. Uploads only happen on the render thread.
 */
public final class AnimatedTextureTarget {
    /** Id of the Knight Lib texture being updated right now, -1 when none. */
    private static int current = -1;

    private AnimatedTextureTarget() {
    }

    public static void begin(int textureId) {
        current = textureId;
    }

    public static void end() {
        current = -1;
    }

    /** Tag an image that is about to be uploaded into the current Knight Lib texture. */
    public static void tag(NativeImage image) {
        if (image == null || current < 0) {
            return;
        }
        try {
            ((INativeImageExt) (Object) image).radiance$setTarget(TextureTracker.captureTarget(current));
        } catch (IllegalStateException notPrepared) {
            // Radiance doesn't know this texture yet; leave the upload to Radiance as before.
        }
    }
}
