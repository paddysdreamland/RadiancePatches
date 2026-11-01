package dev.paddysdreamland.radiancepatches.mixin.glint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radiance.client.vertex.PBRVertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armor glint fix.
 * Radiance supports enchantment glint on armor by hooking ItemRenderer.getArmorFoilBuffer, but Minecraft 1.21.1
 * no longer uses that for armor: it draws the armor once and then draws the whole model a second time with the
 * glint texture (HumanoidArmorLayer.renderGlint). Radiance treats that second copy as normal geometry, so the glint
 * texture shows up as a solid, unmoving purple skin over the armor.
 * This marks the armor's own vertices as glinting instead (the same way Radiance handles items, so the shader pack
 * adds the moving shimmer), and skips the second copy.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    /** True while an enchanted armor piece is being drawn. Armor is only drawn on the render thread. */
    @Unique
    private static boolean radiancePatches$pieceHasGlint;

    /** True once the armor's own vertices got the glint, so the separate glint copy can be skipped. */
    @Unique
    private static boolean radiancePatches$glintApplied;

    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"))
    private void radiancePatches$startPiece(PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity,
                                            EquipmentSlot slot, int packedLight,
                                            net.minecraft.client.model.HumanoidModel<?> model,
                                            float limbSwing, float limbSwingAmount, float partialTick,
                                            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(slot);
        radiancePatches$pieceHasGlint = !stack.isEmpty() && stack.hasFoil();
        radiancePatches$glintApplied = false;
    }

    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("RETURN"))
    private void radiancePatches$endPiece(PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity,
                                          EquipmentSlot slot, int packedLight,
                                          net.minecraft.client.model.HumanoidModel<?> model,
                                          float limbSwing, float limbSwingAmount, float partialTick,
                                          float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        radiancePatches$pieceHasGlint = false;
        radiancePatches$glintApplied = false;
    }

    @Redirect(method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer radiancePatches$glintArmorBuffer(MultiBufferSource bufferSource, RenderType renderType) {
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        if (radiancePatches$pieceHasGlint && consumer instanceof PBRVertexConsumer pbrConsumer) {
            radiancePatches$glintApplied = true;
            return new PBRVertexConsumer.GLint(pbrConsumer, RenderType.armorEntityGlint());
        }
        return consumer;
    }

    @Inject(method = "renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V",
            at = @At("HEAD"), cancellable = true)
    private void radiancePatches$skipGlintCopy(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                               Model model, CallbackInfo ci) {
        if (radiancePatches$glintApplied) {
            ci.cancel();
        }
    }
}
