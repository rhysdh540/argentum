package dev.rdh.argentum.impl.render.entity;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import dev.rdh.argentum.mixin.core.world.ClientChunkCacheAccessor;
import dev.rdh.argentum.mixin.core.world.WorldChunkAccessor;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeInstanceMultiMap;
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

    public List<Entity> getLoadedEntityList(ClientWorld world) {
        Consumer<Entity> addEntity = this.entityList::add;
        // Iterate directly over chunk entity lists where possible - mods may create multipart entities that are not
        // added to the main loadedEntityList.
        if (world.getChunkSource() instanceof ClientChunkCacheAccessor provider) {
            var loadedChunks = provider.getAllChunks();
            for (WorldChunk chunk : loadedChunks) {
                if (!((WorldChunkAccessor)chunk).getHasEntities()) {
                    continue;
                }
                TypeInstanceMultiMap<Entity>[] entityMaps = chunk.getEntities();
                for (TypeInstanceMultiMap<Entity> map : entityMaps) {
                    map.forEach(addEntity);
                }
            }
        } else {
            // Best we can do is the loaded entity list - this will miss some multipart entities
            world.entities.forEach(addEntity);
        }
        return this.entityList;
    }
}
