package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import dev.rdh.argentum.extras.ArgentumExtrasConfig;
import net.minecraft.client.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.particle.ParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
    private void argentumExtras$filterParticle(int type, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, int[] parameters, CallbackInfoReturnable<?> cir) {
        if (!isEnabled(type)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "addEmitter", at = @At("HEAD"), cancellable = true)
    private void argentumExtras$filterEmitter(Entity entity, ParticleType type, CallbackInfo ci) {
        if (!isEnabled(type.getId())) {
            ci.cancel();
        }
    }

    private static boolean isEnabled(int type) {
        ArgentumExtrasConfig config = ArgentumExtras.CONFIG;
        return switch (type) {
            case 0, 1, 2, 3 -> config.explosionParticles;
            case 13, 14, 15, 16, 17, 20, 21, 22, 23, 34 -> config.spellParticles;
            case 24, 25 -> config.portalParticles;
            case 11, 12, 19, 26, 27, 29 -> config.smokeAndFlameParticles;
            case 30 -> config.redstoneParticles;
            case 4, 5, 6, 7, 8, 18, 39 -> config.waterParticles;
            default -> config.miscellaneousParticles;
        };
    }
}
