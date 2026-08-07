package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @ModifyConstant(method = "postRender", constant = @Constant(doubleValue = 256.0D))
    private double argentumExtras$changeShadowDistance(double vanilla) {
        int distance = ArgentumExtras.CONFIG.entityShadowDistance;
        return distance * distance;
    }

    @SuppressWarnings("WrapWithConditionTargetsNonVoid")
    @WrapWithCondition(method = "renderNameTag(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;enableDepthTest()V", ordinal = 1),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;depthMask(Z)V", ordinal = 1),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;draw(Ljava/lang/String;III)I", ordinal = 2)
    })
    private boolean argentumExtras$disableSecondNameTagLayer() {
        return ArgentumExtras.CONFIG.secondNameTagLayer;
    }
}
