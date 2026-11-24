package dev.paddysdreamland.radiancepatches.mixin.particle;

import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Particle maps fix: which texture a particle is showing right now. */
@Mixin(TextureSheetParticle.class)
public interface TextureSheetParticleAccessor {
    @Accessor("sprite")
    TextureAtlasSprite radiancePatches$getSprite();
}
