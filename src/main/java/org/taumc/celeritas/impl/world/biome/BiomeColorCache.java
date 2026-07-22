package org.taumc.celeritas.impl.world.biome;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;

public final class BiomeColorCache extends org.embeddedt.embeddium.impl.biome.BiomeColorCache<Biome, BiomeColorCache.ColorType> {
    private final Source source;
    private final BlockPos.Mutable cursor = new BlockPos.Mutable();

    public BiomeColorCache(int blendRadius) {
        this(new Source(), blendRadius);
    }

    private BiomeColorCache(Source source, int blendRadius) {
        super(source::getBiome, blendRadius);
        this.source = source;
    }

    public void update(ChunkRenderContext context) {
        this.source.context = context;
        super.update(context.origin());
    }

    @Override
    protected int resolveColor(ColorType type, Biome biome, int x, int y, int z) {
        this.cursor.set(x, y, z);
        return switch (type) {
            case GRASS -> biome.getGrassColor(this.cursor);
            case FOLIAGE -> biome.getFoliageColor(this.cursor);
            case WATER -> biome.waterFogColor;
        };
    }

    public enum ColorType {
        GRASS,
        FOLIAGE,
        WATER
    }

    private static final class Source {
        private ChunkRenderContext context;

        private Biome getBiome(int x, int y, int z) {
            return this.context.getBiome(x, z);
        }
    }
}
