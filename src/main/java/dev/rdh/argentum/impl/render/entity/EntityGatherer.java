package dev.rdh.argentum.impl.render.entity;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import dev.rdh.argentum.mixin.core.world.ClientChunkCacheAccessor;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeInstanceMultiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.List;
import java.util.function.Consumer;

public class EntityGatherer {
    private final List<Entity> entityList;

    public EntityGatherer() {
        this.entityList = new ObjectArrayList<>();
    }

    public void clear() {
        this.entityList.clear();
    }

    public List<Entity> getLoadedEntityList(ClientWorld world, int centerChunkX, int centerChunkZ, int radius) {
        Consumer<Entity> addEntity = this.entityList::add;
        // Iterate directly over chunk entity lists where possible - mods may create multipart entities that are not
        // added to the main loadedEntityList.
        if (world.getChunkSource() instanceof ClientChunkCacheAccessor provider) {
            var chunksByPos = provider.getChunksByPos();
            int diameter = radius * 2 + 1;

            if (diameter * diameter < chunksByPos.size()) {
                for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
                    for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                        WorldChunk chunk = chunksByPos.get(ChunkPos.toLong(chunkX, chunkZ));
                        if (chunk != null) {
                            collect(chunk, addEntity);
                        }
                    }
                }
            } else {
                for (WorldChunk chunk : provider.getAllChunks()) {
                    collect(chunk, addEntity);
                }
            }
        } else {
            // Best we can do is the loaded entity list - this will miss some multipart entities
            world.entities.forEach(addEntity);
        }
        return this.entityList;
    }

    private static void collect(WorldChunk chunk, Consumer<Entity> addEntity) {
        if (!chunk.argentum$hasEntities()) {
            return;
        }

        for (TypeInstanceMultiMap<Entity> map : chunk.getEntities()) {
            map.forEach(addEntity);
        }
    }
}
