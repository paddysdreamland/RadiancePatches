package dev.paddysdreamland.radiancepatches.mixin.fluid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.paddysdreamland.radiancepatches.fluid.FluidLookConsumer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Modded fluids fix.
 * Radiance draws every fluid except lava with water's textures and water's biome colour, so modded fluids
 * (and modded lava-like fluids) all looked like water, and their own resource-pack maps were never used.
 * This gives each modded fluid its own textures and colour back. Because they no longer use water's textures,
 * they also stop counting as water in the shader pack (water is recognised by its textures' flag maps).
 * Vanilla water and lava are left exactly as Radiance draws them.
 */
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {
    @WrapOperation(method = "renderLiquid",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V"))
    private void radiancePatches$ownFluidLook(LiquidBlockRenderer renderer, BlockAndTintGetter level, BlockPos pos,
                                              VertexConsumer consumer, BlockState blockState, FluidState fluidState,
                                              Operation<Void> original) {
        VertexConsumer wrapped = consumer;
        try {
            wrapped = radiancePatches$wrap(level, pos, consumer, fluidState);
        } catch (RuntimeException error) {
            wrapped = consumer;
        }
        original.call(renderer, level, pos, wrapped, blockState, fluidState);
    }

    private static VertexConsumer radiancePatches$wrap(BlockAndTintGetter level, BlockPos pos, VertexConsumer consumer,
                                                       FluidState fluidState) {
        if (fluidState.getFluidType() == NeoForgeMod.WATER_TYPE.value()
            || fluidState.getFluidType() == NeoForgeMod.LAVA_TYPE.value()) {
            return consumer;
        }
        TextureAtlasSprite[] own = FluidSpriteCache.getFluidSprites(level, pos, fluidState);
        if (own == null || own.length < 2 || own[0] == null || own[1] == null) {
            return consumer;
        }
        boolean drawnAsLava = fluidState.is(FluidTags.LAVA);
        TextureAtlasSprite[] radianceUses = drawnAsLava
            ? new TextureAtlasSprite[] {
                FluidSpriteCache.getSprite(ResourceLocation.withDefaultNamespace("block/lava_still")),
                FluidSpriteCache.getSprite(ResourceLocation.withDefaultNamespace("block/lava_flow")),
                null}
            : new TextureAtlasSprite[] {
                FluidSpriteCache.getSprite(ResourceLocation.withDefaultNamespace("block/water_still")),
                FluidSpriteCache.getSprite(ResourceLocation.withDefaultNamespace("block/water_flow")),
                FluidSpriteCache.getSprite(ResourceLocation.withDefaultNamespace("block/water_overlay"))};
        TextureAtlasSprite[] target = {own[0], own[1], own.length > 2 && own[2] != null ? own[2] : own[1]};

        int tint = IClientFluidTypeExtensions.of(fluidState).getTintColor(fluidState, level, pos);
        int radianceTint = drawnAsLava ? 0xFFFFFF : BiomeColors.getAverageWaterColor(level, pos);
        float[] scale = {
            ratio(tint >> 16 & 0xFF, radianceTint >> 16 & 0xFF),
            ratio(tint >> 8 & 0xFF, radianceTint >> 8 & 0xFF),
            ratio(tint & 0xFF, radianceTint & 0xFF)};
        int alpha = tint >>> 24 == 0 ? 255 : tint >>> 24;
        return new FluidLookConsumer(consumer, radianceUses, target, scale, alpha);
    }

    /** How much to scale a colour channel Radiance tinted with "from" so it ends up tinted with "to" instead. */
    private static float ratio(int to, int from) {
        return to / 255.0f / Math.max(from / 255.0f, 1.0f / 255.0f);
    }
}
