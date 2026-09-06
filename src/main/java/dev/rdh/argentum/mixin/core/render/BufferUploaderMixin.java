package dev.rdh.argentum.mixin.core.render;

import java.nio.ByteBuffer;

import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.rdh.argentum.impl.Argentum;

@Mixin(BufferUploader.class)
public class BufferUploaderMixin {
    @Unique
    private static int argentum$buffer;

    @Unique
    private static boolean argentum$streaming;

    @Inject(method = "end", at = @At("HEAD"))
    private void argentum$streamToBuffer(BufferBuilder builder, CallbackInfo ci) {
        if (builder.getVertexCount() <= 0) {
            return;
        }

        long bytes = (long) builder.getVertexCount() * builder.getFormat().getVertexSize();

        if (argentum$buffer == 0) {
            argentum$buffer = GL15C.glGenBuffers();
        }
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, argentum$buffer);
        GL15C.nglBufferData(GL15C.GL_ARRAY_BUFFER, bytes, MemoryUtil.memAddress(builder.getBuffer(), 0), GL15C.GL_STREAM_DRAW);
        argentum$streaming = true;
    }

    @Inject(method = "end", at = @At("RETURN"))
    private void argentum$unbindBuffer(BufferBuilder builder, CallbackInfo ci) {
        if (argentum$streaming) {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
            argentum$streaming = false;
        }
    }

    @WrapOperation(method = "end", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glVertexPointer(IIILjava/nio/ByteBuffer;)V"))
    private void argentum$vertexPointer(int size, int type, int stride, ByteBuffer data, Operation<Void> original) {
        if (argentum$streaming) {
            GL11.glVertexPointer(size, type, stride, data.position());
        } else {
            original.call(size, type, stride, data);
        }
    }

    @WrapOperation(method = "end", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexCoordPointer(IIILjava/nio/ByteBuffer;)V"))
    private void argentum$texCoordPointer(int size, int type, int stride, ByteBuffer data, Operation<Void> original) {
        if (argentum$streaming) {
            GL11.glTexCoordPointer(size, type, stride, data.position());
        } else {
            original.call(size, type, stride, data);
        }
    }

    @WrapOperation(method = "end", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColorPointer(IIILjava/nio/ByteBuffer;)V"))
    private void argentum$colorPointer(int size, int type, int stride, ByteBuffer data, Operation<Void> original) {
        if (argentum$streaming) {
            GL11.glColorPointer(size, type, stride, data.position());
        } else {
            original.call(size, type, stride, data);
        }
    }

    @WrapOperation(method = "end", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glNormalPointer(IILjava/nio/ByteBuffer;)V"))
    private void argentum$normalPointer(int type, int stride, ByteBuffer data, Operation<Void> original) {
        if (argentum$streaming) {
            GL11.glNormalPointer(type, stride, data.position());
        } else {
            original.call(type, stride, data);
        }
    }
}
