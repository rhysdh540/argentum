package dev.rdh.argentum.mixin.features.particle;

import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.entity.Entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.extensions.ParticleExtension;
import dev.rdh.argentum.impl.debug.RenderMetrics;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Unique
    private RenderMetrics.Category celeritas$previousCategory;

    @Inject(method = {"render", "renderLit"}, at = @At("HEAD"))
    private void celeritas$beginParticlePass(Entity camera, float tickDelta, CallbackInfo ci) {
        this.celeritas$previousCategory = RenderMetrics.setCategory(RenderMetrics.Category.PARTICLE);
    }

    @Inject(method = {"render", "renderLit"}, at = @At("RETURN"))
    private void celeritas$endParticlePass(Entity camera, float tickDelta, CallbackInfo ci) {
        RenderMetrics.setCategory(this.celeritas$previousCategory);
    }

    @WrapWithCondition(
            method = {"render", "renderLit"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/particle/Particle;render(Lnet/minecraft/client/render/vertex/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"),
            require = 2
    )
    private boolean celeritas$renderVisibleParticle(Particle particle, BufferBuilder buffer, Entity camera,
            float tickDelta, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        boolean visible = ((ParticleExtension) particle).celeritas$isVisible();
        if (visible) {
            RenderMetrics.recordRenderedParticle();
        } else {
            RenderMetrics.recordCulledParticle();
        }
        return visible;
    }
}
