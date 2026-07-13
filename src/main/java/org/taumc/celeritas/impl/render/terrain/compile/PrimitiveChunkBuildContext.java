package org.taumc.celeritas.impl.render.terrain.compile;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.lwjgl.opengl.GL11C;

import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.DefaultVertexFormat;

import java.nio.IntBuffer;

public class PrimitiveChunkBuildContext extends ChunkBuildContext {
    private static final BlockLayer[] LAYERS = BlockLayer.values();

    private final BufferBuilder[] layerBuffers = new BufferBuilder[LAYERS.length];
    private final boolean[] usedLayerBuffers = new boolean[LAYERS.length];
    private int originX;
    private int originY;
    private int originZ;

    public PrimitiveChunkBuildContext(RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
    }

    public void beginSection(int x, int y, int z) {
        this.originX = x;
        this.originY = y;
        this.originZ = z;
    }

    public BufferBuilder getBuffer(BlockLayer layer) {
        int index = layer.ordinal();
        BufferBuilder buffer = this.layerBuffers[index];
        if (buffer == null) {
            buffer = new BufferBuilder(getInitialBufferSize(layer) * 1024 / Integer.BYTES);
            this.layerBuffers[index] = buffer;
        }
        if (!this.usedLayerBuffers[index]) {
            buffer.begin(GL11C.GL_QUADS, DefaultVertexFormat.BLOCK);
            buffer.offset(-this.originX, -this.originY, -this.originZ);
            this.usedLayerBuffers[index] = true;
        }
        return buffer;
    }

    public void finishSection(ChunkBuildBuffers buffers) {
        for (int i = 0; i < this.layerBuffers.length; i++) {
            if (!this.usedLayerBuffers[i]) {
                continue;
            }

            BufferBuilder buffer = this.layerBuffers[i];
            buffer.end();
            this.usedLayerBuffers[i] = false;
            try {
                this.copyRawBuffer(buffer.getBuffer().asIntBuffer(), buffer.getVertexCount(), buffers,
                        buffers.getRenderPassConfiguration().getMaterialForRenderType(LAYERS[i]));
            } finally {
                buffer.clear();
                buffer.offset(0, 0, 0);
            }
        }
    }

    // what size should the buffer for this layer be? in kib
    private static int getInitialBufferSize(BlockLayer layer) {
		return switch(layer) {
		    case SOLID, CUTOUT_MIPPED -> 512;
		    case TRANSLUCENT -> 128;
		    default -> 32;
	    };
    }

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private static final int[] NORMAL_WINDING = new int[] {0, 1, 2, 3};
    private static final int[] BACKFACE_WINDING = new int[] {3, 2, 1, 0};

    public void copyRawBuffer(IntBuffer rawBuffer, int vertexCount, ChunkBuildBuffers buffers, Material material) {
        if (vertexCount == 0) {
            return;
        }

        // Require
        if ((vertexCount & 0x3) != 0) {
            throw new IllegalStateException();
        }

		outputQuads(rawBuffer, this.vertices, vertexCount / 4, buffers, material, NORMAL_WINDING);
    }

    private void outputQuads(IntBuffer rawBuffer, ChunkVertexEncoder.Vertex[] celeritasVertices, int numQuads, ChunkBuildBuffers buffers, Material material, int[] winding) {
        int ptr = 0;
        for (int quadIdx = 0; quadIdx < numQuads; quadIdx++) {
            float uSum = 0, vSum = 0;
            for (int vIdx = 0; vIdx < 4; vIdx++) {
                var vertex = celeritasVertices[winding[vIdx]];
                vertex.x = Float.intBitsToFloat(rawBuffer.get(ptr++));
                vertex.y = Float.intBitsToFloat(rawBuffer.get(ptr++));
                vertex.z = Float.intBitsToFloat(rawBuffer.get(ptr++));

                // In 1.8+, color comes before all UVs. In 1.7-, texture UV comes before color.
                vertex.color = rawBuffer.get(ptr++);

                float u = Float.intBitsToFloat(rawBuffer.get(ptr++));
                float v = Float.intBitsToFloat(rawBuffer.get(ptr++));
                vertex.u = u;
                uSum += u;
                vertex.v = v;
                vSum += v;
                vertex.light = rawBuffer.get(ptr++);
            }
            int trueNormal = QuadUtil.calculateNormal(celeritasVertices);
            for (int vIdx = 0; vIdx < 4; vIdx++) {
                celeritasVertices[winding[vIdx]].trueNormal = trueNormal;
            }
            ModelQuadFacing facing = QuadUtil.findNormalFace(trueNormal);
            // TODO implement render pass downgrading for 1.5+
			buffers.get(material).getVertexBuffer(facing).push(celeritasVertices, material);
        }
    }

    @Override
    public void cleanup() {
        for (int i = 0; i < this.layerBuffers.length; i++) {
            if (this.usedLayerBuffers[i]) {
                BufferBuilder buffer = this.layerBuffers[i];
                try {
                    buffer.end();
                } finally {
                    buffer.clear();
                    buffer.offset(0, 0, 0);
                    this.usedLayerBuffers[i] = false;
                }
            }
        }
        super.cleanup();
    }
}
