package dev.paddysdreamland.radiancepatches.mixin.memory;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import dev.paddysdreamland.radiancepatches.memory.MemoryTracker;
import dev.paddysdreamland.radiancepatches.memory.TrackedAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Memory tracker: counts Minecraft's vertex buffers. They live in native memory that Java's garbage collector never
 * frees, so one that is dropped without being closed stays allocated until the game exits.
 */
@Mixin(ByteBufferBuilder.class)
public abstract class ByteBufferBuilderTrackerMixin implements TrackedAllocator {
    @Shadow
    long pointer;

    @Shadow
    private int capacity;

    @Unique
    private int radiancePatches$capacityBeforeResize;

    @Inject(method = "<init>(I)V", at = @At("TAIL"))
    private void radiancePatches$opened(int initialCapacity, CallbackInfo ci) {
        MemoryTracker.builderOpened(this.capacity);
    }

    @Inject(method = "resize(I)V", at = @At("HEAD"))
    private void radiancePatches$beforeResize(int newSize, CallbackInfo ci) {
        this.radiancePatches$capacityBeforeResize = this.capacity;
    }

    @Inject(method = "resize(I)V", at = @At("TAIL"))
    private void radiancePatches$afterResize(int newSize, CallbackInfo ci) {
        MemoryTracker.builderResized((long) this.capacity - this.radiancePatches$capacityBeforeResize);
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void radiancePatches$closed(CallbackInfo ci) {
        if (this.pointer != 0L) {
            MemoryTracker.builderClosed(this.capacity);
        }
    }

    @Override
    public int radiancePatches$capacity() {
        return this.capacity;
    }
}
