package org.taumc.celeritas.mixin.features.profiling;

import net.minecraft.client.render.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.debug.RenderMetrics;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void celeritas$countDraw(int mode, CallbackInfo ci) {
        RenderMetrics.recordDraw();
    }
}
