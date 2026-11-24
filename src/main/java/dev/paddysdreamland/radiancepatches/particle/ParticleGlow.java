package dev.paddysdreamland.radiancepatches.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radiance.client.constant.Constants;
import com.radiance.client.proxy.world.EntityProxy;
import com.radiance.client.vertex.PBRVertexConsumer;
import com.radiance.client.vertex.StorageVertexConsumerProvider;
import dev.paddysdreamland.radiancepatches.RadiancePatchesConfig;
import dev.paddysdreamland.radiancepatches.mixin.particle.EntityProxyAccessor;
import dev.paddysdreamland.radiancepatches.mixin.particle.ParticleAccessor;
import dev.paddysdreamland.radiancepatches.mixin.particle.TextureSheetParticleAccessor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared state for the particle glow and particle maps fixes.
 * Radiance paints ordinary particles flat on top of the finished, tone-mapped picture. That is cheap, but a particle
 * painted there can't glow and only uses an emission map's glow, never its normals or roughness.
 * Particles that light themselves up (flames, lava sparks, end rods, soul fire, glow squid sparkles, sculk souls,
 * explosions...) and particles whose texture has _s or _n maps are moved into the ray traced particles instead,
 * the way Radiance already draws particles with their own render code. Self-lit ones get Radiance emission.
 * Everything here runs on the render thread.
 */
public final class ParticleGlow {
    /** Id of the ray traced group these particles are drawn in (any value not used by Radiance itself). */
    private static final long GROUP_ID = 0x5250_5054_474CL;

    private static final String[] PARTICLE_FOLDERS = {"textures/particle/", "textures/particles/"};

    /** Particle sprites (as named in the particle atlas: "minecraft:flame") that have _s, _n or _f maps. */
    private static final Set<ResourceLocation> SPRITES_WITH_MAPS = ConcurrentHashMap.newKeySet();

    /** Whether a particle class picks its own brightness instead of using the light where it is. */
    private static final ClassValue<Boolean> PICKS_OWN_LIGHT = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> current = type; current != null && current != Particle.class; current = current.getSuperclass()) {
                try {
                    current.getDeclaredMethod("getLightColor", float.class);
                    return true;
                } catch (NoSuchMethodException missing) {
                    // Not overridden here, look at the parent class.
                } catch (Throwable error) {
                    // The class can't be inspected (a mod's optional dependency is missing): just check its light.
                    return true;
                }
            }
            return false;
        }
    };

    private static final GlowingConsumer GLOWING = new GlowingConsumer();

    private static Boolean glowEnabled;
    private static Boolean mapsEnabled;
    private static float strength = -1.0f;

    /** The ray traced particles of the frame being built, created on first use. */
    private static StorageVertexConsumerProvider frame;

    private ParticleGlow() {
    }

    /** Start of Radiance's particle rebuild. */
    public static void begin() {
        if (frame != null) {
            // Left over from a rebuild that failed halfway; it was never handed to Radiance.
            frame.close();
            frame = null;
        }
    }

    /**
     * Where a particle should be drawn: the buffer Radiance picked (painted on top) or a ray traced one.
     * Self-lit particles get a buffer that gives every vertex the particle's glow.
     */
    public static VertexConsumer bufferFor(Particle particle, float partialTick, RenderType layer, VertexConsumer painted) {
        if (layer == null) {
            return painted;
        }
        float glow = glowEnabled() ? glowOf(particle, partialTick) : 0.0f;
        if (glow <= 0.0f && !(mapsEnabled() && hasMaps(particle))) {
            return painted;
        }
        if (frame == null) {
            frame = new StorageVertexConsumerProvider(0);
            frame.setEntityBuildContext(Constants.Coordinates.CAMERA_SHIFT.getValue(), false, 0.0, 0.0, 0.0);
        }
        VertexConsumer traced = frame.getBuffer(layer);
        if (glow > 0.0f && traced instanceof PBRVertexConsumer pbr) {
            return GLOWING.reset(pbr, glow);
        }
        return traced;
    }

    /** End of Radiance's particle rebuild: hand the ray traced particles to Radiance with the rest. */
    public static void submit(List<StorageVertexConsumerProvider> providers, EntityProxy.EntityRenderDataList renderData) {
        StorageVertexConsumerProvider built = frame;
        frame = null;
        if (built == null) {
            return;
        }
        // Radiance closes every provider in this list once the frame is built.
        providers.add(built);
        EntityProxy.processWorldEntityRenderData(built, EntityProxyAccessor.radiancePatches$staticEntityKey(GROUP_ID),
            0.0, 0.0, 0.0, Constants.RayTracingFlags.PARTICLE, true, renderData);
    }

    /**
     * How strongly a particle glows: its own block light divided by 15, times the configured strength.
     * Only counts when the particle is brighter than the spot it is in, so particles that just take the light
     * around them (smoke next to a torch, block crumbs) don't glow.
     */
    private static float glowOf(Particle particle, float partialTick) {
        if (!PICKS_OWN_LIGHT.get(particle.getClass())) {
            return 0.0f;
        }
        ParticleAccessor access = (ParticleAccessor) particle;
        int own = Math.min(LightTexture.block(access.radiancePatches$getLightColor(partialTick)), 15);
        if (own <= 0) {
            return 0.0f;
        }
        ClientLevel level = access.radiancePatches$getLevel();
        BlockPos pos = BlockPos.containing(access.radiancePatches$getX(), access.radiancePatches$getY(), access.radiancePatches$getZ());
        int around = level != null && level.hasChunkAt(pos) ? LightTexture.block(LevelRenderer.getLightColor(level, pos)) : 0;
        if (own <= around) {
            return 0.0f;
        }
        if (strength < 0.0f) {
            strength = Math.max(RadiancePatchesConfig.getFloat(RadiancePatchesConfig.PARTICLE_GLOW_STRENGTH), 0.0f);
        }
        return own / 15.0f * strength;
    }

    private static boolean hasMaps(Particle particle) {
        if (SPRITES_WITH_MAPS.isEmpty() || !(particle instanceof TextureSheetParticle)) {
            return false;
        }
        TextureAtlasSprite sprite = ((TextureSheetParticleAccessor) particle).radiancePatches$getSprite();
        return sprite != null && TextureAtlas.LOCATION_PARTICLES.equals(sprite.atlasLocation())
            && SPRITES_WITH_MAPS.contains(sprite.contents().name());
    }

    /** Resource reload started: forget which particles had maps. */
    public static void clearMaps() {
        SPRITES_WITH_MAPS.clear();
    }

    /** Radiance found a map ("minecraft:textures/particle/flame_s.png"); remember the particle sprite it belongs to. */
    public static void recordMap(ResourceLocation file) {
        String path = file.getPath();
        if (path.startsWith("textures/flag/")) {
            path = "textures/" + path.substring("textures/flag/".length());
        }
        for (String folder : PARTICLE_FOLDERS) {
            // "<name>_s.png": drop the folder, the suffix and the extension.
            if (path.startsWith(folder) && path.endsWith(".png") && path.length() > folder.length() + 6) {
                String name = path.substring(folder.length(), path.length() - ".png".length() - 2);
                SPRITES_WITH_MAPS.add(ResourceLocation.fromNamespaceAndPath(file.getNamespace(), name));
                return;
            }
        }
    }

    public static boolean isParticlePath(String path) {
        for (String folder : PARTICLE_FOLDERS) {
            if (path.startsWith(folder)) {
                return true;
            }
        }
        return false;
    }

    private static boolean glowEnabled() {
        if (glowEnabled == null) {
            glowEnabled = RadiancePatchesConfig.isEnabled(RadiancePatchesConfig.PARTICLE_GLOW);
        }
        return glowEnabled;
    }

    private static boolean mapsEnabled() {
        if (mapsEnabled == null) {
            mapsEnabled = RadiancePatchesConfig.isEnabled(RadiancePatchesConfig.PARTICLE_MAPS);
        }
        return mapsEnabled;
    }

    /** A particle has been drawn: give its glowing quads a back side. */
    public static void afterRender() {
        GLOWING.addBackSides();
    }

    /**
     * Passes everything through and gives each vertex the particle's glow. One instance, reused for every particle.
     * It also remembers the vertices, so each glowing quad can be drawn a second time facing the other way.
     * Particles are flat quads turned towards the camera, and bounced-light rays (Advanced Patches: "Particles Light
     * Their Surroundings") skip the back of a surface, so without a back side a flame couldn't light anything behind
     * it. The camera never sees the back side, because camera rays skip back faces too.
     */
    private static final class GlowingConsumer implements VertexConsumer {
        /** Per vertex: x, y, z, u, v (floats) and colour, uv1, uv2 (ints), plus which of the optional parts were set. */
        private static final int FLOATS = 5;
        private static final int INTS = 4;
        private static final int HAS_COLOR = 1;
        private static final int HAS_UV = 2;
        private static final int HAS_UV1 = 4;
        private static final int HAS_UV2 = 8;

        private PBRVertexConsumer target;
        private float glow;
        private float[] floats = new float[4 * FLOATS * 4];
        private int[] ints = new int[4 * INTS * 4];
        private int vertices;

        GlowingConsumer reset(PBRVertexConsumer target, float glow) {
            this.target = target;
            this.glow = glow;
            this.vertices = 0;
            return this;
        }

        /** Draws every finished quad of the particle again with its corners in reverse order, which flips its facing. */
        void addBackSides() {
            if (target == null) {
                return;
            }
            int quads = vertices / 4;
            for (int quad = 0; quad < quads; quad++) {
                for (int corner = 3; corner >= 0; corner--) {
                    int vertex = quad * 4 + corner;
                    int f = vertex * FLOATS;
                    int i = vertex * INTS;
                    int parts = ints[i + 3];
                    target.addVertex(floats[f], floats[f + 1], floats[f + 2]);
                    target.albedoEmission(glow);
                    if ((parts & HAS_UV) != 0) {
                        target.setUv(floats[f + 3], floats[f + 4]);
                    }
                    if ((parts & HAS_COLOR) != 0) {
                        int color = ints[i];
                        target.setColor(color & 0xFF, color >>> 8 & 0xFF, color >>> 16 & 0xFF, color >>> 24);
                    }
                    if ((parts & HAS_UV1) != 0) {
                        target.setUv1(ints[i + 1] & 0xFFFF, ints[i + 1] >>> 16);
                    }
                    if ((parts & HAS_UV2) != 0) {
                        target.setUv2(ints[i + 2] & 0xFFFF, ints[i + 2] >>> 16);
                    }
                }
            }
            target = null;
            vertices = 0;
        }

        private int current() {
            return (vertices - 1) * INTS;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            target.addVertex(x, y, z);
            target.albedoEmission(glow);
            if ((vertices + 1) * FLOATS > floats.length) {
                floats = java.util.Arrays.copyOf(floats, floats.length * 2);
                ints = java.util.Arrays.copyOf(ints, ints.length * 2);
            }
            int f = vertices * FLOATS;
            floats[f] = x;
            floats[f + 1] = y;
            floats[f + 2] = z;
            ints[vertices * INTS + 3] = 0;
            vertices++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            target.setColor(red, green, blue, alpha);
            if (vertices > 0) {
                int i = current();
                ints[i] = red & 0xFF | (green & 0xFF) << 8 | (blue & 0xFF) << 16 | (alpha & 0xFF) << 24;
                ints[i + 3] |= HAS_COLOR;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            target.setUv(u, v);
            if (vertices > 0) {
                int f = (vertices - 1) * FLOATS;
                floats[f + 3] = u;
                floats[f + 4] = v;
                ints[current() + 3] |= HAS_UV;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            target.setUv1(u, v);
            if (vertices > 0) {
                int i = current();
                ints[i + 1] = u & 0xFFFF | (v & 0xFFFF) << 16;
                ints[i + 3] |= HAS_UV1;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            target.setUv2(u, v);
            if (vertices > 0) {
                int i = current();
                ints[i + 2] = u & 0xFFFF | (v & 0xFFFF) << 16;
                ints[i + 3] |= HAS_UV2;
            }
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            // Particles don't set normals; the back side gets its facing from the reversed corner order.
            target.setNormal(x, y, z);
            return this;
        }
    }
}
