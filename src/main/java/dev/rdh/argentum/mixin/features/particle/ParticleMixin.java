package dev.rdh.argentum.mixin.features.particle;

import net.minecraft.client.entity.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.extensions.ParticleExtension;
import dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer;

@Mixin(Particle.class)
public abstract class ParticleMixin implements ParticleExtension {
    @Unique
    private boolean celeritas$visible = true;

    @Inject(method = "tick", at = @At("TAIL"))
    private void celeritas$updateVisibility(CallbackInfo ci) {
        ArgentumWorldRenderer renderer = ArgentumWorldRenderer.instanceNullable();
        this.celeritas$visible = renderer == null || renderer.isParticleVisible((Particle)(Object)this);
    }

    @Override
    public boolean argentum$isVisible() {
        return this.celeritas$visible;
    }
}
