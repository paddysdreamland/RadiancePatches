package dev.paddysdreamland.radiancepatches.stage;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.radiance.client.constant.Constants;
import com.radiance.client.proxy.world.EntityProxy;
import com.radiance.client.vertex.StorageVertexConsumerProvider;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;

/**
 * Level render models fix.
 * Models that mods draw during NeoForge's level render events (Create Overpressure tube sections,
 * CarryOn carried blocks and mobs, ...) are captured by Radiance, but only painted flat on top of
 * the finished image. This takes the textured models from those events, rebuilds them in
 * Radiance's own vertex format and queues them as ray traced geometry instead.
 * Lines, outlines, glint, block cracks and anything drawn through walls stay as they were.
 */
public final class StageModelCapture {
    // Key prefix Radiance itself doesn't use (it uses 0x1 to 0x6).
    private static final long KEY_PREFIX = 0x7000000000000000L;
    private static final long KEY_PAYLOAD_MASK = 0x0fffffffffffffffL;

    private static final Map<Object, Long> STAGE_KEYS = new IdentityHashMap<>();
    private static boolean active;
    private static StorageVertexConsumerProvider provider;

    private static boolean reflectionReady;
    private static Field stateField;
    private static Field depthTestField;
    private static Field functionNameField;

    private StageModelCapture() {
    }

    /** Called when Radiance starts capturing an outermost level render stage. */
    public static void begin() {
        if (provider != null) {
            provider.close();
            provider = null;
        }
        active = true;
    }

    /** Called when that capture finished. Queues whatever was collected as ray traced geometry. */
    public static void end(Object stage) {
        active = false;
        StorageVertexConsumerProvider collected = provider;
        provider = null;
        if (collected == null) {
            return;
        }
        try {
            long key = STAGE_KEYS.computeIfAbsent(stage,
                ignored -> KEY_PREFIX | (mix64(0x5354414745L + STAGE_KEYS.size()) & KEY_PAYLOAD_MASK));
            EntityProxy.EntityRenderDataList list = new EntityProxy.EntityRenderDataList();
            EntityProxy.processWorldEntityRenderData(collected, key, 0.0, 0.0, 0.0,
                Constants.RayTracingFlags.WORLD, true, list);
            if (list.isEmpty()) {
                collected.close();
                return;
            }
            EntityProxy.queueBuild(List.of(collected), list, 0.0075f, Constants.Coordinates.CAMERA_SHIFT, false);
        } catch (RuntimeException | Error error) {
            collected.close();
            throw error;
        }
    }

    /**
     * Called for every mesh Radiance would paint on top of the image.
     * Returns true when the mesh was taken over (and closed) here.
     */
    public static boolean tryCapture(RenderType layer, MeshData mesh) {
        if (!active || layer == null || mesh == null || !EntityProxy.isEntityNativeTransferEnabled()) {
            return false;
        }
        MeshData.DrawState drawState = mesh.drawState();
        VertexFormat format = drawState.format();
        if (drawState.mode() != VertexFormat.Mode.QUADS || layer.mode() != VertexFormat.Mode.QUADS) {
            return false;
        }
        boolean isEntityFormat = format == DefaultVertexFormat.NEW_ENTITY;
        if (!isEntityFormat && format != DefaultVertexFormat.BLOCK) {
            return false;
        }
        String name = renderTypeName(layer);
        if (name.contains("glint") || name.contains("crumbling") || name.contains("outline")) {
            return false;
        }
        if (drawsThroughWalls(layer)) {
            return false;
        }
        int vertexCount = drawState.vertexCount();
        if (vertexCount <= 0 || vertexCount % 4 != 0) {
            return false;
        }

        int stride = format.getVertexSize();
        int positionOffset = format.getOffset(VertexFormatElement.POSITION);
        int colorOffset = format.getOffset(VertexFormatElement.COLOR);
        int uvOffset = format.getOffset(VertexFormatElement.UV0);
        int overlayOffset = isEntityFormat ? format.getOffset(VertexFormatElement.UV1) : -1;
        int lightOffset = format.getOffset(VertexFormatElement.UV2);
        int normalOffset = format.getOffset(VertexFormatElement.NORMAL);
        if (stride <= 0 || positionOffset < 0 || colorOffset < 0 || uvOffset < 0 || lightOffset < 0 || normalOffset < 0) {
            return false;
        }

        ByteBuffer vertices = mesh.vertexBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);
        if (vertices.remaining() < vertexCount * stride) {
            return false;
        }
        int start = vertices.position();

        if (provider == null) {
            provider = new StorageVertexConsumerProvider(0);
            provider.setEntityBuildContext(Constants.Coordinates.CAMERA_SHIFT.getValue(), false, 0.0, 0.0, 0.0);
        }
        VertexConsumer out = provider.getBuffer(layer);

        for (int i = 0; i < vertexCount; i++) {
            int base = start + i * stride;
            out.addVertex(
                vertices.getFloat(base + positionOffset),
                vertices.getFloat(base + positionOffset + 4),
                vertices.getFloat(base + positionOffset + 8));
            out.setColor(
                vertices.get(base + colorOffset) & 0xFF,
                vertices.get(base + colorOffset + 1) & 0xFF,
                vertices.get(base + colorOffset + 2) & 0xFF,
                vertices.get(base + colorOffset + 3) & 0xFF);
            out.setUv(vertices.getFloat(base + uvOffset), vertices.getFloat(base + uvOffset + 4));
            if (overlayOffset >= 0) {
                out.setUv1(vertices.getShort(base + overlayOffset) & 0xFFFF,
                    vertices.getShort(base + overlayOffset + 2) & 0xFFFF);
            }
            out.setUv2(vertices.getShort(base + lightOffset) & 0xFFFF, vertices.getShort(base + lightOffset + 2) & 0xFFFF);
            out.setNormal(
                vertices.get(base + normalOffset) / 127.0f,
                vertices.get(base + normalOffset + 1) / 127.0f,
                vertices.get(base + normalOffset + 2) / 127.0f);
        }

        mesh.close();
        return true;
    }

    /** The render type's own name ("entity_cutout", "overpressure_curve_tube", ...), not its full state. */
    private static String renderTypeName(RenderType layer) {
        String text = layer.toString();
        int start = text.indexOf('[');
        int end = text.indexOf(':');
        if (text.startsWith("RenderType[") && start >= 0 && end > start) {
            return text.substring(start + 1, end);
        }
        return text;
    }

    /** True for render types that ignore depth (x-ray style highlights); those stay as an overlay. */
    private static boolean drawsThroughWalls(RenderType layer) {
        try {
            if (!reflectionReady) {
                reflectionReady = true;
                Class<?> composite = Class.forName("net.minecraft.client.renderer.RenderType$CompositeRenderType");
                stateField = composite.getDeclaredField("state");
                stateField.setAccessible(true);
                depthTestField = stateField.getType().getDeclaredField("depthTestState");
                depthTestField.setAccessible(true);
                functionNameField = depthTestField.getType().getDeclaredField("functionName");
                functionNameField.setAccessible(true);
            }
            if (stateField == null || !stateField.getDeclaringClass().isInstance(layer)) {
                return false;
            }
            Object state = stateField.get(layer);
            Object depthTest = state == null ? null : depthTestField.get(state);
            Object function = depthTest == null ? null : functionNameField.get(depthTest);
            return "always".equals(function);
        } catch (ReflectiveOperationException | RuntimeException error) {
            stateField = null;
            return false;
        }
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
