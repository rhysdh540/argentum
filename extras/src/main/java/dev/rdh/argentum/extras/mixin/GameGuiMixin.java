package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GameGui.class)
public class GameGuiMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At("TAIL"))
    private void argentumExtras$renderFps(float tickDelta, CallbackInfo ci) {
        if (ArgentumExtras.CONFIG.fpsHud && !this.minecraft.options.debugEnabled) {
            this.minecraft.textRenderer.drawWithShadow(Minecraft.getCurrentFps() + " FPS", 2, 2, 0xFFFFFF);
        }
    }

    @ModifyArgs(method = "renderVignette",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;color4f(FFFF)V",
                    ordinal = 0))
    private void argentumExtras$changeBorderVignetteStrength(Args args) {
        this.argentumExtras$changeVignetteStrength(args);
    }

    @ModifyArgs(method = "renderVignette",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;color4f(FFFF)V",
                    ordinal = 1))
    private void argentumExtras$changeVignetteStrength(Args args) {
        float strength = ArgentumExtras.CONFIG.vignetteStrength / 100.0F;
        args.set(0, args.<Float>get(0) * strength);
        args.set(1, args.<Float>get(1) * strength);
        args.set(2, args.<Float>get(2) * strength);
    }
}
