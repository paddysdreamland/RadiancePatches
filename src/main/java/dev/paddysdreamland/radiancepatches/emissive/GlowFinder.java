package dev.paddysdreamland.radiancepatches.emissive;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Glow finder (off by default, glow_finder=true in radiance_patches.properties).
 * Lists every texture that a mod draws at full brightness on purpose, so you know which textures are meant to glow.
 * Results go to logs/radiance_patches_glow_finder.txt, together with whether Radiance Patches already makes them
 * glow or whether they need an emission map. Look at things somewhere enclosed with no sky above: outdoors, sky
 * light counts as full even at night, so normal surfaces next to a light source would show up too.
 */
public final class GlowFinder {
    private static final Path FILE = Path.of("logs", "radiance_patches_glow_finder.txt");
    private static final Map<String, String> FOUND = new ConcurrentHashMap<>();
    private static final Map<Long, String> SPRITE_CACHE = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile boolean reflectionFailed;
    private static Field stateField;
    private static Field textureStateField;
    private static Method cutoutTextureMethod;

    private GlowFinder() {
    }

    /** The texture a render type draws with (an atlas for blocks and items, a single file for most mobs). */
    public static ResourceLocation textureOf(RenderType renderType) {
        if (renderType == null || reflectionFailed) {
            return null;
        }
        try {
            if (stateField == null) {
                Class<?> composite = Class.forName("net.minecraft.client.renderer.RenderType$CompositeRenderType");
                Field state = composite.getDeclaredField("state");
                state.setAccessible(true);
                Field textureState = state.getType().getDeclaredField("textureState");
                textureState.setAccessible(true);
                Method cutout = textureState.getType().getDeclaredMethod("cutoutTexture");
                cutout.setAccessible(true);
                textureStateField = textureState;
                cutoutTextureMethod = cutout;
                stateField = state;
            }
            if (!stateField.getDeclaringClass().isInstance(renderType)) {
                return null;
            }
            Object textureState = textureStateField.get(stateField.get(renderType));
            Object texture = cutoutTextureMethod.invoke(textureState);
            return texture instanceof Optional<?> optional && optional.orElse(null) instanceof ResourceLocation location
                ? location : null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            reflectionFailed = true;
            return null;
        }
    }

    /** Called for every vertex drawn at full brightness. */
    public static void record(ResourceLocation texture, String renderTypeName, float u, float v, String status) {
        if (texture == null) {
            return;
        }
        String textureName = spriteAt(texture, u, v);
        String key = textureName + "|" + renderTypeName;
        if (FOUND.putIfAbsent(key, status) == null) {
            write();
        }
    }

    /** For an atlas, the sprite at this spot ("mekanism:liquid/energy"); otherwise the texture file itself. */
    private static String spriteAt(ResourceLocation texture, float u, float v) {
        AbstractTexture loaded = Minecraft.getInstance().getTextureManager().getTexture(texture, null);
        if (!(loaded instanceof TextureAtlas atlas)) {
            return texture.toString();
        }
        long key = ((long) texture.hashCode() << 40) ^ ((long) (u * 16384.0f) << 20) ^ (long) (v * 16384.0f);
        return SPRITE_CACHE.computeIfAbsent(key, ignored -> {
            for (TextureAtlasSprite sprite : atlas.getTextures().values()) {
                if (u >= sprite.getU0() && u <= sprite.getU1() && v >= sprite.getV0() && v <= sprite.getV1()) {
                    return "sprite " + sprite.contents().name();
                }
            }
            return texture + " (unknown spot)";
        });
    }

    private static synchronized void write() {
        List<String> needsMap = new ArrayList<>();
        List<String> handled = new ArrayList<>();
        for (Map.Entry<String, String> entry : FOUND.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            String line = "  " + parts[0] + "\n      drawn as: " + parts[1]
                + "\n      emission map: " + mapPath(parts[0]);
            if ("needs map".equals(entry.getValue())) {
                needsMap.add(line);
            } else {
                handled.add(line + "\n      already glows: " + entry.getValue());
            }
        }
        needsMap.sort(String::compareTo);
        handled.sort(String::compareTo);
        StringBuilder text = new StringBuilder();
        text.append("Radiance Patches glow finder, updated ").append(LocalTime.now().format(TIME)).append("\n");
        text.append("Textures drawn at full brightness on purpose since the game started.\n");
        text.append("Only trust results from an enclosed place with no sky above (underground, a sealed room).\n");
        text.append("Outdoors, sky light counts as full even at night, so blocks next to light sources show up too.\n");
        text.append("Items in your hotbar or hand may also show up; ignore those.\n\n");
        text.append("NEEDS AN EMISSION MAP (").append(needsMap.size()).append(")\n");
        needsMap.forEach(line -> text.append(line).append("\n"));
        text.append("\nALREADY GLOWS THROUGH RADIANCE PATCHES (").append(handled.size()).append(")\n");
        handled.forEach(line -> text.append(line).append("\n"));
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, text.toString(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            System.err.println("[Radiance Patches] Could not write " + FILE + ": " + error);
        }
    }

    /** Where the emission map for a texture goes in a resource pack. */
    private static String mapPath(String textureName) {
        String name = textureName.startsWith("sprite ") ? textureName.substring("sprite ".length()) : textureName;
        int colon = name.indexOf(':');
        if (colon < 0 || name.contains(" ")) {
            return "(unknown)";
        }
        String namespace = name.substring(0, colon);
        String path = name.substring(colon + 1);
        if (!textureName.startsWith("sprite ")) {
            return "assets/" + namespace + "/" + (path.endsWith(".png") ? path.substring(0, path.length() - 4) : path)
                + "_s.png";
        }
        return "assets/" + namespace + "/textures/" + path + "_s.png";
    }
}
