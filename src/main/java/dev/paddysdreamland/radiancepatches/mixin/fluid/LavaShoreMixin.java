package dev.paddysdreamland.radiancepatches.mixin.fluid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.paddysdreamland.radiancepatches.fluid.LavaShore;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lava shore edges: works out which neighbours of a lava block are walls while it is drawn. See LavaShore. */
@Mixin(BlockRenderDispatcher.class)
public abstract class LavaShoreMixin {
    @WrapOperation(method = "renderLiquid",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V"))
    private void radiancePatches$lavaShore(LiquidBlockRenderer renderer, BlockAndTintGetter level, BlockPos pos,
                                           VertexConsumer consumer, BlockState blockState, FluidState fluidState,
                                           Operation<Void> original) {
        float value = 0.0f;
        try {
            if (fluidState.is(FluidTags.LAVA)) {
                value = LavaShore.valueFor(level, pos);
            }
        } catch (RuntimeException error) {
            value = 0.0f;
        }
        LavaShore.begin(value);
        try {
            original.call(renderer, level, pos, consumer, blockState, fluidState);
        } finally {
            LavaShore.end();
        }
    }
}
