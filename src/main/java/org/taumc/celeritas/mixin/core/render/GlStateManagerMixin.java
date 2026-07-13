package org.taumc.celeritas.mixin.core.render;

import net.minecraft.client.render.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.fog.GLStateManagerFogService;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    @Inject(method = "enableFog", at = @At("HEAD"))
    private static void captureFogEnabled(CallbackInfo ci) {
        GLStateManagerFogService.fogEnabled = true;
    }

    @Inject(method = "disableFog", at = @At("HEAD"))
    private static void captureFogDisabled(CallbackInfo ci) {
        GLStateManagerFogService.fogEnabled = false;
    }

    @Inject(method = "fogMode", at = @At("HEAD"))
    private static void captureFogMode(int mode, CallbackInfo ci) {
        GLStateManagerFogService.fogMode = mode;
    }

    @Inject(method = "fogDensity", at = @At("HEAD"))
    private static void captureFogDensity(float density, CallbackInfo ci) {
        GLStateManagerFogService.fogDensity = density;
    }

    @Inject(method = "fogStart", at = @At("HEAD"))
    private static void captureFogStart(float start, CallbackInfo ci) {
        GLStateManagerFogService.fogStart = start;
    }

    @Inject(method = "fogEnd", at = @At("HEAD"))
    private static void captureFogEnd(float end, CallbackInfo ci) {
        GLStateManagerFogService.fogEnd = end;
    }
}
