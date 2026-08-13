package dev.rdh.argentum.impl.render.entity.instancing;

import dev.rdh.argentum.impl.render.instancing.InstancedGeometryBuffer;

import net.minecraft.client.render.model.Box;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.render.model.Polygon;
import net.minecraft.client.render.model.Vertex;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.lwjgl.BufferUtils;
import dev.rdh.argentum.mixin.features.model.instancing.BoxAccessor;
import dev.rdh.argentum.mixin.features.model.instancing.PolygonAccessor;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ModelGeometry {
    private final List<PartGeometry> parts = new ArrayList<>();
    private int partIndex;

    public void begin() {
        this.partIndex = 0;
    }

    public InstanceGeometry getGeometry(ModelPart part, float scale) {
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

    void delete(CommandList commandList) {
        this.parts.forEach(geometry -> geometry.delete(commandList));
        this.parts.clear();
    }
}

final class PartGeometry extends InstanceGeometry {
    private final ModelPart part;
    private final InstancedGeometryBuffer buffers;
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
        this.buffers = new InstancedGeometryBuffer(vertices, InstancedVertexFormats.ENTITY_VERTEX, InstancedVertexFormats.ENTITY_INSTANCE);
    }

    ModelPart part() {
        return this.part;
    }

    public void render(CommandList commandList, Instances instances) {
        this.buffers.draw(commandList, instances.upload(), this.vertexCount, instances.count());
    }

    @Override
    public void delete(CommandList commandList) {
        this.buffers.delete(commandList);
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

    void add(Matrix4f matrix, float u, float v, int layer, Vector4fc color, float effectTime, Vector4fc overlayColor) {
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
        this.data[i++] = color.x();
        this.data[i++] = color.y();
        this.data[i++] = color.z();
        this.data[i++] = color.w();
        this.data[i++] = effectTime;
        this.data[i++] = overlayColor.x();
        this.data[i++] = overlayColor.y();
        this.data[i++] = overlayColor.z();
        this.data[i++] = overlayColor.w();
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
