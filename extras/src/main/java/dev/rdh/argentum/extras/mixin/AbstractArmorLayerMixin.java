package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.entity.layer.AbstractArmorLayer;
import net.minecraft.client.render.model.Model;
import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArmorLayer.class)
public class AbstractArmorLayerMixin {
    @Inject(method = "renderEnchantmentGlint", at = @At("HEAD"), cancellable = true)
    private void argentumExtras$hideArmorGlint(LivingEntity entity, Model model,
            float limbAngle, float limbDistance, float tickDelta, float animationProgress,
            float headYaw, float headPitch, float scale, CallbackInfo ci) {
        if (!ArgentumExtras.CONFIG.armorGlint) {
            ci.cancel();
        }
    }
}
