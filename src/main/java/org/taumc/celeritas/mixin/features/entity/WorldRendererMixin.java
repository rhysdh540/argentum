package org.taumc.celeritas.mixin.features.entity;

import net.minecraft.client.render.Culler;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.EntityShadowBatch;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void celeritas$beginEntityBatches(Entity camera, Culler culler, float tickDelta, CallbackInfo ci) {
        EntityShadowBatch.beginFrame();
        EntityInstancingRenderer.beginFrame();
    }

    @Inject(
            method = "renderEntities",
            at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=blockentities")
    )
    private void celeritas$flushEntityBatches(Entity camera, Culler culler, float tickDelta, CallbackInfo ci) {
        RenderDevice.enterManagedCode();
        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            EntityShadowBatch.flush(commandList);
            EntityInstancingRenderer.flush(commandList);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }
}
