package dev.paddysdreamland.radiancepatches.mixin.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Particle glow fix: reads where a particle is and how bright it wants to be. */
@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("level")
    ClientLevel radiancePatches$getLevel();

    @Accessor("x")
    double radiancePatches$getX();

    @Accessor("y")
    double radiancePatches$getY();

    @Accessor("z")
    double radiancePatches$getZ();

    @Invoker("getLightColor")
    int radiancePatches$getLightColor(float partialTick);
}
