package dev.rdh.argentum.impl.render.terrain.matrix;

import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.joml.Matrix4f;

import java.util.Objects;

public class PrimitiveChunkMatrixGetter {
    private static ChunkRenderMatrices matrices;

    public static void update(float[] projection, float[] modelView) {
        matrices = new ChunkRenderMatrices(new Matrix4f().set(projection), new Matrix4f().set(modelView));
    }

    public static ChunkRenderMatrices getMatrices() {
        return Objects.requireNonNull(matrices, "Render matrices have not been captured");
    }
}
