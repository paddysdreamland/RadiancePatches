package dev.paddysdreamland.radiancepatches.texid;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Texture id fix.
 * Radiance can only handle OpenGL texture ids below 4096. Some mods (Xaero's World Map and
 * others) create and delete thousands of textures, which pushes new ids above that and crashes
 * Radiance. This pool reserves a block of low ids at startup, hands them to Minecraft's own
 * textures, and takes them back (cleared) when those textures are deleted, so they get reused.
 */
public final class TexIdPool {
    public static final int LIMIT = 4096;

    private static final Logger LOG = LoggerFactory.getLogger("RadiancePatches/TextureIds");

    // OpenGL constants, named for readability.
    private static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_TEXTURE_IMMUTABLE_FORMAT = 0x912F;
    private static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    private static final int GL_PIXEL_UNPACK_BUFFER_BINDING = 0x88EF;
    private static final int GL_TEXTURE_WIDTH = 0x1000;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    private static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    private static final int GL_TEXTURE_WRAP_S = 0x2802;
    private static final int GL_TEXTURE_WRAP_T = 0x2803;
    private static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    private static final int GL_LINEAR = 0x2601;
    private static final int GL_REPEAT = 0x2901;
    private static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
    private static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    private static final int GL_TEXTURE_MIN_LOD = 0x813A;
    private static final int GL_TEXTURE_MAX_LOD = 0x813B;
    private static final int GL_TEXTURE_LOD_BIAS = 0x8501;
    private static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
    private static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    private static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    private static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    private static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;
    private static final int GL_RED = 0x1903;
    private static final int GL_GREEN = 0x1904;
    private static final int GL_BLUE = 0x1905;
    private static final int GL_ALPHA = 0x1906;

    private static boolean initialized;
    private static int mipLevelCount = 1;
    private static final BitSet owned = new BitSet(LIMIT);
    private static final BitSet free = new BitSet(LIMIT);
    private static int freeCount;
    private static int handedOut;
    private static int recycled;
    private static int fallbacks;
    private static boolean warnedLow;
    private static boolean warnedEmpty;

    private TexIdPool() {
    }

    /** Reserves as many ids below the limit as OpenGL gives out, in batches. */
    private static void init() {
        initialized = true;
        int maxSize = Math.max(1, GL11.glGetInteger(GL_MAX_TEXTURE_SIZE));
        mipLevelCount = 32 - Integer.numberOfLeadingZeros(maxSize);
        int[] batch = new int[256];
        for (int round = 0; round < 64; round++) {
            GL11.glGenTextures(batch);
            List<Integer> giveBack = new ArrayList<>();
            boolean reachedLimit = false;
            for (int id : batch) {
                if (id > 0 && id < LIMIT && !owned.get(id)) {
                    owned.set(id);
                    free.set(id);
                    freeCount++;
                    continue;
                }
                giveBack.add(id);
                if (id >= LIMIT) {
                    reachedLimit = true;
                }
            }
            if (!giveBack.isEmpty()) {
                GL11.glDeleteTextures(giveBack.stream().mapToInt(Integer::intValue).toArray());
            }
            if (reachedLimit) {
                break;
            }
        }
        LOG.info("Reserved {} texture ids below {} for Minecraft-managed textures", freeCount, LIMIT);
    }

    /** Hands out a reserved id, or -1 when none are left (Minecraft then uses a normal id). */
    public static synchronized int take() {
        if (!initialized) {
            init();
        }
        int id = free.nextSetBit(1);
        if (id < 0) {
            fallbacks++;
            if (!warnedEmpty) {
                warnedEmpty = true;
                LOG.warn("All {} reserved texture ids are in use; new textures get normal ids again (Radiance may crash if one goes above {})",
                    owned.cardinality(), LIMIT);
            }
            return -1;
        }
        free.clear(id);
        handedOut++;
        if (--freeCount < 128 && !warnedLow) {
            warnedLow = true;
            LOG.warn("Only {} reserved texture ids left ({} handed out, {} recycled so far)", freeCount, handedOut, recycled);
        }
        return id;
    }

    public static synchronized boolean isOwned(int id) {
        return id > 0 && id < LIMIT && owned.get(id);
    }

    public static synchronized boolean anyOwned(int[] ids) {
        for (int id : ids) {
            if (isOwned(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes a reserved id back instead of deleting it: its image data is cleared and its
     * settings reset to OpenGL defaults, so the next texture that gets it starts clean.
     * Returns false when the id isn't one of ours (Minecraft deletes it normally then).
     */
    public static synchronized boolean recycle(int id) {
        if (!isOwned(id)) {
            return false;
        }
        if (free.get(id)) {
            return true;
        }
        int previousBinding = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL_TEXTURE_2D, id);
        if (GL.getCapabilities().OpenGL42 && GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_IMMUTABLE_FORMAT) != 0) {
            // Fixed-size storage can't be cleared; give this id up for good.
            GL11.glBindTexture(GL_TEXTURE_2D, previousBinding);
            owned.clear(id);
            return false;
        }
        int unpackBuffer = GL11.glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        if (unpackBuffer != 0) {
            GL15.glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        }
        for (int level = 0; level < mipLevelCount; level++) {
            if (GL11.glGetTexLevelParameteri(GL_TEXTURE_2D, level, GL_TEXTURE_WIDTH) > 0) {
                GL11.glTexImage2D(GL_TEXTURE_2D, level, GL_RGBA8, 0, 0, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            }
        }
        if (unpackBuffer != 0) {
            GL15.glBindBuffer(GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        }
        GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 1000);
        GL11.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, -1000.0f);
        GL11.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 1000.0f);
        GL11.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f);
        GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, 0);
        if (GL.getCapabilities().OpenGL33) {
            GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_RED);
            GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_GREEN);
            GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_BLUE);
            GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
        }
        GL11.glBindTexture(GL_TEXTURE_2D, previousBinding);
        free.set(id);
        freeCount++;
        if (++recycled % 2000 == 0) {
            LOG.info("Texture id pool: {}", stats());
        }
        return true;
    }

    public static synchronized String stats() {
        return "reserved=" + owned.cardinality() + " free=" + freeCount + " handedOut=" + handedOut
            + " recycled=" + recycled + " fallbacks=" + fallbacks;
    }
}
