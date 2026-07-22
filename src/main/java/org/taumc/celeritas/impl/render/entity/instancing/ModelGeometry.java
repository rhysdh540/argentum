package org.taumc.celeritas.impl.render.entity.instancing;

import net.minecraft.client.render.model.Box;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.render.model.Polygon;
import net.minecraft.client.render.model.Vertex;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.taumc.celeritas.mixin.features.model.instancing.BoxAccessor;
import org.taumc.celeritas.mixin.features.model.instancing.PolygonAccessor;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ModelBatch {
    private final List<PartGeometry> parts = new ArrayList<>();
    private int partIndex;

    void begin() {
        this.partIndex = 0;
    }

    PartGeometry getGeometry(ModelPart part, float scale) {
        if (this.partIndex < this.parts.size()) {
            PartGeometry geometry = this.parts.get(this.partIndex++);
            if (geometry.part() == part) {
                return geometry;
            }
        } else {
            this.partIndex++;
        }
        for (PartGeometry geometry : this.parts) {
            if (geometry.part() == part) {
                return geometry;
            }
        }
        PartGeometry geometry = new PartGeometry(part, scale);
        this.parts.add(geometry);
        return geometry;
    }
}

final class PartGeometry implements EntityGeometry {
    private static final int VERTEX_STRIDE = 12 * Float.BYTES;
    private static final int INSTANCE_FLOATS = 28;
    private static final int INSTANCE_STRIDE = INSTANCE_FLOATS * Float.BYTES;

    private final ModelPart part;
    private final int vertexBuffer;
    private final int instanceBuffer;
    private final int vertexCount;

    PartGeometry(ModelPart part, float scale) {
        this.part = part;
        this.vertexCount = part.boxes.size() * 24;
        FloatBuffer vertices = BufferUtils.createFloatBuffer(this.vertexCount * 12);
        for (Box box : part.boxes) {
            for (Polygon polygon : ((BoxAccessor)box).celeritas$getFaces()) {
                putPolygon(vertices, polygon, scale);
            }
        }
        vertices.flip();
        this.vertexBuffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBuffer);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW);
        this.instanceBuffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
    }

    ModelPart part() {
        return this.part;
    }

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
        ARBDrawInstanced.glDrawArraysInstancedARB(GL11.GL_QUADS, 0, this.vertexCount, instances.count());
    }

    private static void attribute(int index, int size, int stride, long offset) {
        GL20C.glEnableVertexAttribArray(index);
        GL20C.glVertexAttribPointer(index, size, GL11.GL_FLOAT, false, stride, offset);
    }

    private static void putPolygon(FloatBuffer output, Polygon polygon, float scale) {
        Vertex[] vertices = polygon.vertices;
        float ax = (float)(vertices[0].pos.x - vertices[1].pos.x);
        float ay = (float)(vertices[0].pos.y - vertices[1].pos.y);
        float az = (float)(vertices[0].pos.z - vertices[1].pos.z);
        float bx = (float)(vertices[2].pos.x - vertices[1].pos.x);
        float by = (float)(vertices[2].pos.y - vertices[1].pos.y);
        float bz = (float)(vertices[2].pos.z - vertices[1].pos.z);
        float nx = by * az - bz * ay;
        float ny = bz * ax - bx * az;
        float nz = bx * ay - by * ax;
        float inverseLength = 1.0F / (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (((PolygonAccessor)polygon).celeritas$isNormalFlipped()) {
            inverseLength = -inverseLength;
        }
        nx *= inverseLength;
        ny *= inverseLength;
        nz *= inverseLength;
        for (Vertex vertex : vertices) {
            output.put((float)vertex.pos.x * scale).put((float)vertex.pos.y * scale)
                    .put((float)vertex.pos.z * scale).put(vertex.u).put(vertex.v)
                    .put(nx).put(ny).put(nz).put(1.0F).put(1.0F).put(1.0F).put(1.0F);
        }
    }
}

interface EntityGeometry {
    void render(Instances instances);
}

final class Instances {
    private static final int FLOATS = 28;

    private float[] data = new float[64 * FLOATS];
    private FloatBuffer upload = BufferUtils.createFloatBuffer(this.data.length);
    private int size;
    private int count;

    void clear() {
        this.size = 0;
        this.count = 0;
    }

    void add(Matrix4f matrix, float u, float v, int layer, float red, float green, float blue, float alpha,
            float effectTime, float overlayRed, float overlayGreen, float overlayBlue, float overlayAlpha) {
        if (this.size + FLOATS > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
        }
        int i = this.size;
        this.data[i++] = matrix.m00();
        this.data[i++] = matrix.m01();
        this.data[i++] = matrix.m02();
        this.data[i++] = matrix.m03();
        this.data[i++] = matrix.m10();
        this.data[i++] = matrix.m11();
        this.data[i++] = matrix.m12();
        this.data[i++] = matrix.m13();
        this.data[i++] = matrix.m20();
        this.data[i++] = matrix.m21();
        this.data[i++] = matrix.m22();
        this.data[i++] = matrix.m23();
        this.data[i++] = matrix.m30();
        this.data[i++] = matrix.m31();
        this.data[i++] = matrix.m32();
        this.data[i++] = matrix.m33();
        this.data[i++] = u;
        this.data[i++] = v;
        this.data[i++] = layer;
        this.data[i++] = red;
        this.data[i++] = green;
        this.data[i++] = blue;
        this.data[i++] = alpha;
        this.data[i++] = effectTime;
        this.data[i++] = overlayRed;
        this.data[i++] = overlayGreen;
        this.data[i++] = overlayBlue;
        this.data[i++] = overlayAlpha;
        this.size = i;
        this.count++;
    }

    int count() {
        return this.count;
    }

    void sortBackToFront() {
        this.sort(0, this.count - 1);
    }

    private void sort(int low, int high) {
        int left = low;
        int right = high;
        float pivot = this.distance((low + high) >>> 1);
        while (left <= right) {
            while (this.distance(left) > pivot) left++;
            while (this.distance(right) < pivot) right--;
            if (left <= right) {
                this.swap(left++, right--);
            }
        }
        if (low < right) this.sort(low, right);
        if (left < high) this.sort(left, high);
    }

    private float distance(int index) {
        int offset = index * FLOATS + 12;
        float x = this.data[offset];
        float y = this.data[offset + 1];
        float z = this.data[offset + 2];
        return x * x + y * y + z * z;
    }

    private void swap(int first, int second) {
        int a = first * FLOATS;
        int b = second * FLOATS;
        for (int i = 0; i < FLOATS; i++) {
            float value = this.data[a + i];
            this.data[a + i] = this.data[b + i];
            this.data[b + i] = value;
        }
    }

    FloatBuffer upload() {
        if (this.upload.capacity() < this.size) {
            this.upload = BufferUtils.createFloatBuffer(this.data.length);
        }
        return this.upload.clear().put(this.data, 0, this.size).flip();
    }
}
