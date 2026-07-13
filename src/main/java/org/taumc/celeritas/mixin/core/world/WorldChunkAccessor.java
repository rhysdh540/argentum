package org.taumc.celeritas.mixin.core.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public interface WorldChunkAccessor {
    @Accessor("lastSaveHadEntities")
    boolean getHasEntities();
}
