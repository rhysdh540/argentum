package dev.rdh.argentum.extras.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.layer.AbstractArmorLayer;
import net.minecraft.client.render.entity.layer.CapeLayer;
import net.minecraft.client.render.entity.layer.Deadmou5Layer;
import net.minecraft.client.render.entity.layer.EntityRenderLayer;
import net.minecraft.client.render.entity.layer.ItemInHandLayer;
import net.minecraft.client.render.entity.layer.StuckArrowLayer;
import net.minecraft.client.render.entity.layer.WitchItemInHandLayer;
import net.minecraft.client.render.entity.layer.WornSkullLayer;
import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @WrapWithCondition(method = "renderLayers",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/layer/EntityRenderLayer;render(Lnet/minecraft/entity/living/LivingEntity;FFFFFFF)V"))
    private boolean argentumExtras$renderLayer(EntityRenderLayer<?> layer, LivingEntity entity,
            float limbAngle, float limbDistance, float tickDelta, float animationProgress,
            float headYaw, float headPitch, float scale) {
        if (layer instanceof AbstractArmorLayer) return ArgentumExtras.CONFIG.armor;
        if (layer instanceof ItemInHandLayer || layer instanceof WitchItemInHandLayer) {
            return ArgentumExtras.CONFIG.heldItems;
        }
        if (layer instanceof WornSkullLayer) return ArgentumExtras.CONFIG.wornHeads;
        if (layer instanceof CapeLayer) return ArgentumExtras.CONFIG.capes;
        if (layer instanceof Deadmou5Layer) return ArgentumExtras.CONFIG.playerEars;
        if (layer instanceof StuckArrowLayer) return ArgentumExtras.CONFIG.stuckArrows;
        return true;
    }

    @Inject(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At("HEAD"), cancellable = true)
    private void argentumExtras$hideNameTags(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        if (!ArgentumExtras.CONFIG.nameTags) {
            ci.cancel();
        }
    }
}
