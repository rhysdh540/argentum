package dev.rdh.argentum.extras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow private Minecraft minecraft;
    @Shadow private float renderDistance;
    @Shadow private boolean thiccFog;

    @ModifyExpressionValue(method = "tickFov",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/living/player/ClientPlayerEntity;getFovModifier()F"))
    private float argentumExtras$scaleDynamicFov(float modifier) {
        return 1.0F + (modifier - 1.0F) * strength(ArgentumExtras.CONFIG.dynamicFovStrength);
    }

    @ModifyArg(method = "applyHurtCam",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;rotatef(FFFF)V"),
            index = 0)
    private float argentumExtras$scaleHurtCamera(float angle) {
        return angle * strength(ArgentumExtras.CONFIG.hurtCameraStrength);
    }

    @ModifyArgs(method = "applyViewBobbing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;translatef(FFF)V"))
    private void argentumExtras$scaleViewBobbingTranslation(Args args) {
        float strength = strength(ArgentumExtras.CONFIG.viewBobbingStrength);
        args.set(0, args.<Float>get(0) * strength);
        args.set(1, args.<Float>get(1) * strength);
        args.set(2, args.<Float>get(2) * strength);
    }

    @ModifyArg(method = "applyViewBobbing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;rotatef(FFFF)V"),
            index = 0)
    private float argentumExtras$scaleViewBobbingRotation(float angle) {
        return angle * strength(ArgentumExtras.CONFIG.viewBobbingStrength);
    }

    @ModifyArgs(method = "setupCamera",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;scalef(FFF)V"))
    private void argentumExtras$scalePortalDistortion(Args args) {
        float scale = args.get(0);
        args.set(0, 1.0F + (scale - 1.0F) * strength(ArgentumExtras.CONFIG.portalDistortionStrength));
    }

    @ModifyArg(method = "renderClouds",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", ordinal = 0),
            index = 3)
    private float argentumExtras$extendCloudProjection(float vanilla) {
        int distance = ArgentumExtras.CONFIG.cloudRenderDistance;
        return distance == 0 ? vanilla : Math.max(vanilla, distance * 2.0F);
    }

    @Inject(method = "renderClouds",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;setupFog(IF)V",
                    shift = At.Shift.AFTER))
    private void argentumExtras$removeCloudFog(WorldRenderer renderer, float tickDelta, int pass, CallbackInfo ci) {
        if (!ArgentumExtras.CONFIG.cloudFog) {
            GlStateManager.disableFog();
        }
    }

    @WrapOperation(method = "render(IFJ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;setupFog(IF)V"))
    private void argentumExtras$changeTerrainFog(GameRenderer instance, int mode, float tickDelta,
            Operation<Void> original) {
        original.call(instance, mode, tickDelta);
        int density = ArgentumExtras.CONFIG.terrainFogDensity;
        Entity camera = this.minecraft.getCamera();
        if (density < 100 && mode != -1
                && (!(camera instanceof LivingEntity living) || !living.hasStatusEffect(StatusEffect.BLINDNESS))) {
            GlStateManager.fogEnd(density == 0 ? Float.MAX_VALUE : this.renderDistance / strength(density));
        }
    }

    @ModifyArg(method = "setupFog",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;fogDensity(F)V"))
    private float argentumExtras$changeFluidFog(float density) {
        return this.thiccFog ? density : density * strength(ArgentumExtras.CONFIG.fluidFogDensity);
    }

    @ModifyExpressionValue(method = {"tickRain", "renderSnowAndRain"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getRain(F)F"))
    private float argentumExtras$scaleWeatherDensity(float density) {
        return density * strength(ArgentumExtras.CONFIG.weatherDensity);
    }

    @ModifyConstant(method = "renderSnowAndRain", constant = @Constant(intValue = 5))
    private int argentumExtras$changeFastWeatherDistance(int vanilla) {
        return weatherDistance(vanilla);
    }

    @ModifyConstant(method = "renderSnowAndRain", constant = @Constant(intValue = 10))
    private int argentumExtras$changeFancyWeatherDistance(int vanilla) {
        return weatherDistance(vanilla);
    }

    @Unique
    private static int weatherDistance(int vanilla) {
        int distance = ArgentumExtras.CONFIG.weatherRenderDistance;
        return distance == 0 ? vanilla : distance;
    }

    @Unique
    private static float strength(int percentage) {
        return percentage / 100.0F;
    }
}
