package dev.paddysdreamland.radiancepatches.emf;

import java.util.ArrayDeque;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * EMF fix: remembers which buffer source the mob (or block entity) currently being drawn uses,
 * so Entity Model Features parts with their own texture can draw into that same buffer.
 */
public final class CurrentBuffers {
    private static final ThreadLocal<ArrayDeque<MultiBufferSource>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final MultiBufferSource NONE = renderType -> {
        throw new IllegalStateException("No buffer source");
    };
    private static boolean announced;

    private CurrentBuffers() {
    }

    public static void push(MultiBufferSource buffers) {
        ArrayDeque<MultiBufferSource> stack = STACK.get();
        if (stack.size() > 64) {
            // Something never popped; start over rather than grow forever.
            stack.clear();
        }
        stack.push(buffers == null ? NONE : buffers);
    }

    public static void pop() {
        ArrayDeque<MultiBufferSource> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /** The buffer source of whatever is being drawn right now, or null. */
    public static MultiBufferSource current() {
        MultiBufferSource buffers = STACK.get().peek();
        return buffers == NONE ? null : buffers;
    }

    public static void announceOnce() {
        if (!announced) {
            announced = true;
            System.out.println("[Radiance Patches] Routing EMF texture-override parts into the mob's own buffer.");
        }
    }
}
