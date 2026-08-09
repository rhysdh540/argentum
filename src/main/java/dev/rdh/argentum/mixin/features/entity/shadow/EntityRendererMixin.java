package dev.rdh.argentum.mixin.features.entity.shadow;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.render.entity.EntityShadowBatch;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Shadow
    protected EntityRenderDispatcher dispatcher;

    @Shadow
    protected float shadowSize;

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private void celeritas$batchShadow(Entity entity, double dx, double dy, double dz, float opacity, float tickDelta, CallbackInfo ci) {
        if (EntityShadowBatch.record(this.dispatcher.world, entity, dx, dy, dz, opacity, tickDelta, this.shadowSize)) {
            ci.cancel();
        }
    }
}
