package dev.rdh.argentum.mixin.features.profiling;

import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.debug.RenderMetrics;

@Mixin(BufferUploader.class)
public abstract class BufferUploaderMixin {
    @Inject(
            method = "end",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDrawArrays(III)V", remap = false)
    )
    private void celeritas$countDraw(BufferBuilder buffer, CallbackInfo ci) {
        RenderMetrics.recordDraw();
    }
}
