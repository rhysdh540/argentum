package org.taumc.celeritas.mixin.features.terrain;

import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.VisibleChunkCollector;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.taumc.celeritas.impl.render.terrain.ChunkRenderListPool;

@Mixin(VisibleChunkCollector.class)
public abstract class VisibleChunkCollectorMixin {
    @Redirect(
            method = "createRenderList",
            at = @At(
                    value = "NEW",
                    target = "(Lorg/embeddedt/embeddium/impl/render/chunk/region/RenderRegion;)Lorg/embeddedt/embeddium/impl/render/chunk/lists/ChunkRenderList;"
            )
    )
    private ChunkRenderList celeritas$reuseRenderList(RenderRegion region) {
        return ChunkRenderListPool.acquire(region);
    }
}
