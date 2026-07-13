package org.taumc.celeritas.mixin.core;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.Celeritas;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    private boolean logGlErrors;

    @Inject(method = "init", at = @At("RETURN"))
    private void celeritas$configureGlErrorChecking(CallbackInfo ci) {
        this.logGlErrors = Celeritas.CONFIG.checkGlErrors;
    }
}
