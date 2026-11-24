package dev.paddysdreamland.radiancepatches.mixin.radiance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.radiance.client.proxy.world.ChunkProxy;
import dev.paddysdreamland.radiancepatches.memory.MemoryTracker;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chunk buffer leak fix.
 * Every thread that builds chunks for Radiance gets its own set of vertex buffers (about 10 MB of native memory).
 * Whenever the world's chunk grid is rebuilt (joining a world, changing dimension, changing render distance or video
 * settings, F3+A, reloading resources) Radiance stops its chunk-building threads and starts new ones. The old threads'
 * buffer sets were never closed, and Java's garbage collector doesn't free native memory, so each rebuild leaked one
 * set per thread: hundreds of MB per dimension change, piling up until RAM ran out.
 * This remembers which thread owns which set and closes the sets of threads that have finished.
 * (Buffers Radiance already handed to its native renderer were swapped out of the set first, so they aren't touched.)
 */
@Mixin(value = ChunkProxy.class, remap = false)
public abstract class ChunkBufferLeakMixin {
    @Unique
    private static final Map<Thread, SectionBufferBuilderPack> radiancePatches$packs = new ConcurrentHashMap<>();

    @WrapOperation(
        method = {"scopedBlockBufferAllocatorStorage()Ljava/lang/AutoCloseable;",
                  "rebuildSingle(Lcom/radiance/client/proxy/world/ChunkProxy$ChunkBuild;Z)V"},
        at = @At(value = "INVOKE", target = "Ljava/lang/ThreadLocal;get()Ljava/lang/Object;"),
        require = 2
    )
    private static Object radiancePatches$rememberOwner(ThreadLocal<?> local, Operation<Object> original) {
        Object value = original.call(local);
        if (value instanceof SectionBufferBuilderPack pack) {
            Thread thread = Thread.currentThread();
            if (!radiancePatches$packs.containsKey(thread) && radiancePatches$packs.putIfAbsent(thread, pack) == null) {
                // A thread we haven't seen: new chunk-building threads usually mean old ones have just finished.
                radiancePatches$closeFinishedThreads();
            }
        }
        return value;
    }

    @Inject(method = "clear()V", at = @At("TAIL"))
    private static void radiancePatches$afterClear(CallbackInfo ci) {
        radiancePatches$closeFinishedThreads();
        MemoryTracker.note("chunk grid rebuilt (Radiance restarted its chunk-building threads)");
    }

    @Unique
    private static void radiancePatches$closeFinishedThreads() {
        int closed = 0;
        for (Iterator<Map.Entry<Thread, SectionBufferBuilderPack>> it = radiancePatches$packs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Thread, SectionBufferBuilderPack> entry = it.next();
            if (!entry.getKey().isAlive()) {
                it.remove();
                try {
                    entry.getValue().close();
                    closed++;
                } catch (Throwable error) {
                    System.err.println("[Radiance Patches] Could not free a finished chunk thread's buffers: " + error);
                }
            }
        }
        if (closed > 0) {
            MemoryTracker.note("freed the buffers of " + closed + " finished chunk-building thread(s)");
        }
    }
}
