package dev.paddysdreamland.radiancepatches.fluid;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Sits between Radiance's fluid drawing and the real vertex buffer while a modded fluid is drawn.
 * Radiance draws every fluid except lava with water's textures and water's biome colour. This moves each texture
 * coordinate from the water texture it landed on to the same spot on the fluid's own texture, and swaps the colour
 * for the fluid's own tint while keeping Radiance's shading.
 */
public final class FluidLookConsumer implements VertexConsumer {
    private final VertexConsumer target;
    private final TextureAtlasSprite[] from;
    private final TextureAtlasSprite[] to;
    private final float[] colorScale;
    private final int alpha;

    public FluidLookConsumer(VertexConsumer target, TextureAtlasSprite[] from, TextureAtlasSprite[] to,
                             float[] colorScale, int alpha) {
        this.target = target;
        this.from = from;
        this.to = to;
        this.colorScale = colorScale;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        target.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        target.setColor(scale(red, colorScale[0]), scale(green, colorScale[1]), scale(blue, colorScale[2]),
                        Math.min(alpha, this.alpha));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        for (int i = 0; i < from.length; i++) {
            TextureAtlasSprite source = from[i];
            TextureAtlasSprite destination = to[i];
            if (source == null || destination == null) {
                continue;
            }
            float width = source.getU1() - source.getU0();
            float height = source.getV1() - source.getV0();
            if (width <= 0.0f || height <= 0.0f) {
                continue;
            }
            float localU = (u - source.getU0()) / width;
            float localV = (v - source.getV0()) / height;
            if (localU >= -0.001f && localU <= 1.001f && localV >= -0.001f && localV <= 1.001f) {
                target.setUv(destination.getU0() + localU * (destination.getU1() - destination.getU0()),
                             destination.getV0() + localV * (destination.getV1() - destination.getV0()));
                return this;
            }
        }
        target.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        target.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        target.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        target.setNormal(x, y, z);
        return this;
    }

    private static int scale(int channel, float factor) {
        return Math.max(0, Math.min(255, Math.round(channel * factor)));
    }
}
