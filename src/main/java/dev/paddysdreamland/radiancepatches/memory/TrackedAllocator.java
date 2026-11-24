package dev.paddysdreamland.radiancepatches.memory;

/** Added to Minecraft's vertex buffers (ByteBufferBuilder) by the memory tracker, to read their current size. */
public interface TrackedAllocator {
    int radiancePatches$capacity();
}
