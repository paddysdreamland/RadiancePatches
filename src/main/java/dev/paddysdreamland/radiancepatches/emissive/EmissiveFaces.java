package dev.paddysdreamland.radiancepatches.emissive;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.paddysdreamland.radiancepatches.RadiancePatchesConfig;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

/**
 * Shared state for the emissive faces fix.
 * Models can mark single faces as glowing ("neoforge_data": {"block_light": 15, ...} in the model JSON,
 * used by Undergarden's shimmerweed, droopvine and others). Minecraft bakes that into the face's light value,
 * which only makes it fullbright in vanilla. Radiance ignores it, so this turns it into real Radiance emission.
 */
public final class EmissiveFaces {
    /** Ints per vertex in a baked quad, and where the light value sits inside one vertex. */
    private static final int VERTEX_INTS = 8;
    private static final int LIGHT_INT = 6;

    /** How far glowing faces are lifted off the face they are drawn on top of (in blocks). */
    private static final float LIFT = 0.002f;

    /** Glow of the face currently being drawn on this thread (chunks are built on several threads). */
    private static final ThreadLocal<float[]> CURRENT = ThreadLocal.withInitial(() -> new float[1]);

    private static float strength = -1.0f;

    private EmissiveFaces() {
    }

    /** Glow for a face: its baked block light divided by 15, times the configured strength. 0 = not glowing. */
    public static float glowOf(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        int highest = 0;
        for (int i = LIGHT_INT; i < vertices.length; i += VERTEX_INTS) {
            highest = Math.max(highest, LightTexture.block(vertices[i]));
        }
        if (highest == 0) {
            return 0.0f;
        }
        if (strength < 0.0f) {
            strength = Math.max(RadiancePatchesConfig.getFloat(RadiancePatchesConfig.EMISSIVE_FACES_STRENGTH), 0.0f);
        }
        return highest / 15.0f * strength;
    }

    /** Copy of the pose moved a hair along the face's direction, so a glowing overlay sits on top. */
    public static PoseStack.Pose lifted(PoseStack.Pose pose, BakedQuad quad) {
        Direction direction = quad.getDirection();
        if (direction == null) {
            return pose;
        }
        PoseStack.Pose copy = pose.copy();
        copy.pose().translate(direction.getStepX() * LIFT, direction.getStepY() * LIFT, direction.getStepZ() * LIFT);
        return copy;
    }

    public static void begin(float glow) {
        CURRENT.get()[0] = glow;
    }

    public static void end() {
        CURRENT.get()[0] = 0.0f;
    }

    /** Glow of the face being drawn right now on this thread, 0 when none. */
    public static float current() {
        return CURRENT.get()[0];
    }
}
