package org.taumc.celeritas.impl.render.terrain;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.render.block.BlockLayer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrimitiveRenderPassConfigurationBuilder {
    private record PrimitivePipelineState(int pass, boolean disableBlend) implements TerrainRenderPass.PipelineState {
        @Override
        public void setup() {
            if (disableBlend) {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
            }
        }

        @Override
        public void clear() {
            if (disableBlend) {
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            }
        }
    }

    private static TerrainRenderPass.TerrainRenderPassBuilder builderForRenderType(int pass, boolean disableBlend, ChunkVertexType vertexType) {
        var builder = TerrainRenderPass.builder();
        builder.pipelineState(new PrimitivePipelineState(pass, disableBlend));
        builder.vertexType(vertexType).primitiveType(QuadPrimitiveType.TRIANGULATED);
        return builder;
    }

    public static RenderPassConfiguration<?> build(ChunkVertexType vertexType, boolean translucencySorting) {
        TerrainRenderPass solidPass = builderForRenderType(0, true, vertexType)
                .name("solid")
                .fragmentDiscard(false)
                .useReverseOrder(false)
                .build();
        TerrainRenderPass cutoutMippedPass = builderForRenderType(0, false, vertexType)
                .name("cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();
        TerrainRenderPass translucentPass = builderForRenderType(1, false, vertexType)
                .name("translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(translucencySorting)
                .build();
        Material translucentMaterial = new Material(translucentPass, AlphaCutoffParameter.ZERO, true);
        Material solidMaterial = new Material(solidPass, AlphaCutoffParameter.ZERO, true);
        Material cutoutMippedMaterial = new Material(cutoutMippedPass, AlphaCutoffParameter.ONE_TENTH, true);
        Material cutoutMaterial = new Material(cutoutMippedPass, AlphaCutoffParameter.ONE_TENTH, false);

        Map<BlockLayer, Collection<TerrainRenderPass>> vanillaRenderStages = new Reference2ReferenceOpenHashMap<>();
        vanillaRenderStages.put(BlockLayer.SOLID, List.of(solidPass, cutoutMippedPass));
        vanillaRenderStages.put(BlockLayer.TRANSLUCENT, List.of(translucentPass));

        Map<BlockLayer, Material> renderTypeToMaterialMap = new Reference2ReferenceOpenHashMap<>();
        renderTypeToMaterialMap.put(BlockLayer.SOLID, solidMaterial);
        renderTypeToMaterialMap.put(BlockLayer.CUTOUT, cutoutMaterial);
        renderTypeToMaterialMap.put(BlockLayer.CUTOUT_MIPPED, cutoutMippedMaterial);
        renderTypeToMaterialMap.put(BlockLayer.TRANSLUCENT, translucentMaterial);

        return new RenderPassConfiguration<>(renderTypeToMaterialMap,
                vanillaRenderStages,
                cutoutMippedMaterial,
                cutoutMippedMaterial,
                translucentMaterial);
    }
}
