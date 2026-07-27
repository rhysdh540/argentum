package dev.rdh.argentum.impl.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import org.embeddedt.embeddium.impl.util.PositionUtil;

import net.minecraft.world.World;

public final class ClonedChunkSectionCache {
    private static final int MAX_SIZE = 512;

    private final World world;
    private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> sections = new Long2ReferenceLinkedOpenHashMap<>();

    public ClonedChunkSectionCache(World world) {
        this.world = world;
    }

    ClonedChunkSection acquire(int x, int y, int z) {
        long key = PositionUtil.packSection(x, y, z);
        ClonedChunkSection section = this.sections.getAndMoveToLast(key);

        if (section == null) {
            section = new ClonedChunkSection(this.world, x, y, z);
            if (this.sections.size() >= MAX_SIZE) {
                this.sections.removeFirst();
            }
            this.sections.putAndMoveToLast(key, section);
        }

        return section;
    }

    public void invalidate(int x, int y, int z) {
        this.sections.remove(PositionUtil.packSection(x, y, z));
    }
}
