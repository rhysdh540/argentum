package org.taumc.celeritas.mixin.features.model;

import net.minecraft.client.render.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.CpuModelBatch;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    @Inject(method = {"enableCull", "disableCull"}, at = @At("HEAD"))
    private static void celeritas$flushModelBeforeCullChange(CallbackInfo ci) {
        CpuModelBatch.flush();
    }

    @Inject(method = "cullFace", at = @At("HEAD"))
    private static void celeritas$flushModelBeforeCullChange(int mode, CallbackInfo ci) {
        CpuModelBatch.flush();
    }

    @Inject(method = "pushMatrix", at = @At("HEAD"))
    private static void celeritas$pushModelMatrix(CallbackInfo ci) {
        CpuModelBatch.pushMatrix();
    }

    @Inject(method = "popMatrix", at = @At("HEAD"))
    private static void celeritas$popModelMatrix(CallbackInfo ci) {
        CpuModelBatch.popMatrix();
    }

    @Inject(method = "translatef", at = @At("HEAD"))
    private static void celeritas$translateModel(float x, float y, float z, CallbackInfo ci) {
        CpuModelBatch.translate(x, y, z);
    }

    @Inject(method = "translated", at = @At("HEAD"))
    private static void celeritas$translateModel(double x, double y, double z, CallbackInfo ci) {
        CpuModelBatch.translate((float)x, (float)y, (float)z);
    }

    @Inject(method = "rotatef", at = @At("HEAD"))
    private static void celeritas$rotateModel(float angle, float x, float y, float z, CallbackInfo ci) {
        CpuModelBatch.rotate(angle, x, y, z);
    }

    @Inject(method = "scalef", at = @At("HEAD"))
    private static void celeritas$scaleModel(float x, float y, float z, CallbackInfo ci) {
        CpuModelBatch.scale(x, y, z);
    }

    @Inject(method = "scaled", at = @At("HEAD"))
    private static void celeritas$scaleModel(double x, double y, double z, CallbackInfo ci) {
        CpuModelBatch.scale((float)x, (float)y, (float)z);
    }
}
