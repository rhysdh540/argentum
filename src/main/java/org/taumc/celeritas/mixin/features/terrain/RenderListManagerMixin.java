package org.taumc.celeritas.mixin.features.terrain;

import org.embeddedt.embeddium.impl.render.chunk.lists.RenderListManager;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.ChunkRenderListPool;

@Mixin(RenderListManager.class)
public abstract class RenderListManagerMixin {
    @Shadow
    private SortedRenderLists renderLists;

    @Shadow
    public abstract void finishPreviousGraphUpdate();

    @Inject(
            method = "finishPreviousGraphUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/embeddedt/embeddium/impl/render/chunk/lists/VisibleChunkCollector;createRenderLists()Lorg/embeddedt/embeddium/impl/render/chunk/lists/SortedRenderLists;",
                    shift = At.Shift.BEFORE
            )
    )
    private void celeritas$releasePreviousRenderLists(CallbackInfo ci) {
        ChunkRenderListPool.release(this.renderLists);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void celeritas$releaseRenderLists(CallbackInfo ci) {
        this.finishPreviousGraphUpdate();
        ChunkRenderListPool.release(this.renderLists);
    }
}
