package dev.rdh.argentum.extras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.vertex.VertexBuffer;
import net.minecraft.client.render.world.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @ModifyExpressionValue(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/Dimension;getSunriseColor(FF)[F"))
    private float[] argentumExtras$hideSunrise(float[] color) {
        return ArgentumExtras.CONFIG.sky ? color : null;
    }

    @WrapWithCondition(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/VertexBuffer;draw(I)V",
                    ordinal = 0))
    private boolean argentumExtras$drawSky(VertexBuffer buffer, int mode) {
        return ArgentumExtras.CONFIG.sky;
    }

    @WrapWithCondition(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;callList(I)V",
                    ordinal = 0))
    private boolean argentumExtras$drawSky(int list) {
        return ArgentumExtras.CONFIG.sky;
    }

    @WrapWithCondition(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/VertexBuffer;draw(I)V",
                    ordinal = 2))
    private boolean argentumExtras$drawDarkSky(VertexBuffer buffer, int mode) {
        return ArgentumExtras.CONFIG.sky;
    }

    @WrapWithCondition(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;callList(I)V",
                    ordinal = 2))
    private boolean argentumExtras$drawDarkSky(int list) {
        return ArgentumExtras.CONFIG.sky;
    }

    @ModifyArg(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;color4f(FFFF)V",
                    ordinal = 0), index = 3)
    private float argentumExtras$celestialAlpha(float alpha) {
        return ArgentumExtras.CONFIG.sunAndMoon ? alpha : 0.0F;
    }

    @WrapWithCondition(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/VertexBuffer;draw(I)V",
                    ordinal = 1))
    private boolean argentumExtras$drawStars(VertexBuffer buffer, int mode) {
        return ArgentumExtras.CONFIG.stars;
    }

    @WrapWithCondition(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;callList(I)V",
                    ordinal = 1))
    private boolean argentumExtras$drawStars(int list) {
        return ArgentumExtras.CONFIG.stars;
    }
}
