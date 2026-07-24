package org.taumc.celeritas.mixin.features.model.instancing;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.layer.EntityRenderLayer;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.model.entity.PlayerModel;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow
    protected Model model;

    @Shadow
    protected List<?> layers;

    @Shadow
    protected boolean solidRender;

    @Shadow
    protected abstract int getOverlayColor(LivingEntity entity, float brightness, float tickDelta);

    @Unique
    private boolean celeritas$instancingEntity;

    @Inject(method = "render", at = @At("HEAD"))
    private void celeritas$beginEntity(LivingEntity entity, double x, double y, double z, float yaw,
            float tickDelta, CallbackInfo ci) {
        boolean player = entity instanceof PlayerEntity && this.model instanceof PlayerModel;
        boolean eligible = !this.solidRender
                && !entity.isInvisible()
                && !entity.shouldRenderOnFire();
        float overlayRed = 0.0F;
        float overlayGreen = 0.0F;
        float overlayBlue = 0.0F;
        float overlayAlpha = 0.0F;
        if (entity.damagedTimer > 0 || entity.deathTicks > 0) {
            overlayRed = 1.0F;
            overlayAlpha = 0.3F;
        } else {
            int overlay = this.getOverlayColor(entity, entity.getBrightness(tickDelta), tickDelta);
            if ((overlay >>> 24) != 0) {
                overlayRed = (overlay >>> 16 & 0xFF) / 255.0F;
                overlayGreen = (overlay >>> 8 & 0xFF) / 255.0F;
                overlayBlue = (overlay & 0xFF) / 255.0F;
                overlayAlpha = 1.0F - (overlay >>> 24) / 255.0F;
            }
        }
        Identifier texture = eligible ? ((EntityRendererAccessor)this).celeritas$getTextureLocation(entity) : null;
        this.celeritas$instancingEntity = eligible && EntityInstancingRenderer.beginEntity(
                this.model, texture, player, player || !this.layers.isEmpty(), entity.ticks + tickDelta,
                overlayRed, overlayGreen, overlayBlue, overlayAlpha);
    }

    @Inject(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V", at = @At("HEAD"), cancellable = true)
    private void celeritas$deferNameTag(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        if (this.celeritas$instancingEntity
                && EntityInstancingRenderer.deferNameTag((LivingEntityRenderer<?>)(Object)this, entity, x, y, z)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void celeritas$beginModel(LivingEntity entity, float walkAnimationProgress, float walkAnimationSpeed,
            float bob, float yaw, float pitch, float scale, CallbackInfo ci) {
        if (this.celeritas$instancingEntity) {
            EntityInstancingRenderer.beginModel();
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void celeritas$endModel(LivingEntity entity, float walkAnimationProgress, float walkAnimationSpeed,
            float bob, float yaw, float pitch, float scale, CallbackInfo ci) {
        if (this.celeritas$instancingEntity) {
            EntityInstancingRenderer.endModel();
        }
    }

    @Redirect(
            method = "renderLayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/layer/EntityRenderLayer;render(Lnet/minecraft/entity/living/LivingEntity;FFFFFFF)V")
    )
    private void celeritas$captureLayer(EntityRenderLayer<LivingEntity> layer, LivingEntity entity,
            float walkAnimationProgress, float walkAnimationSpeed, float tickDelta, float bob,
            float yaw, float pitch, float scale) {
        boolean capture = this.celeritas$instancingEntity && EntityInstancingRenderer.beginLayer(layer, entity);
        try {
            layer.render(entity, walkAnimationProgress, walkAnimationSpeed, tickDelta, bob, yaw, pitch, scale);
        } finally {
            if (capture) {
                EntityInstancingRenderer.endLayer();
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void celeritas$endEntity(LivingEntity entity, double x, double y, double z, float yaw,
            float tickDelta, CallbackInfo ci) {
        if (this.celeritas$instancingEntity) {
            EntityInstancingRenderer.endEntity();
            this.celeritas$instancingEntity = false;
        }
    }
}
