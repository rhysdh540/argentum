package org.taumc.celeritas.mixin.features.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.model.Model;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.taumc.celeritas.impl.render.entity.CpuModelBatch;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @WrapOperation(
            method = "renderModel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/Model;render(Lnet/minecraft/entity/Entity;FFFFFF)V")
    )
    private void celeritas$batchModel(Model model, Entity entity, float walkAnimationProgress,
            float walkAnimationSpeed, float bob, float yaw, float pitch, float scale, Operation<Void> original) {
        if (!CpuModelBatch.begin()) {
            original.call(model, entity, walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
            return;
        }

        try {
            original.call(model, entity, walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        } finally {
            CpuModelBatch.end();
        }
    }
}
