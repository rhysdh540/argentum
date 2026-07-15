package org.taumc.celeritas.mixin.features.terrain;

import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.taumc.celeritas.impl.render.terrain.ChunkRenderListPool;

@Mixin(ChunkRenderList.class)
public abstract class ChunkRenderListMixin implements ChunkRenderListPool.Resettable {
    @Mutable
    @Shadow
    @Final
    private RenderRegion region;

    @Shadow
    private int sectionsWithGeometryCount;

    @Shadow
    private int sectionsWithSpritesCount;

    @Shadow
    private int sectionsWithEntitiesCount;

    @Shadow
    private int size;

    @Override
    public void celeritas$reset(RenderRegion region) {
        this.region = region;
        this.sectionsWithGeometryCount = 0;
        this.sectionsWithSpritesCount = 0;
        this.sectionsWithEntitiesCount = 0;
        this.size = 0;
    }
}
