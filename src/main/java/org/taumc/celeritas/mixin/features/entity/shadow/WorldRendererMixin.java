package org.taumc.celeritas.mixin.features.entity.shadow;

import net.minecraft.client.render.Culler;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.EntityShadowBatch;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void celeritas$beginEntityShadows(Entity camera, Culler culler, float tickDelta, CallbackInfo ci) {
        EntityShadowBatch.beginFrame();
    }

    @Inject(
            method = "renderEntities",
            at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=blockentities")
    )
    private void celeritas$drawEntityShadows(Entity camera, Culler culler, float tickDelta, CallbackInfo ci) {
        EntityShadowBatch.flush();
    }
}
