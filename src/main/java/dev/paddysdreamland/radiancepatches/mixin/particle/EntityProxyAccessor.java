package dev.paddysdreamland.radiancepatches.mixin.particle;

import com.radiance.client.proxy.world.EntityProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Particle glow fix: Radiance's ids for things that aren't entities, so ray traced particles get one of their own. */
@Mixin(value = EntityProxy.class, remap = false)
public interface EntityProxyAccessor {
    @Invoker("staticEntityKey")
    static long radiancePatches$staticEntityKey(long value) {
        throw new AssertionError();
    }
}
