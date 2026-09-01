package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.rdh.cera.modules.CustomLoadingScreens.LoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.ProgressRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProgressRenderer.class)
public class ProgressRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * Drawn over vanilla's dirt quad rather than replacing it: the custom background always covers the
     * whole screen, and the progress bar is drawn after this point either way.
     */
    @Inject(
            method = "progressStagePercentage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/Tesselator;end()V",
                    ordinal = 0, shift = At.Shift.AFTER)
    )
    private void cera$drawCustomBackground(int percentage, CallbackInfo ci,
            @Local(ordinal = 1) int width, @Local(ordinal = 2) int height) {
        LoadingScreen screen = this.minecraft.cera$getCustomLoadingScreens().active();
        if (screen != null) screen.draw(width, height);
    }
}
