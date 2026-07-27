package dev.rdh.argentum.mixin.core.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.render.terrain.fog.GLStateManagerFogService;
import dev.rdh.argentum.impl.debug.RenderMetrics;

import net.minecraft.client.gui.GameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    private float fogRed;

    @Shadow
    private float fogGreen;

    @Shadow
    private float fogBlue;

	@Inject(method = "render(FJ)V", at = @At("HEAD"))
    private void celeritas$beginMetricsFrame(float tickDelta, long startTime, CallbackInfo ci) {
        RenderMetrics.beginFrame();
    }

    @WrapOperation(
            method = "render(FJ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;render(F)V")
    )
    private void celeritas$profileHud(GameGui gui, float tickDelta, Operation<Void> original) {
        RenderMetrics.Category previous = RenderMetrics.setCategory(RenderMetrics.Category.HUD);
        try {
            original.call(gui, tickDelta);
        } finally {
            RenderMetrics.setCategory(previous);
        }
    }

    @WrapOperation(
            method = "render(FJ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(IIF)V")
    )
    private void celeritas$profileScreen(Screen screen, int mouseX, int mouseY, float tickDelta, Operation<Void> original) {
        RenderMetrics.Category previous = RenderMetrics.setCategory(RenderMetrics.Category.HUD);
        try {
            original.call(screen, mouseX, mouseY, tickDelta);
        } finally {
            RenderMetrics.setCategory(previous);
        }
    }

    @Inject(method = "setupClearColor", at = @At("RETURN"))
    private void captureFogColor(float par1, CallbackInfo ci) {
        GLStateManagerFogService.fogColorRed = this.fogRed;
        GLStateManagerFogService.fogColorGreen = this.fogGreen;
        GLStateManagerFogService.fogColorBlue = this.fogBlue;
    }
}
