package dev.rdh.argentum.mixin.features.particle;

import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.entity.Entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @WrapWithCondition(
            method = {"render", "renderLit"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/particle/Particle;render(Lnet/minecraft/client/render/vertex/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"),
            require = 2
    )
    private boolean celeritas$renderVisibleParticle(Particle particle, BufferBuilder buffer, Entity camera,
            float tickDelta, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        return particle.argentum$isVisible();
    }
}
