package dev.paddysdreamland.radiancepatches;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The on/off switches for each fix, kept in config/radiance_patches.properties.
 * This is a plain properties file because it has to be read before Minecraft's own
 * config system exists (fixes are decided while the game classes are being loaded).
 */
public final class RadiancePatchesConfig {
    public static final String VEIL = "veil_fix";
    public static final String TEXTURE_IDS = "texture_id_fix";
    public static final String EMF = "emf_fix";
    public static final String LEVEL_RENDER_MODELS = "level_render_models_fix";
    public static final String BLOCK_ENTITY_CRASH = "block_entity_crash_fix";
    public static final String MODDED_VILLAGER_LAYERS = "modded_villager_layers_fix";
    public static final String EXTRA_PBR_FOLDERS = "extra_pbr_folders_fix";
    public static final String ARMOR_GLINT = "armor_glint_fix";
    public static final String OVERLAY_LAYERS = "overlay_layers_fix";
    public static final String GLOW_LAYERS = "glow_layers_fix";
    public static final String GLOW_LAYERS_STRENGTH = "glow_layers_strength";
    public static final String EMISSIVE_FACES = "emissive_faces_fix";
    public static final String EMISSIVE_FACES_STRENGTH = "emissive_faces_strength";
    public static final String GLOW_FINDER = "glow_finder";
    public static final String MODDED_FLUIDS = "modded_fluids_fix";
    public static final String DIMENSION_PROFILES_SWITCH = "dimension_profiles_fix";
    public static final String DIMENSION_PROFILES = "dimension_profiles";
    public static final String EXTRA_MAP_ROOTS = "extra_map_roots";
    public static final String LAVA_SHORE = "lava_shore_edges";
    public static final String ARM_MARKER = "first_person_arm_marker";
    public static final String UNKNOWN_TRANSPARENCY = "unknown_transparency_fix";
    public static final String SLABBED_COPYCATS = "slabbed_copycats_crash_fix";
    public static final String MEMORY_TRACKER = "memory_tracker";
    public static final String CHUNK_BUFFER_LEAK = "chunk_buffer_leak_fix";
    public static final String PARTICLE_GLOW = "particle_glow_fix";
    public static final String PARTICLE_GLOW_STRENGTH = "particle_glow_strength";
    public static final String PARTICLE_MAPS = "particle_maps_fix";
    public static final String PARTICLE_CULLING = "particle_culling_fix";
    public static final String KNIGHTLIB_TEXTURES = "knightlib_textures_fix";

    /** Settings whose default is not "true". */
    private static final Map<String, String> DEFAULTS = Map.of(GLOW_LAYERS_STRENGTH, "1.0", EMISSIVE_FACES_STRENGTH, "1.0", GLOW_FINDER, "false", MEMORY_TRACKER, "false", EXTRA_MAP_ROOTS, "render",
        DIMENSION_PROFILES, "tardis_refined:*=1", PARTICLE_GLOW_STRENGTH, "1.0");

    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        DESCRIPTIONS.put(VEIL, "No crash when Veil (Create Aeronautics, Levitite) draws vanilla chunk layers.");
        DESCRIPTIONS.put(TEXTURE_IDS, "Keeps texture ids below 4096 so Radiance doesn't crash when mods make thousands of textures.");
        DESCRIPTIONS.put(EMF, "Entity Model Features parts with their own texture no longer float at the camera.");
        DESCRIPTIONS.put(LEVEL_RENDER_MODELS, "Models drawn by mods during level render events (Create Overpressure tubes, CarryOn) are ray traced instead of painted flat on top.");
        DESCRIPTIONS.put(UNKNOWN_TRANSPARENCY, "No \"Invalid render layer\" crash when mods draw see-through layers Radiance doesn't recognise (GeckoLib glowmasks: Companions mobs and effects, and others).");
        DESCRIPTIONS.put(SLABBED_COPYCATS, "No \"Tesselating block model\" crash when Slabbed lowers Copycats blocks near a chunk edge. Only does anything when Slabbed is installed.");
        DESCRIPTIONS.put(KNIGHTLIB_TEXTURES, "Knight Lib's animated textures (Companions' Great Chalice and others) show their real texture instead of random coloured squares. Only does anything when Knight Lib is installed.");
        DESCRIPTIONS.put(CHUNK_BUFFER_LEAK, "Frees the vertex buffers of Radiance's old chunk-building threads. Without it every dimension change, render distance or video settings change, F3+A or resource reload leaks a few hundred MB of RAM.");
        DESCRIPTIONS.put(BLOCK_ENTITY_CRASH, "No crash when the list of always-visible block entities changes while Radiance reads it.");
        DESCRIPTIONS.put(MODDED_VILLAGER_LAYERS, "Profession and level layers of modded villagers no longer hide under the base skin.");
        DESCRIPTIONS.put(EXTRA_PBR_FOLDERS, "_s, _n and _f maps also work in textures/blockentity, block_entity, ctm, liquid and fluid.");
        DESCRIPTIONS.put(EXTRA_MAP_ROOTS, "Folders outside textures/ where some mods keep textures (Mekanism: assets/mekanism/render/). _s and _n maps placed next to those textures work too. Comma-separated; needs extra_pbr_folders_fix.");
        DESCRIPTIONS.put(OVERLAY_LAYERS, "Extra layers on mobs (glowing eyes, emissive patterns, clothes) always sit on top of the skin instead of randomly hiding under it.");
        DESCRIPTIONS.put(GLOW_LAYERS, "Layers drawn to glow (mob eyes, the Warden's spots, emissive overlays like TARDIS shell lights, Create's Wand of Symmetry core, lightning bolts, TARDIS Refined's screwdriver selection) glow by themselves, no emission maps needed.");
        DESCRIPTIONS.put(GLOW_LAYERS_STRENGTH, "How bright those layers glow (1.0 = like a full-strength emission map).");
        DESCRIPTIONS.put(EMISSIVE_FACES, "Block and item faces that models mark as glowing (\"neoforge_data\": {\"block_light\": ...}, Undergarden's shimmerweed and others) really glow, no emission maps needed.");
        DESCRIPTIONS.put(EMISSIVE_FACES_STRENGTH, "How bright those faces glow at block_light 15 (1.0 = like a full-strength emission map). Lower block_light values glow less.");
        DESCRIPTIONS.put(PARTICLE_CULLING, "Particles outside the view are skipped like in NeoForge, instead of Radiance rebuilding every particle every frame. Less lag when lots of blocks break at once.");
        DESCRIPTIONS.put(PARTICLE_GLOW,"Particles that light themselves up (flames, lava sparks, end rods, soul fire, glow squid sparkles, sculk souls, explosions) are ray traced and really glow, instead of being painted flat on top of the picture.");
        DESCRIPTIONS.put(PARTICLE_GLOW_STRENGTH, "How bright those particles glow at full brightness (1.0 = like a full-strength emission map). An emission map on the particle wins where it has a glow value.");
        DESCRIPTIONS.put(PARTICLE_MAPS, "_s, _n and _f maps also work for particles (textures/particle and textures/particles). Particles with maps are ray traced so the whole map applies, not just its glow.");
        DESCRIPTIONS.put(MODDED_FLUIDS, "Modded fluids use their own textures and colour instead of water's, and stop counting as water. Vanilla water and lava are unchanged.");
        DESCRIPTIONS.put(LAVA_SHORE, "Lava remembers which blocks next to it are walls, so the shader pack can darken lava along the shore (Advanced Patches: \"Shore Darkening\").");
        DESCRIPTIONS.put(ARM_MARKER, "Marks the first-person arm so the shader pack can tell it apart from the held item (Advanced Patches: \"Hand Opacity\"). The marker is invisible.");
        DESCRIPTIONS.put(DIMENSION_PROFILES_SWITCH, "Tell the shader pack which dimension you are in, so it can give some dimensions their own sky (Advanced Patches: \"TARDIS Interiors\").");
        DESCRIPTIONS.put(DIMENSION_PROFILES, "Which shader pack profile each dimension uses, comma-separated. \"mod:*\" matches every dimension of a mod. Profile 1 = TARDIS Interiors in Advanced Patches.");
        DESCRIPTIONS.put(GLOW_FINDER, "Tool, off by default: lists every texture mods draw at full brightness on purpose in logs/radiance_patches_glow_finder.txt, and whether it still needs an emission map. Use it somewhere enclosed with no sky above (underground or a sealed room).");
        DESCRIPTIONS.put(MEMORY_TRACKER, "Tool, off by default: every 30 seconds writes where the game's memory is (Java heap, native memory, vertex buffers Radiance still holds) to logs/radiance_patches_memory.txt, to find what grows when memory use climbs past what you allocated.");
        DESCRIPTIONS.put(ARMOR_GLINT, "Enchanted armor gets Radiance's moving glint instead of a solid, still purple layer.");
    }

    private static final Path FILE = Path.of("config", "radiance_patches.properties");
    private static Properties values;

    private RadiancePatchesConfig() {
    }

    /** True when the given fix is switched on (on by default, unless DEFAULTS says otherwise). */
    public static synchronized boolean isEnabled(String key) {
        if (values == null) {
            load();
        }
        return !"false".equalsIgnoreCase(values.getProperty(key, defaultValue(key)).trim());
    }

    /** A number setting, falling back to its default when the value is missing or not a number. */
    public static synchronized float getFloat(String key) {
        if (values == null) {
            load();
        }
        try {
            return Float.parseFloat(values.getProperty(key, defaultValue(key)).trim());
        } catch (NumberFormatException error) {
            return Float.parseFloat(defaultValue(key));
        }
    }

    /** A text setting, falling back to its default when missing. */
    public static synchronized String getString(String key) {
        if (values == null) {
            load();
        }
        return values.getProperty(key, defaultValue(key)).trim();
    }

    private static String defaultValue(String key) {
        return DEFAULTS.getOrDefault(key, "true");
    }

    private static void load() {
        values = new Properties();
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                values.load(reader);
            } catch (IOException | IllegalArgumentException error) {
                System.err.println("[Radiance Patches] Could not read " + FILE + ", using defaults: " + error);
            }
        }
        // Write the file back so new switches appear with their explanation.
        boolean missing = DESCRIPTIONS.keySet().stream().anyMatch(key -> !values.containsKey(key));
        if (missing || !Files.exists(FILE)) {
            save();
        }
    }

    private static void save() {
        StringBuilder text = new StringBuilder();
        text.append("# Radiance Patches: true = fix on, false = fix off, numbers are settings. Restart the game after changing.\n\n");
        for (Map.Entry<String, String> entry : DESCRIPTIONS.entrySet()) {
            text.append("# ").append(entry.getValue()).append('\n');
            text.append(entry.getKey()).append('=').append(values.getProperty(entry.getKey(), defaultValue(entry.getKey())).trim()).append("\n\n");
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                writer.write(text.toString());
            }
        } catch (IOException error) {
            System.err.println("[Radiance Patches] Could not write " + FILE + ": " + error);
        }
    }
}
