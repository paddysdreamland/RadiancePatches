package dev.paddysdreamland.radiancepatches;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Leaves out the mixins of any fix that is switched off in config/radiance_patches.properties. */
public final class RadiancePatchesMixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "dev.paddysdreamland.radiancepatches.mixin.";

    private static final Map<String, String> FIX_FOR_MIXIN = Map.ofEntries(
        Map.entry("veil.LevelRendererMixin", RadiancePatchesConfig.VEIL),
        Map.entry("texid.GlStateManagerMixin", RadiancePatchesConfig.TEXTURE_IDS),
        Map.entry("texid.TextureUtilMixin", RadiancePatchesConfig.TEXTURE_IDS),
        Map.entry("emf.EntityRenderDispatcherMixin", RadiancePatchesConfig.EMF),
        Map.entry("emf.BlockEntityRenderDispatcherMixin", RadiancePatchesConfig.EMF),
        Map.entry("emf.EMFModelPartMixin", RadiancePatchesConfig.EMF),
        Map.entry("radiance.EntityProxyStageMixin", RadiancePatchesConfig.LEVEL_RENDER_MODELS),
        Map.entry("radiance.AuxiliaryTexturesMixin", RadiancePatchesConfig.EXTRA_PBR_FOLDERS),
        Map.entry("glint.HumanoidArmorLayerMixin", RadiancePatchesConfig.ARMOR_GLINT),
        Map.entry("radiance.EntityLayerOrderMixin", RadiancePatchesConfig.OVERLAY_LAYERS),
        Map.entry("radiance.GlowLayersMixin", RadiancePatchesConfig.GLOW_LAYERS),
        Map.entry("emissive.ModelBlockRendererMixin", RadiancePatchesConfig.EMISSIVE_FACES),
        Map.entry("emissive.ItemRendererMixin", RadiancePatchesConfig.EMISSIVE_FACES),
        Map.entry("emissive.PBRVertexConsumerEmissiveMixin", RadiancePatchesConfig.EMISSIVE_FACES),
        Map.entry("emissive.GlowFinderMixin", RadiancePatchesConfig.GLOW_FINDER),
        Map.entry("fluid.BlockRenderDispatcherMixin", RadiancePatchesConfig.MODDED_FLUIDS),
        Map.entry("fluid.LavaShoreMixin", RadiancePatchesConfig.LAVA_SHORE),
        Map.entry("fluid.PBRVertexConsumerShoreMixin", RadiancePatchesConfig.LAVA_SHORE),
        Map.entry("hand.PlayerHandMarkerMixin", RadiancePatchesConfig.ARM_MARKER),
        Map.entry("radiance.BufferProxyDimensionMixin", RadiancePatchesConfig.DIMENSION_PROFILES_SWITCH)
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String shortName = mixinClassName.startsWith(PACKAGE) ? mixinClassName.substring(PACKAGE.length()) : mixinClassName;
        if (shortName.equals("radiance.EntityProxyPatchMixin")) {
            // Holds two fixes; each one checks its own switch at runtime.
            return RadiancePatchesConfig.isEnabled(RadiancePatchesConfig.BLOCK_ENTITY_CRASH)
                || RadiancePatchesConfig.isEnabled(RadiancePatchesConfig.MODDED_VILLAGER_LAYERS);
        }
        String fix = FIX_FOR_MIXIN.get(shortName);
        return fix == null || RadiancePatchesConfig.isEnabled(fix);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
