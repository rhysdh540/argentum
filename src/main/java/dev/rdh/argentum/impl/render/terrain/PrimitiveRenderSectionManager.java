package dev.rdh.argentum.impl.render.terrain;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.functions.MultidrawFunctions;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.DirectMultiDrawEmitter;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.MultiDrawEmitter;
import org.embeddedt.embeddium.impl.render.chunk.sprite.GenericSectionSpriteTicker;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.position.SectionPos;
import org.jetbrains.annotations.Nullable;
import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.debug.RenderMetrics;
import dev.rdh.argentum.impl.extensions.SpriteExtension;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveChunkBuildContext;
import dev.rdh.argentum.impl.render.terrain.compile.task.ChunkBuilderMeshingTask;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;
import dev.rdh.argentum.impl.world.cloned.ClonedChunkSectionCache;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;

public class PrimitiveRenderSectionManager extends RenderSectionManager {
    private final World world;
    private final ClonedChunkSectionCache sectionCache;
    private final ReferenceOpenHashSet<RenderSection> sectionsWithSkyLight = new ReferenceOpenHashSet<>();

    public PrimitiveRenderSectionManager(RenderPassConfiguration<?> configuration, World world, int renderDistance, CommandList commandList, int minSection, int maxSection, int requestedThreads) {
        super(configuration, () -> new PrimitiveChunkBuildContext(configuration), ChunkRenderer::new, renderDistance, commandList, minSection, maxSection, requestedThreads, false);
        this.world = world;
        this.sectionCache = new ClonedChunkSectionCache(world);
    }

    public static PrimitiveRenderSectionManager create(ChunkVertexType vertexType, World world, int renderDistance, CommandList commandList) {
        int maxSection = world.getHeight() / 16;
        return new PrimitiveRenderSectionManager(PrimitiveRenderPassConfigurationBuilder.build(vertexType,
                Argentum.CONFIG.translucencySorting, Argentum.CONFIG.chunkFadeInDuration), world, renderDistance, commandList,
                0, maxSection,
                Argentum.CONFIG.chunkBuilderThreads);
    }

    @Override
    protected AsyncOcclusionMode getAsyncOcclusionMode() {
        return Argentum.CONFIG.asyncOcclusion;
    }

    @Override
    protected @Nullable SectionTicker createSectionTicker() {
        return new GenericSectionSpriteTicker<>(s -> ((SpriteExtension) s).celeritas$markActive());
    }

    @Override
    protected boolean shouldRespectUpdateTaskQueueSizeLimit() {
        return true;
    }

    @Override
    protected boolean useFogOcclusion() {
        return Argentum.CONFIG.fogCulling;
    }

    @Override
    protected boolean shouldUseOcclusionCulling(Viewport positionedViewport, boolean spectator) {
        var camBlockPos = positionedViewport.getBlockCoord();

        var block = this.world.getBlockState(new BlockPos(camBlockPos.x(), camBlockPos.y(), camBlockPos.z())).getBlock();

		return !spectator || !block.isOpaque();
    }

    @Override
    protected boolean isSectionVisuallyEmpty(int x, int y, int z) {
        return ChunkRenderContext.isSectionEmpty(this.world, x, y, z);
    }

    @Override
    protected @Nullable ChunkBuilderTask<ChunkBuildOutput> createRebuildTask(RenderSection render, int frame) {
        ChunkRenderContext context = ChunkRenderContext.prepare(this.world,
                new SectionPos(render.getChunkX(), render.getChunkY(), render.getChunkZ()), this.sectionCache);
        if (context == null) {
            return null;
        }

        return new ChunkBuilderMeshingTask(render, context, frame, this.cameraPosition);
    }

    @Override
    protected void invalidateCachedSectionData(RenderSection section) {
        this.sectionCache.invalidate(section.getChunkX(), section.getChunkY(), section.getChunkZ());
    }

    @Override
    protected boolean allowImportantRebuilds() {
        return !Argentum.CONFIG.deferChunkUpdates;
    }

    @Override
    protected boolean updateSectionInfo(RenderSection render, @Nullable BuiltRenderSectionData info) {
        boolean changed = super.updateSectionInfo(render, info);

        if (changed) {
            if (!(info instanceof PrimitiveBuiltRenderSectionData data)) {
                this.sectionsWithSkyLight.remove(render);
            } else if (data.hasSkyLight) {
                this.sectionsWithSkyLight.add(render);
            }
        }

        return changed;
    }

    public Collection<RenderSection> getSectionsWithSkyLight() {
        return this.sectionsWithSkyLight;
    }

    private static class ChunkRenderer extends DefaultChunkRenderer {

        public ChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
            super(device, renderPassConfiguration, new CountingMultiDrawEmitter(device));
        }

        @Override
        protected boolean useBlockFaceCulling() {
            return Argentum.CONFIG.blockFaceCulling;
        }

        @Override
        protected void configureShaderInterface(ChunkShaderInterface shader) {
            shader.setTextureSlot(ChunkShaderTextureSlot.BLOCK, 0);
            shader.setTextureSlot(ChunkShaderTextureSlot.LIGHT, 1);
        }
    }

    private static class CountingMultiDrawEmitter implements MultiDrawEmitter {
        private final DirectMultiDrawEmitter delegate = new DirectMultiDrawEmitter();
        private final boolean nativeMultiDraw;

        private CountingMultiDrawEmitter(RenderDevice device) {
            this.nativeMultiDraw = device.getDeviceFunctions().multidrawFunctions() == MultidrawFunctions.CORE;
        }

        @Override
        public void addDrawCommands(long meshData, int facingMask, int indexPointerMask) {
            this.delegate.addDrawCommands(meshData, facingMask, indexPointerMask);
        }

        @Override
        public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType) {
            RenderMetrics.recordTerrainDraws(this.nativeMultiDraw ? 1 : this.delegate.batch().size());
            this.delegate.executeBatch(commandList, tessellation, primitiveType);
        }

        @Override
        public boolean isEmpty() {
            return this.delegate.isEmpty();
        }

        @Override
        public int getIndexBufferSize() {
            return this.delegate.getIndexBufferSize();
        }

        @Override
        public void clear() {
            this.delegate.clear();
        }

        @Override
        public void delete() {
            this.delegate.delete();
        }
    }
}
