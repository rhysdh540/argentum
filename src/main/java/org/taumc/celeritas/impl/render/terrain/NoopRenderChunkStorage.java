package org.taumc.celeritas.impl.render.terrain;

import net.minecraft.client.render.world.RenderChunk;
import net.minecraft.client.render.world.RenderChunkFactory;
import net.minecraft.client.render.world.RenderChunkStorage;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class NoopRenderChunkStorage extends RenderChunkStorage {
    private static final RenderChunk[] EMPTY_CHUNKS = new RenderChunk[0];

    public NoopRenderChunkStorage(World world, int viewDistance, WorldRenderer renderer, RenderChunkFactory factory) {
        super(world, viewDistance, renderer, factory);
        this.chunks = EMPTY_CHUNKS;
    }

    @Override
    protected void setFactory(RenderChunkFactory factory) {
    }

    @Override
    protected void setViewDistance(int viewDistance) {
    }

    @Override
    public void releaseBuffers() {
    }

    @Override
    public void updateCameraPos(double x, double z) {
    }

    @Override
    public void markDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    @Override
    protected RenderChunk getChunk(BlockPos pos) {
        return null;
    }
}
