package dev.rdh.argentum.impl.render.entity.instancing;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import dev.rdh.argentum.impl.render.instancing.InstancedGeometryBuffer;
import dev.rdh.argentum.impl.render.instancing.InstanceDataBuffer;
import dev.rdh.argentum.impl.render.instancing.ModelPartGeometry;

import net.minecraft.client.render.model.ModelPart;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.joml.Matrix4f;
import org.joml.Vector4fc;

public final class ModelGeometry {
    private final ObjectArrayList<PartGeometry> parts = new ObjectArrayList<>();
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
        ModelPartGeometry geometry = ModelPartGeometry.create(part, scale);
        this.vertexCount = geometry.vertexCount();
        this.buffers = new InstancedGeometryBuffer(geometry.vertices(), InstancedVertexFormats.ENTITY_VERTEX, InstancedVertexFormats.ENTITY_INSTANCE);
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

}

final class Instances extends InstanceDataBuffer {
    private static final int FLOATS = 28;

    Instances() {
        super(FLOATS, 12, 64);
    }

    void add(Matrix4f matrix, float u, float v, int layer, Vector4fc color, float effectTime, Vector4fc overlayColor) {
        int i = this.appendOffset();
        float[] data = this.data();
        data[i++] = matrix.m00();
        data[i++] = matrix.m01();
        data[i++] = matrix.m02();
        data[i++] = matrix.m03();
        data[i++] = matrix.m10();
        data[i++] = matrix.m11();
        data[i++] = matrix.m12();
        data[i++] = matrix.m13();
        data[i++] = matrix.m20();
        data[i++] = matrix.m21();
        data[i++] = matrix.m22();
        data[i++] = matrix.m23();
        data[i++] = matrix.m30();
        data[i++] = matrix.m31();
        data[i++] = matrix.m32();
        data[i++] = matrix.m33();
        data[i++] = u;
        data[i++] = v;
        data[i++] = layer;
        data[i++] = color.x();
        data[i++] = color.y();
        data[i++] = color.z();
        data[i++] = color.w();
        data[i++] = effectTime;
        data[i++] = overlayColor.x();
        data[i++] = overlayColor.y();
        data[i++] = overlayColor.z();
        data[i] = overlayColor.w();
        this.finishInstance();
    }
}
