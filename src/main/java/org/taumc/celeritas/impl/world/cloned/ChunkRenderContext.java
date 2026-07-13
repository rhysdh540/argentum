package org.taumc.celeritas.impl.world.cloned;

import org.embeddedt.embeddium.impl.util.position.SectionPos;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;
import net.minecraft.world.gen.WorldGeneratorType;

public final class ChunkRenderContext implements WorldView {
    private static final int SECTION_LENGTH = 3;
    private static final int SECTION_COUNT = SECTION_LENGTH * SECTION_LENGTH * SECTION_LENGTH;

    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final WorldGeneratorType generatorType;
    private final boolean hasSky;

    private ChunkRenderContext(World world, SectionPos origin, ClonedChunkSection[] sections) {
        this.origin = origin;
        this.sections = sections;
        this.generatorType = world.getGeneratorType();
        this.hasSky = !world.dimension.hasNoSky();
    }

    public static @Nullable ChunkRenderContext prepare(World world, SectionPos origin, ClonedChunkSectionCache cache) {
        if (isSectionEmpty(world, origin.x(), origin.y(), origin.z())) {
            return null;
        }

        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_COUNT];
        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    sections[getSectionIndex(x, y, z)] = cache.acquire(origin.x() + x - 1, origin.y() + y - 1, origin.z() + z - 1);
                }
            }
        }

        return new ChunkRenderContext(world, origin, sections);
    }

    public static boolean isSectionEmpty(World world, int x, int y, int z) {
        WorldChunk chunk = world.getChunkAt(x, z);
        if (chunk.isEmpty()) {
            return true;
        }

        WorldChunkSection[] sections = chunk.getSections();
        return y < 0 || y >= sections.length || sections[y] == null || sections[y].isEmpty();
    }

    public SectionPos origin() {
        return this.origin;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        ClonedChunkSection section = this.getSection(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        return section == null ? Blocks.AIR.defaultState() : section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        ClonedChunkSection section = this.getSection(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        return section == null ? null : section.getBlockEntity(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    @Override
    public int getLightColor(BlockPos pos, int ambientLight) {
        int sky = this.getBrightness(LightType.SKY, pos);
        int block = Math.max(this.getBrightness(LightType.BLOCK, pos), ambientLight);
        return sky << 20 | block << 4;
    }

    private int getBrightness(LightType type, BlockPos pos) {
        if (type == LightType.SKY && !this.hasSky) {
            return 0;
        }

        if (pos.getY() < 0 || pos.getY() >= 256) {
            return type.defaultValue;
        }

        if (!this.getBlockState(pos).getBlock().usesNeighborLight()) {
            return this.getLight(type, pos);
        }

        int light = 0;
        for (Direction direction : Direction.values()) {
            light = Math.max(light, this.getLight(type, pos.offset(direction)));
            if (light == 15) {
                break;
            }
        }
        return light;
    }

    private int getLight(LightType type, BlockPos pos) {
        ClonedChunkSection section = this.getSection(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        return section == null ? type.defaultValue : section.getLight(type, pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        ClonedChunkSection section = this.getSection(pos.getX() >> 4, this.origin.y(), pos.getZ() >> 4);
        return section == null ? Biome.DEFAULT : section.getBiome(pos.getX() & 15, pos.getZ() & 15);
    }

    @Override
    public boolean isAir(BlockPos pos) {
        return this.getBlockState(pos).getBlock().getMaterial() == Material.AIR;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int getDirectSignal(BlockPos pos, Direction direction) {
        BlockState state = this.getBlockState(pos);
        return state.getBlock().getDirectSignal(this, pos, state, direction);
    }

    @Override
    public WorldGeneratorType getGeneratorType() {
        return this.generatorType;
    }

    private @Nullable ClonedChunkSection getSection(int x, int y, int z) {
        int localX = x - this.origin.x() + 1;
        int localY = y - this.origin.y() + 1;
        int localZ = z - this.origin.z() + 1;
        if ((localX | localY | localZ) < 0 || localX >= SECTION_LENGTH || localY >= SECTION_LENGTH || localZ >= SECTION_LENGTH) {
            return null;
        }
        return this.sections[getSectionIndex(localX, localY, localZ)];
    }

    private static int getSectionIndex(int x, int y, int z) {
        return (x * SECTION_LENGTH + y) * SECTION_LENGTH + z;
    }
}
