package dev.paddysdreamland.radiancepatches.dimension;

import dev.paddysdreamland.radiancepatches.RadiancePatchesConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

/**
 * Dimension profiles: which shader pack profile a dimension uses, from dimension_profiles in the config.
 * Entries look like "tardis_refined:*=1" (every dimension of that mod) or "undergarden:undergarden=2" (one dimension).
 * The number is passed to the shader pack, which has the matching settings group (Advanced Patches: profile 1 =
 * "TARDIS Interiors"). Dimensions that are not listed get 0, meaning Radiance's normal behaviour.
 */
public final class DimensionProfiles {
    private static Map<String, Integer> rules;
    private static ResourceLocation lastDimension;
    private static int lastProfile;

    private DimensionProfiles() {
    }

    /** Profile number (0-255) for the dimension the player is currently in. */
    public static int current() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }
        ResourceLocation dimension = level.dimension().location();
        if (!dimension.equals(lastDimension)) {
            lastDimension = dimension;
            lastProfile = profileFor(dimension);
        }
        return lastProfile;
    }

    private static int profileFor(ResourceLocation dimension) {
        String id = dimension.toString();
        for (Map.Entry<String, Integer> rule : rules().entrySet()) {
            String pattern = rule.getKey();
            boolean matches = pattern.endsWith("*")
                ? id.startsWith(pattern.substring(0, pattern.length() - 1))
                : id.equals(pattern);
            if (matches) {
                return rule.getValue();
            }
        }
        return 0;
    }

    private static Map<String, Integer> rules() {
        if (rules == null) {
            Map<String, Integer> parsed = new LinkedHashMap<>();
            for (String entry : RadiancePatchesConfig.getString(RadiancePatchesConfig.DIMENSION_PROFILES).split(",")) {
                int equals = entry.lastIndexOf('=');
                if (equals <= 0) {
                    continue;
                }
                try {
                    int profile = Integer.parseInt(entry.substring(equals + 1).trim());
                    if (profile >= 0 && profile <= 255) {
                        parsed.put(entry.substring(0, equals).trim(), profile);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip entries that aren't "dimension=number".
                }
            }
            rules = parsed;
        }
        return rules;
    }
}
