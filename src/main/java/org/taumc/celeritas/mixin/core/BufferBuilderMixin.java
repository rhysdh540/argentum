package org.taumc.celeritas.mixin.core;

import java.nio.IntBuffer;

import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.VertexFormat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
    @Shadow
    private IntBuffer intBuffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private VertexFormat format;

    @Inject(method = "nextVertex", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/BufferBuilder;grow(I)V"))
    private void celeritas$syncBufferPosition(CallbackInfo ci) {
        this.intBuffer.position(this.vertexCount * this.format.getIntSize());
    }
}
