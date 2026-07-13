package org.taumc.celeritas.impl.world.cloned;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.embeddedt.embeddium.impl.util.position.SectionPos;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkNibbleStorage;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;

import java.util.Arrays;
import java.util.Map;

final class ClonedChunkSection {
    private final SectionPos position;
    private final WorldChunkSection data;
    private final ChunkNibbleStorage emptySectionSkyLight;
    private final Short2ObjectMap<BlockEntity> blockEntities = new Short2ObjectOpenHashMap<>();
    private final byte[] biomes;
    private final boolean hasSky;

    ClonedChunkSection(World world, int sectionX, int sectionY, int sectionZ) {
        this.position = new SectionPos(sectionX, sectionY, sectionZ);

        WorldChunk chunk = world.getChunkAt(sectionX, sectionZ);
        WorldChunkSection source = getSection(chunk, sectionY);
        this.hasSky = !world.dimension.hasNoSky();

        if (source == null) {
            this.data = null;
            this.emptySectionSkyLight = createEmptySectionSkyLight(chunk, sectionY, this.hasSky);
        } else {
            this.data = copy(source, this.hasSky);
            this.emptySectionSkyLight = null;
        }

        this.biomes = Arrays.copyOf(chunk.getBiomes(), 256);
        copyBlockEntities(chunk, sectionY);
    }

    private static WorldChunkSection getSection(WorldChunk chunk, int sectionY) {
        WorldChunkSection[] sections = chunk.getSections();
        return sectionY >= 0 && sectionY < sections.length ? sections[sectionY] : null;
    }

    private static WorldChunkSection copy(WorldChunkSection source, boolean hasSky) {
        WorldChunkSection copy = new WorldChunkSection(source.getOffsetY(), hasSky);
        copy.setBlockStates(Arrays.copyOf(source.getBlockStates(), source.getBlockStates().length));
        copy.setBlockLightStorage(copy(source.getBlockLightStorage()));

        if (hasSky && source.getSkyLightStorage() != null) {
            copy.setSkyLightStorage(copy(source.getSkyLightStorage()));
        }

        return copy;
    }

    private static ChunkNibbleStorage copy(ChunkNibbleStorage source) {
        return new ChunkNibbleStorage(Arrays.copyOf(source.getData(), source.getData().length));
    }

    private static ChunkNibbleStorage createEmptySectionSkyLight(WorldChunk chunk, int sectionY, boolean hasSky) {
        if (!hasSky || sectionY < 0 || sectionY >= 16) {
            return null;
        }

        ChunkNibbleStorage light = new ChunkNibbleStorage();
        int minY = sectionY << 4;
        int[] heightMap = chunk.getHeightMap();

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int height = heightMap[z << 4 | x];
                for (int y = Math.max(0, height - minY); y < 16; y++) {
                    light.set(x, y, z, LightType.SKY.defaultValue);
                }
            }
        }

        return light;
    }

    private void copyBlockEntities(WorldChunk chunk, int sectionY) {
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() >> 4 == sectionY) {
                this.blockEntities.put(packLocal(pos.getX(), pos.getY(), pos.getZ()), entry.getValue());
            }
        }
    }

    BlockState getBlockState(int x, int y, int z) {
        return this.data == null ? Blocks.AIR.defaultState() : this.data.getBlockState(x, y, z);
    }

    BlockEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(packLocal(x, y, z));
    }

    int getLight(LightType type, int x, int y, int z) {
        if (this.data != null) {
            return type == LightType.SKY ? this.data.getSkyLight(x, y, z) : this.data.getBlockLight(x, y, z);
        }

        if (type != LightType.SKY || !this.hasSky) {
            return 0;
        }
        if (this.position.y() < 0 || this.position.y() >= 16) {
            return LightType.SKY.defaultValue;
        }
        return this.emptySectionSkyLight == null ? 0 : this.emptySectionSkyLight.get(x, y, z);
    }

    Biome getBiome(int x, int z) {
        return Biome.byId(this.biomes[z << 4 | x] & 255, Biome.DEFAULT);
    }

    SectionPos getPosition() {
        return this.position;
    }

    private static short packLocal(int x, int y, int z) {
        return (short)((x & 15) << 8 | (z & 15) << 4 | y & 15);
    }
}
