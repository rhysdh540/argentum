package dev.rdh.argentum.impl.render.instancing;

import org.embeddedt.embeddium.impl.gl.array.GlVertexArray;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.tessellation.GlVertexArrayTessellation;
import org.embeddedt.embeddium.impl.gl.tessellation.TessellationBinding;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class InstancedGeometryBuffer {
    private FloatBuffer vertices;
    private final GlVertexFormat vertexFormat;
    private final GlVertexFormat instanceFormat;
    private GlMutableBuffer vertexBuffer;
    private GlMutableBuffer instanceBuffer;
    private GlVertexArrayTessellation tessellation;

    public InstancedGeometryBuffer(FloatBuffer vertices, GlVertexFormat vertexFormat, GlVertexFormat instanceFormat) {
        this.vertices = vertices;
        this.vertexFormat = vertexFormat;
        this.instanceFormat = instanceFormat;
    }

    public void initialize(CommandList commandList) {
        if (this.tessellation != null) {
            return;
        }

        try {
            this.vertexBuffer = commandList.createMutableBuffer();
            this.instanceBuffer = commandList.createMutableBuffer();
            commandList.uploadData(this.vertexBuffer, MemoryUtil.memAddress(this.vertices), (long)this.vertices.remaining() * Float.BYTES, GlBufferUsage.STATIC_DRAW);
            this.tessellation = new GlVertexArrayTessellation(new GlVertexArray(), new TessellationBinding[]{
                    TessellationBinding.forVertexBuffer(this.vertexBuffer, this.vertexFormat),
                    TessellationBinding.forVertexBuffer(this.instanceBuffer, this.instanceFormat,
                            this.vertexFormat.getAttributes().size(), 1)
            }
            );
            this.tessellation.init(commandList);
            this.vertices = null;
        } catch (RuntimeException exception) {
            this.delete(commandList);
            throw exception;
        }
    }

    public void draw(CommandList commandList, IntBuffer instances, int vertexCount, int instanceCount) {
        this.initialize(commandList);
        this.uploadInstances(commandList, MemoryUtil.memAddress(instances), (long)instances.remaining() * Integer.BYTES);
        this.tessellation.bind(commandList);
        try {
            ARBDrawInstanced.glDrawArraysInstancedARB(GL11.GL_QUADS, 0, vertexCount, instanceCount);
        } finally {
            this.tessellation.unbind(commandList);
        }
    }

    private void uploadInstances(CommandList commandList, long ptr, long bytes) {
        if (bytes > this.instanceBuffer.getSize()) {
            commandList.allocateStorage(this.instanceBuffer, bytes + (bytes >> 1), GlBufferUsage.STREAM_DRAW);
        } else {
            commandList.bindBuffer(GlBufferTarget.ARRAY_BUFFER, this.instanceBuffer);
        }

        GL15C.nglBufferSubData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), 0L, bytes, ptr);
    }

    public void delete(CommandList commandList) {
        if (this.tessellation != null) {
            commandList.deleteTessellation(this.tessellation);
            this.tessellation = null;
        }
        if (this.vertexBuffer != null) {
            commandList.deleteBuffer(this.vertexBuffer);
            this.vertexBuffer = null;
        }
        if (this.instanceBuffer != null) {
            commandList.deleteBuffer(this.instanceBuffer);
            this.instanceBuffer = null;
        }
    }
}
