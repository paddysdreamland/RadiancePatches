package dev.paddysdreamland.radiancepatches.mixin.memory;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.radiance.client.vertex.StorageVertexConsumerProvider;
import com.radiance.client.vertex.TransferredBuildBuffer;
import dev.paddysdreamland.radiancepatches.memory.MemoryTracker;
import dev.paddysdreamland.radiancepatches.memory.TrackedAllocator;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Memory tracker: counts the vertex buffers Radiance hands to its native renderer. The native side keeps them until
 * it has uploaded them and then closes them; buffers it never closes would pile up here.
 */
@Mixin(value = TransferredBuildBuffer.class, remap = false)
public abstract class TransferredBuildBufferTrackerMixin {
    @Shadow
    @Final
    private ByteBufferBuilder allocator;

    @Shadow
    @Final
    private AtomicBoolean closed;

    @Unique
    private boolean radiancePatches$tracked;

    @Unique
    private boolean radiancePatches$chunk;

    @Unique
    private long radiancePatches$bytes;

    @Inject(method = "takeChunk", at = @At("RETURN"))
    private static void radiancePatches$tookChunk(SectionBufferBuilderPack storage, RenderType layer, MeshData builtBuffer,
                                                  CallbackInfoReturnable<TransferredBuildBuffer> cir) {
        radiancePatches$track(cir.getReturnValue(), true);
    }

    @Inject(method = "takeEntity", at = @At("RETURN"))
    private static void radiancePatches$tookEntity(StorageVertexConsumerProvider provider, RenderType layer, MeshData builtBuffer,
                                                   CallbackInfoReturnable<TransferredBuildBuffer> cir) {
        radiancePatches$track(cir.getReturnValue(), false);
    }

    @Unique
    private static void radiancePatches$track(TransferredBuildBuffer buffer, boolean chunk) {
        if (buffer != null) {
            TransferredBuildBufferTrackerMixin self = (TransferredBuildBufferTrackerMixin) (Object) buffer;
            self.radiancePatches$tracked = true;
            self.radiancePatches$chunk = chunk;
            self.radiancePatches$bytes = ((TrackedAllocator) self.allocator).radiancePatches$capacity();
            MemoryTracker.handedToRadiance(chunk, self.radiancePatches$bytes);
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void radiancePatches$closed(CallbackInfo ci) {
        // close() only does its work the first time, so only count that one.
        if (this.radiancePatches$tracked && !this.closed.get()) {
            this.radiancePatches$tracked = false;
            MemoryTracker.givenBackByRadiance(this.radiancePatches$chunk, this.radiancePatches$bytes);
        }
    }
}
