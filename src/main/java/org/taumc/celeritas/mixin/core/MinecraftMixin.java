package org.taumc.celeritas.mixin.core;

import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.Celeritas;
import org.taumc.celeritas.impl.render.entity.EntityShadowBatch;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    private boolean logGlErrors;

    @Inject(method = "init", at = @At("RETURN"))
    private void celeritas$configureGlErrorChecking(CallbackInfo ci) {
        this.logGlErrors = Celeritas.CONFIG.checkGlErrors;
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void celeritas$deleteInstancedGeometry(CallbackInfo ci) {
        if (!EntityInstancingRenderer.isInitialized()) {
            return;
        }
        RenderDevice.enterManagedCode();
        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            EntityInstancingRenderer.deleteGeometry(commandList);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void celeritas$deleteInstancedRenderResources(CallbackInfo ci) {
        if (!EntityInstancingRenderer.isInitialized() && !EntityShadowBatch.isInitialized()) {
            return;
        }
        RenderDevice.enterManagedCode();
        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            EntityInstancingRenderer.deleteGeometry(commandList);
            EntityShadowBatch.deleteGeometry(commandList);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }
}
