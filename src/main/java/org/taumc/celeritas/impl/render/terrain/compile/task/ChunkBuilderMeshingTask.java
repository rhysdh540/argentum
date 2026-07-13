package org.taumc.celeritas.impl.render.terrain.compile.task;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.joml.Vector3d;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.occlusion.ChunkOcclusionDataBuilder;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportCategory;

public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final int buildTime;
    private final Vector3d camera;
    private final ChunkRenderContext renderContext;

    public ChunkBuilderMeshingTask(RenderSection render, ChunkRenderContext context, int time, Vector3d camera) {
        this.render = render;
        this.buildTime = time;
        this.camera = camera;
        this.renderContext = context;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext context, CancellationToken cancellationToken) {
        PrimitiveChunkBuildContext buildContext = (PrimitiveChunkBuildContext)context;
        var renderData = new PrimitiveBuiltRenderSectionData();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getSectionIndex());

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct

        var blockPos = new BlockPos.Mutable(minX, minY, minZ);
        var renderBlocks = net.minecraft.client.Minecraft.getInstance().getBlockRenderDispatcher();

        buildContext.beginSection(this.renderContext, minX, minY, minZ);

        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        blockPos.set(x, y, z);

                        var blockState = this.renderContext.getBlockState(blockPos);
                        var block = blockState.getBlock();

                        if (block == net.minecraft.block.Blocks.AIR) {
                            continue;
                        }

						if (block.hasBlockEntity()) {
                            BlockEntity blockEntity = this.renderContext.getBlockEntity(blockPos);
                            if (blockEntity != null) {
                                var renderer = BlockEntityRenderDispatcher.INSTANCE.getRenderer(blockEntity);
                                if (renderer != null) {
                                    (renderer.shouldRenderOffScreen() ? renderData.globalBlockEntities : renderData.culledBlockEntities).add(blockEntity);
                                }
                            }
                        }

                        var pass = block.getRenderLayer();

                        if (block.getRenderType() == 3) {
                            buildContext.getBlockRenderer().render(blockState, blockPos, this.renderContext, pass,
                                    buffers, renderData);
                        } else {
                            renderBlocks.render(blockState, blockPos, this.renderContext, buildContext.getBuffer(pass));
                        }

						if (block.isOpaque()) {
                            occluder.markClosed(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                        }
                    }
                }
            }
        } catch (Throwable exception) {
            throw this.addCrashContext(CrashReport.of(exception, "Encountered exception while building chunk meshes"), blockPos);
        }

        buildContext.finishSection(buffers);

        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buffers,(float)camera.x - minX, (float)camera.y - minY, (float)camera.z - minZ);

        if (!meshes.isEmpty()) {
            renderData.hasBlockGeometry = true;
        }

        renderData.visibilityData = occluder.computeVisibilityEncoding();

        return new ChunkBuildOutput(this.render, renderData, meshes, this.buildTime);
    }

    private CrashException addCrashContext(CrashReport report, BlockPos pos) {
        CrashReportCategory category = report.addCategory("Block being rendered");
        try {
            CrashReportCategory.addBlockDetails(category, pos, this.renderContext.getBlockState(pos));
        } catch (Throwable ignored) {
            category.add("Block location", CrashReportCategory.formatPosition(pos));
        }
        category.add("Chunk section", this.render);
        return new CrashException(report);
    }

}
