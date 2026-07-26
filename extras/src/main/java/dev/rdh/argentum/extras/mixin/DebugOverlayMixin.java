package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import dev.rdh.argentum.extras.ArgentumExtrasConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.overlay.DebugOverlay;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.platform.GlStateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugOverlay.class)
public class DebugOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    private List<String> argentumExtras$gameInfo;
    private List<String> argentumExtras$systemInfo;
    private long argentumExtras$gameInfoTimeNanos;
    private long argentumExtras$systemInfoTimeNanos;

    @Inject(method = "render", at = @At("HEAD"))
    private void argentumExtras$scaleDebugHud(Window window, CallbackInfo ci) {
        float scale = (float)getScale(window) / window.getScale();
        GlStateManager.pushMatrix();
        GlStateManager.scalef(scale, scale, 1.0F);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void argentumExtras$restoreDebugHudScale(Window window, CallbackInfo ci) {
        GlStateManager.popMatrix();
    }

    @Redirect(method = "drawSystemInfo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Window;getWidth()I"))
    private int argentumExtras$getDebugHudWidth(Window window) {
        int scale = getScale(window);
        return (this.minecraft.width + scale - 1) / scale;
    }

    @Redirect(method = "drawTpsChart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Window;getHeight()I"))
    private int argentumExtras$getDebugHudHeight(Window window) {
        int scale = getScale(window);
        return (this.minecraft.height + scale - 1) / scale;
    }

    @Inject(method = "getGameInfo", at = @At("HEAD"), cancellable = true)
    private void argentumExtras$reuseGameInfo(CallbackInfoReturnable<List<String>> cir) {
        if (isFresh(this.argentumExtras$gameInfo, this.argentumExtras$gameInfoTimeNanos)) {
            this.argentumExtras$gameInfo.set(1, this.minecraft.fpsDebugInfo);
            cir.setReturnValue(this.argentumExtras$gameInfo);
        }
    }

    @Inject(method = "getGameInfo", at = @At("RETURN"))
    private void argentumExtras$saveGameInfo(CallbackInfoReturnable<List<String>> cir) {
        if (cir.getReturnValue() != this.argentumExtras$gameInfo) {
            this.argentumExtras$gameInfo = cir.getReturnValue();
            this.argentumExtras$gameInfoTimeNanos = System.nanoTime();
        }
    }

    @Inject(method = "getSystemInfo", at = @At("HEAD"), cancellable = true)
    private void argentumExtras$reuseSystemInfo(CallbackInfoReturnable<List<String>> cir) {
        if (isFresh(this.argentumExtras$systemInfo, this.argentumExtras$systemInfoTimeNanos)) {
            cir.setReturnValue(this.argentumExtras$systemInfo);
        }
    }

    @Inject(method = "getSystemInfo", at = @At("RETURN"))
    private void argentumExtras$saveSystemInfo(CallbackInfoReturnable<List<String>> cir) {
        if (cir.getReturnValue() != this.argentumExtras$systemInfo) {
            this.argentumExtras$systemInfo = cir.getReturnValue();
            this.argentumExtras$systemInfoTimeNanos = System.nanoTime();
        }
    }

    private static boolean isFresh(List<String> value, long timestamp) {
        ArgentumExtrasConfig config = ArgentumExtras.CONFIG;
        return config.steadyDebugHud && config.debugHudRefreshIntervalMs > 0 && value != null
                && System.nanoTime() - timestamp < config.debugHudRefreshIntervalMs * 1_000_000L;
    }

    private static int getScale(Window window) {
        int scale = ArgentumExtras.CONFIG.debugHudScale;
        return scale == 0 ? window.getScale() : scale;
    }
}
