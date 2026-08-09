package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.entity.living.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobRenderer.class)
public class MobRendererMixin {
    @Inject(method = "renderRiders", at = @At("HEAD"), cancellable = true)
    private void argentumExtras$hideLeashes(MobEntity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        if (!ArgentumExtras.CONFIG.leashes) {
            ci.cancel();
        }
    }
}
