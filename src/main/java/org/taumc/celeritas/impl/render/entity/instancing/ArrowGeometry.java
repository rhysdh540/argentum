package org.taumc.celeritas.impl.render.entity.instancing;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;

import java.nio.FloatBuffer;

final class ArrowGeometry implements EntityGeometry {
    private static final int VERTEX_STRIDE = 12 * Float.BYTES;
    private static final int INSTANCE_STRIDE = 28 * Float.BYTES;

    private final int vertexBuffer;
    private final int instanceBuffer;

    ArrowGeometry() {
        FloatBuffer vertices = BufferUtils.createFloatBuffer(24 * 12);
        quad(vertices, 1.0F, 0.0F, 0.0F, 0.0F, 0.15625F, 0.15625F, 0.3125F,
                -7, -2, -2, -7, -2, 2, -7, 2, 2, -7, 2, -2);
        quad(vertices, -1.0F, 0.0F, 0.0F, 0.0F, 0.15625F, 0.15625F, 0.3125F,
                -7, 2, -2, -7, 2, 2, -7, -2, 2, -7, -2, -2);
        for (int i = 1; i <= 4; i++) {
            float angle = i * (float)Math.PI * 0.5F;
            float sin = (float)Math.sin(angle);
            float cos = (float)Math.cos(angle);
            side(vertices, sin, cos);
        }
        vertices.flip();
        this.vertexBuffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBuffer);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW);
        this.instanceBuffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void render(Instances instances) {
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBuffer);
        attribute(0, 3, VERTEX_STRIDE, 0);
        attribute(1, 2, VERTEX_STRIDE, 3L * Float.BYTES);
        attribute(2, 3, VERTEX_STRIDE, 5L * Float.BYTES);
        attribute(9, 4, VERTEX_STRIDE, 8L * Float.BYTES);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.instanceBuffer);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, instances.upload(), GL15C.GL_STREAM_DRAW);
        attribute(3, 4, INSTANCE_STRIDE, 0);
        attribute(4, 4, INSTANCE_STRIDE, 4L * Float.BYTES);
        attribute(5, 4, INSTANCE_STRIDE, 8L * Float.BYTES);
        attribute(6, 4, INSTANCE_STRIDE, 12L * Float.BYTES);
        attribute(7, 3, INSTANCE_STRIDE, 16L * Float.BYTES);
        attribute(8, 4, INSTANCE_STRIDE, 19L * Float.BYTES);
        attribute(10, 1, INSTANCE_STRIDE, 23L * Float.BYTES);
        attribute(11, 4, INSTANCE_STRIDE, 24L * Float.BYTES);
        for (int i = 3; i < 9; i++) {
            ARBInstancedArrays.glVertexAttribDivisorARB(i, 1);
        }
        ARBInstancedArrays.glVertexAttribDivisorARB(10, 1);
        ARBInstancedArrays.glVertexAttribDivisorARB(11, 1);
        ARBDrawInstanced.glDrawArraysInstancedARB(GL11.GL_QUADS, 0, 24, instances.count());
    }

    private static void side(FloatBuffer vertices, float sin, float cos) {
        vertex(vertices, -8, -2 * cos, -2 * sin, 0.0F, 0.0F, 0.0F, -sin, cos);
        vertex(vertices, 8, -2 * cos, -2 * sin, 0.5F, 0.0F, 0.0F, -sin, cos);
        vertex(vertices, 8, 2 * cos, 2 * sin, 0.5F, 0.15625F, 0.0F, -sin, cos);
        vertex(vertices, -8, 2 * cos, 2 * sin, 0.0F, 0.15625F, 0.0F, -sin, cos);
    }

    private static void quad(FloatBuffer vertices, float nx, float ny, float nz,
            float minU, float maxU, float minV, float maxV, float... positions) {
        for (int i = 0; i < 4; i++) {
            vertex(vertices, positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2],
                    i == 0 || i == 3 ? minU : maxU, i < 2 ? minV : maxV, nx, ny, nz);
        }
    }

    private static void vertex(FloatBuffer vertices, float x, float y, float z, float u, float v,
            float nx, float ny, float nz) {
        vertices.put(x).put(y).put(z).put(u).put(v).put(nx).put(ny).put(nz)
                .put(1.0F).put(1.0F).put(1.0F).put(1.0F);
    }

    private static void attribute(int index, int size, int stride, long offset) {
        GL20C.glEnableVertexAttribArray(index);
        GL20C.glVertexAttribPointer(index, size, GL11.GL_FLOAT, false, stride, offset);
    }
}
