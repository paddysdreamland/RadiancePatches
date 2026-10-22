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

    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        DESCRIPTIONS.put(VEIL, "No crash when Veil (Create Aeronautics, Levitite) draws vanilla chunk layers.");
        DESCRIPTIONS.put(TEXTURE_IDS, "Keeps texture ids below 4096 so Radiance doesn't crash when mods make thousands of textures.");
        DESCRIPTIONS.put(EMF, "Entity Model Features parts with their own texture no longer float at the camera.");
        DESCRIPTIONS.put(LEVEL_RENDER_MODELS, "Models drawn by mods during level render events (Create Overpressure tubes, CarryOn) are ray traced instead of painted flat on top.");
        DESCRIPTIONS.put(BLOCK_ENTITY_CRASH, "No crash when the list of always-visible block entities changes while Radiance reads it.");
        DESCRIPTIONS.put(MODDED_VILLAGER_LAYERS, "Profession and level layers of modded villagers no longer hide under the base skin.");
        DESCRIPTIONS.put(EXTRA_PBR_FOLDERS, "_s, _n and _f maps also work in textures/blockentity, block_entity, ctm, liquid and fluid.");
    }

    private static final Path FILE = Path.of("config", "radiance_patches.properties");
    private static Properties values;

    private RadiancePatchesConfig() {
    }

    /** True when the given fix is switched on (every fix is on by default). */
    public static synchronized boolean isEnabled(String key) {
        if (values == null) {
            load();
        }
        return !"false".equalsIgnoreCase(values.getProperty(key, "true").trim());
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
        text.append("# Radiance Patches: true = fix on, false = fix off. Restart the game after changing.\n\n");
        for (Map.Entry<String, String> entry : DESCRIPTIONS.entrySet()) {
            text.append("# ").append(entry.getValue()).append('\n');
            text.append(entry.getKey()).append('=').append(values.getProperty(entry.getKey(), "true").trim()).append("\n\n");
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
