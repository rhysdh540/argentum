package org.taumc.celeritas.mixin.core;

import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.taumc.celeritas.impl.extensions.ParticleExtension;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Redirect(
            method = {"render", "renderLit"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/particle/Particle;render(Lnet/minecraft/client/render/vertex/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"),
            require = 2
    )
    private void celeritas$renderVisibleParticle(Particle particle, BufferBuilder buffer, Entity camera,
            float tickDelta, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (((ParticleExtension)particle).celeritas$isVisible()) {
            particle.render(buffer, camera, tickDelta, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        }
    }
}
