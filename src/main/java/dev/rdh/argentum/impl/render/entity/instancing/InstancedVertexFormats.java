package dev.rdh.argentum.impl.render.entity.instancing;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;

public final class InstancedVertexFormats {
    private static final int ENTITY_VERTEX_STRIDE = 12 * Float.BYTES;
    private static final int ENTITY_INSTANCE_STRIDE = 28 * Float.BYTES;
    private static final int SHADOW_INSTANCE_STRIDE = 10 * Float.BYTES;

    public static final GlVertexAttributeBinding[] ENTITY_VERTEX = {
            binding(0, 3, ENTITY_VERTEX_STRIDE, 0, 0),
            binding(1, 2, ENTITY_VERTEX_STRIDE, 3 * Float.BYTES, 0),
            binding(2, 3, ENTITY_VERTEX_STRIDE, 5 * Float.BYTES, 0),
            binding(9, 4, ENTITY_VERTEX_STRIDE, 8 * Float.BYTES, 0)
    };
    public static final GlVertexAttributeBinding[] ENTITY_INSTANCE = {
            binding(3, 4, ENTITY_INSTANCE_STRIDE, 0, 1),
            binding(4, 4, ENTITY_INSTANCE_STRIDE, 4 * Float.BYTES, 1),
            binding(5, 4, ENTITY_INSTANCE_STRIDE, 8 * Float.BYTES, 1),
            binding(6, 4, ENTITY_INSTANCE_STRIDE, 12 * Float.BYTES, 1),
            binding(7, 3, ENTITY_INSTANCE_STRIDE, 16 * Float.BYTES, 1),
            binding(8, 4, ENTITY_INSTANCE_STRIDE, 19 * Float.BYTES, 1),
            binding(10, 1, ENTITY_INSTANCE_STRIDE, 23 * Float.BYTES, 1),
            binding(11, 4, ENTITY_INSTANCE_STRIDE, 24 * Float.BYTES, 1)
    };
    public static final GlVertexAttributeBinding[] SHADOW_VERTEX = {
            binding(0, 2, 2 * Float.BYTES, 0, 0)
    };
    public static final GlVertexAttributeBinding[] SHADOW_INSTANCE = {
            binding(1, 4, SHADOW_INSTANCE_STRIDE, 0, 1),
            binding(2, 4, SHADOW_INSTANCE_STRIDE, 4 * Float.BYTES, 1),
            binding(3, 2, SHADOW_INSTANCE_STRIDE, 8 * Float.BYTES, 1)
    };

    private InstancedVertexFormats() {
    }

    private static GlVertexAttributeBinding binding(int index, int count, int stride, int pointer, int divisor) {
        return new GlVertexAttributeBinding(index,
                new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, "", count, false, pointer, stride, false), divisor);
    }
}
