package org.taumc.celeritas.impl.render.terrain;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.render.block.BlockLayer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrimitiveRenderPassConfigurationBuilder {
    public static final TerrainRenderPass SOLID_PASS, CUTOUT_MIPPED_PASS, TRANSLUCENT_PASS;
    public static final Material SOLID_MATERIAL, CUTOUT_MATERIAL, CUTOUT_MIPPED_MATERIAL, TRANSLUCENT_MATERIAL;

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

    private static TerrainRenderPass.TerrainRenderPassBuilder builderForRenderType(int pass, boolean disableBlend) {
        var builder = TerrainRenderPass.builder();
        builder.pipelineState(new PrimitivePipelineState(pass, disableBlend));
        builder.vertexType(ChunkMeshFormats.VANILLA_LIKE).primitiveType(QuadPrimitiveType.TRIANGULATED);
        return builder;
    }

    static {
        SOLID_PASS =  builderForRenderType(0, true)
                .name("solid")
                .fragmentDiscard(false)
                .useReverseOrder(false)
                .build();
        CUTOUT_MIPPED_PASS = builderForRenderType(0, false)
                .name("cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();
        TRANSLUCENT_PASS = builderForRenderType(1, false)
                .name("translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(true) // TODO allow disabling
                .build();
        TRANSLUCENT_MATERIAL = new Material(TRANSLUCENT_PASS, AlphaCutoffParameter.ZERO, true);
        SOLID_MATERIAL = new Material(SOLID_PASS, AlphaCutoffParameter.ZERO, true);
        CUTOUT_MIPPED_MATERIAL = new Material(CUTOUT_MIPPED_PASS, AlphaCutoffParameter.ONE_TENTH, true);
        CUTOUT_MATERIAL = new Material(CUTOUT_MIPPED_PASS, AlphaCutoffParameter.ONE_TENTH, false);
    }

    public static RenderPassConfiguration<?> build(ChunkVertexType vertexType) {
        Map<BlockLayer, Collection<TerrainRenderPass>> vanillaRenderStages = new Reference2ReferenceOpenHashMap<>();
        vanillaRenderStages.put(BlockLayer.SOLID, List.of(SOLID_PASS, CUTOUT_MIPPED_PASS));
        vanillaRenderStages.put(BlockLayer.TRANSLUCENT, List.of(TRANSLUCENT_PASS));

        Map<BlockLayer, Material> renderTypeToMaterialMap = new Reference2ReferenceOpenHashMap<>();
        renderTypeToMaterialMap.put(BlockLayer.SOLID, SOLID_MATERIAL);
        renderTypeToMaterialMap.put(BlockLayer.CUTOUT, CUTOUT_MATERIAL);
        renderTypeToMaterialMap.put(BlockLayer.CUTOUT_MIPPED, CUTOUT_MIPPED_MATERIAL);
        renderTypeToMaterialMap.put(BlockLayer.TRANSLUCENT, TRANSLUCENT_MATERIAL);

        return new RenderPassConfiguration<>(renderTypeToMaterialMap,
                vanillaRenderStages,
                CUTOUT_MIPPED_MATERIAL,
                CUTOUT_MIPPED_MATERIAL,
                TRANSLUCENT_MATERIAL);
    }
}
