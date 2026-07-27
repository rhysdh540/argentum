package dev.rdh.argentum.impl.render.terrain;

import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;

import java.util.ArrayDeque;

public final class ChunkRenderListPool {
    private static final ArrayDeque<ChunkRenderList> POOL = new ArrayDeque<>();

    private ChunkRenderListPool() {
    }

    public static ChunkRenderList acquire(RenderRegion region) {
        ChunkRenderList list = POOL.pollLast();

        if (list == null) {
            return new ChunkRenderList(region);
        }

        ((Resettable) list).celeritas$reset(region);
        return list;
    }

    public static void release(SortedRenderLists lists) {
        var iterator = lists.iterator();

        while (iterator.hasNext()) {
            ChunkRenderList list = iterator.next();
            ((Resettable) list).celeritas$reset(null);
            POOL.addLast(list);
        }
    }

    public interface Resettable {
        void celeritas$reset(RenderRegion region);
    }
}
