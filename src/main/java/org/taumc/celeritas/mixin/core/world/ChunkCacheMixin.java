package org.taumc.celeritas.mixin.core.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.chunk.ServerChunkCache;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ServerChunkCache.class)
public class ChunkCacheMixin {
    @Shadow
    private ServerWorld world;

    @Inject(method = "loadChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/chunk/ServerChunkCache;loadChunkFromStorage(II)Lnet/minecraft/world/chunk/WorldChunk;"))
    private void markLoaded(int x, int z, CallbackInfoReturnable<WorldChunk> cir, @Share("loaded") LocalBooleanRef didLoad) {
        didLoad.set(true);
    }

    @Inject(method = "loadChunk", at = @At("RETURN"))
    private void sendLoadEventIfLoaded(int x, int z, CallbackInfoReturnable<WorldChunk> cir, @Share("loaded") LocalBooleanRef didLoad) {
        if (didLoad.get()) {
            ChunkTrackerHolder.get(this.world).onChunkStatusAdded(x, z, ChunkStatus.FLAG_ALL);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;unload()V", shift = At.Shift.AFTER))
    private void sendUnloadEvent(CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) WorldChunk chunk) {
        ChunkTrackerHolder.get(this.world).onChunkStatusRemoved(chunk.chunkX, chunk.chunkZ, ChunkStatus.FLAG_ALL);
    }
}
