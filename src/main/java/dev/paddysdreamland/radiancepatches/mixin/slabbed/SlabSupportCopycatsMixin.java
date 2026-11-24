package dev.paddysdreamland.radiancepatches.mixin.slabbed;

import java.lang.reflect.Field;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Slabbed + Copycats crash fix.
 * While a chunk is being built, Slabbed looks at blocks around a slab to work out how far to lower it, and can reach
 * past the edge of the area the chunk builder holds. Slabbed expects this: when the level it reads is the chunk
 * builder's region it catches the error and simply doesn't lower the block. But Copycats hands models its own wrapper
 * around that region (FilteredBlockAndTintGetter), Slabbed doesn't recognise the wrapper, and the error crashes the game
 * ("Tesselating block model", "Index -2 out of bounds for length 9"), every time the chunk loads.
 * This lets Slabbed look inside the Copycats wrapper, so it handles the read the same way it does for the plain region.
 */
@Mixin(targets = "com.slabbed.util.SlabSupport", remap = false)
public abstract class SlabSupportCopycatsMixin {
    @Unique
    private static final String radiancePatches$COPYCATS_WRAPPER =
        "com.copycatsplus.copycats.foundation.copycat.model.FilteredBlockAndTintGetter";

    /** The wrapper's "wrapped" field, or null when it couldn't be found. Looked up once. */
    @Unique
    private static volatile Field radiancePatches$wrappedField;

    @Unique
    private static volatile boolean radiancePatches$lookedUp;

    @Shadow
    private static boolean isRenderRegionView(BlockGetter view) {
        throw new AssertionError();
    }

    @Inject(method = "isRenderRegionView(Lnet/minecraft/world/level/BlockGetter;)Z", at = @At("HEAD"), cancellable = true)
    private static void radiancePatches$lookInsideCopycats(BlockGetter view, CallbackInfoReturnable<Boolean> cir) {
        BlockGetter inner = radiancePatches$unwrap(view);
        if (inner != null && inner != view) {
            // Ask again about the level inside the wrapper (wrappers inside wrappers are unwrapped the same way).
            cir.setReturnValue(isRenderRegionView(inner));
        }
    }

    /** The level inside a Copycats wrapper, or null when the view isn't one. */
    @Unique
    private static BlockGetter radiancePatches$unwrap(BlockGetter view) {
        if (view == null) {
            return null;
        }
        Field field = radiancePatches$wrappedField(view.getClass());
        if (field == null || !field.getDeclaringClass().isInstance(view)) {
            return null;
        }
        try {
            Object inner = field.get(view);
            return inner instanceof BlockGetter getter ? getter : null;
        } catch (IllegalAccessException error) {
            return null;
        }
    }

    @Unique
    private static Field radiancePatches$wrappedField(Class<?> viewClass) {
        if (radiancePatches$lookedUp) {
            return radiancePatches$wrappedField;
        }
        // Find the Copycats wrapper class through the view itself, so this works without Copycats on the build path.
        for (Class<?> type = viewClass; type != null; type = type.getSuperclass()) {
            if (type.getName().equals(radiancePatches$COPYCATS_WRAPPER)) {
                try {
                    radiancePatches$wrappedField = type.getField("wrapped");
                } catch (NoSuchFieldException error) {
                    System.err.println("[Radiance Patches] Copycats' level wrapper has no \"wrapped\" field any more; "
                        + "the Slabbed + Copycats fix can't work with this Copycats version.");
                }
                radiancePatches$lookedUp = true;
                return radiancePatches$wrappedField;
            }
        }
        return null;
    }
}
