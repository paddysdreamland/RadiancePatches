package dev.paddysdreamland.radiancepatches.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared state for the lava shore edges.
 * While a lava block is drawn, its vertices carry which of its 8 neighbours at the same height are solid blocks.
 * The value goes into Radiance's per-vertex emission slot, which the Advanced Patches shader pack reads back to
 * darken lava smoothly where it meets walls. It is stored as (1 + bits) / 16384, so it is far too small to glow
 * in shader packs that don't know about it.
 */
public final class LavaShore {
    /** Value for the lava block being drawn right now on this thread (chunks are built on several threads). */
    private static final ThreadLocal<float[]> CURRENT = ThreadLocal.withInitial(() -> new float[1]);

    private LavaShore() {
    }

    /** The value for a lava block: one bit per solid neighbour, 0 when there are no walls around it. */
    public static float valueFor(BlockAndTintGetter level, BlockPos pos) {
        int walls = 0;
        int bit = 0;
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                neighbour.setWithOffset(pos, dx, 0, dz);
                if (isWall(level.getBlockState(neighbour))) {
                    walls |= 1 << bit;
                }
                bit++;
            }
        }
        return walls == 0 ? 0.0f : (1 + walls) / 16384.0f;
    }

    private static boolean isWall(BlockState state) {
        return !state.isAir() && !state.getFluidState().is(FluidTags.LAVA) && state.blocksMotion();
    }

    public static void begin(float value) {
        CURRENT.get()[0] = value;
    }

    public static void end() {
        CURRENT.get()[0] = 0.0f;
    }

    /** Value for the lava block being drawn right now on this thread, 0 when none. */
    public static float current() {
        return CURRENT.get()[0];
    }
}
