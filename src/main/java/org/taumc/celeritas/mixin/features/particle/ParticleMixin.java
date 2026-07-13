package org.taumc.celeritas.mixin.features.particle;

import net.minecraft.client.entity.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.extensions.ParticleExtension;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

@Mixin(Particle.class)
public abstract class ParticleMixin implements ParticleExtension {
    @Unique
    private boolean celeritas$visible = true;

    @Inject(method = "tick", at = @At("TAIL"))
    private void celeritas$updateVisibility(CallbackInfo ci) {
        CeleritasWorldRenderer renderer = CeleritasWorldRenderer.instanceNullable();
        this.celeritas$visible = renderer == null || renderer.isParticleVisible((Particle)(Object)this);
    }

    @Override
    public boolean celeritas$isVisible() {
        return this.celeritas$visible;
    }
}
