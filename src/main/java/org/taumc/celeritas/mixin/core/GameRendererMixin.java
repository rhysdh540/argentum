package org.taumc.celeritas.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.fog.GLStateManagerFogService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    private float fogRed;

    @Shadow
    private float fogGreen;

    @Shadow
    private float fogBlue;

    @Shadow
    private Minecraft minecraft;

    @Inject(method = "setupClearColor", at = @At("RETURN"))
    private void captureFogColor(float par1, CallbackInfo ci) {
        GLStateManagerFogService.fogColorRed = this.fogRed;
        GLStateManagerFogService.fogColorGreen = this.fogGreen;
        GLStateManagerFogService.fogColorBlue = this.fogBlue;
    }
}
