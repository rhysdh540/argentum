package dev.rdh.argentum.mixin.core.world;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.world.chunk.ClientChunkCache;
import net.minecraft.world.chunk.WorldChunk;

import java.util.List;

@Mixin(ClientChunkCache.class)
public interface ClientChunkCacheAccessor {
    @Accessor("chunks")
    List<WorldChunk> getAllChunks();
}