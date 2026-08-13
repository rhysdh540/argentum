package dev.rdh.argentum.mixin.core;

import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.frame.RenderAheadManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.render.entity.EntityShadowBatch;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Unique
    private final RenderAheadManager celeritas$renderAheadManager = new RenderAheadManager();

    @Shadow
    private boolean logGlErrors;

    @Inject(method = "init", at = @At("RETURN"))
    private void celeritas$configureGlErrorChecking(CallbackInfo ci) {
        this.logGlErrors = Argentum.CONFIG.checkGlErrors;
    }

    @Inject(method = "runGame", at = @At("HEAD"))
    private void celeritas$startFrame(CallbackInfo ci) {
        this.celeritas$renderAheadManager.startFrame(Argentum.CONFIG.cpuRenderAheadLimit);
    }

    @Inject(method = "runGame", at = @At("RETURN"))
    private void celeritas$endFrame(CallbackInfo ci) {
        this.celeritas$renderAheadManager.endFrame();
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void celeritas$deleteInstancedRenderResources(CallbackInfo ci) {
        if (!EntityShadowBatch.isInitialized()) {
            return;
        }
        RenderDevice.enterManagedCode();
        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            EntityShadowBatch.deleteGeometry(commandList);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }
}
