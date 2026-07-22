package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import dev.rdh.argentum.extras.ArgentumExtrasConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.overlay.DebugOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugOverlay.class)
public class DebugOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    private List<String> argentumExtras$gameInfo;
    private List<String> argentumExtras$systemInfo;
    private long argentumExtras$gameInfoTimeNanos;
    private long argentumExtras$systemInfoTimeNanos;

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
}
