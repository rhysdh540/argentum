package dev.rdh.argentum.impl.render.entity.instancing;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;

public final class InstancedVertexAttributeBinding extends GlVertexAttributeBinding {
    private final int divisor;

    public InstancedVertexAttributeBinding(int index, int count, int stride, int pointer, int divisor) {
        super(index, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, "", count, false, pointer, stride, false));
        this.divisor = divisor;
    }

    public int divisor() {
        return this.divisor;
    }
}
