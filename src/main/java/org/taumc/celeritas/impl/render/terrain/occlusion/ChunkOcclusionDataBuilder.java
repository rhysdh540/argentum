package org.taumc.celeritas.impl.render.terrain.occlusion;

import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;

import java.util.BitSet;

public final class ChunkOcclusionDataBuilder {
    private static final int BLOCK_COUNT = 16 * 16 * 16;
    private static final int[] EDGE_POINTS = createEdgePoints();

    private final BitSet closed = new BitSet(BLOCK_COUNT);
    private final int[] queue = new int[BLOCK_COUNT];

    public void reset() {
        this.closed.clear();
    }

    public void markClosed(int x, int y, int z) {
        this.closed.set(pack(x, y, z));
    }

    public long computeVisibilityEncoding() {
        int closedCount = this.closed.cardinality();
        if (closedCount < 256) {
            return VisibilityEncoding.EVERYTHING;
        }
        if (closedCount == BLOCK_COUNT) {
            return VisibilityEncoding.NULL;
        }

        long visibility = 0L;
        for (int point : EDGE_POINTS) {
            if (!this.closed.get(point)) {
                visibility |= encodeFaces(this.findOpenFaces(point));
            }
        }
        return visibility;
    }

    private int findOpenFaces(int start) {
        int read = 0;
        int write = 0;
        int faces = 0;
        this.queue[write++] = start;
        this.closed.set(start);

        while (read < write) {
            int pos = this.queue[read++];
            int x = pos & 15;
            int z = pos >> 4 & 15;
            int y = pos >> 8 & 15;

            if (y == 0) faces |= 1 << GraphDirection.DOWN;
            if (y == 15) faces |= 1 << GraphDirection.UP;
            if (z == 0) faces |= 1 << GraphDirection.NORTH;
            if (z == 15) faces |= 1 << GraphDirection.SOUTH;
            if (x == 0) faces |= 1 << GraphDirection.WEST;
            if (x == 15) faces |= 1 << GraphDirection.EAST;

            if (y > 0) write = this.enqueue(pos - 256, write);
            if (y < 15) write = this.enqueue(pos + 256, write);
            if (z > 0) write = this.enqueue(pos - 16, write);
            if (z < 15) write = this.enqueue(pos + 16, write);
            if (x > 0) write = this.enqueue(pos - 1, write);
            if (x < 15) write = this.enqueue(pos + 1, write);
        }

        return faces;
    }

    private int enqueue(int pos, int write) {
        if (!this.closed.get(pos)) {
            this.closed.set(pos);
            this.queue[write++] = pos;
        }
        return write;
    }

    private static long encodeFaces(int faces) {
        long encoding = 0L;
        for (int from = 0; from < GraphDirection.COUNT; from++) {
            if ((faces & 1 << from) == 0) continue;
            for (int to = 0; to < GraphDirection.COUNT; to++) {
                if ((faces & 1 << to) != 0) {
                    encoding |= 1L << (from * 8 + to);
                }
            }
        }
        return encoding;
    }

    private static int[] createEdgePoints() {
        int[] points = new int[1352];
        int index = 0;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
                        points[index++] = pack(x, y, z);
                    }
                }
            }
        }
        return points;
    }

    private static int pack(int x, int y, int z) {
        return (x & 15) | (z & 15) << 4 | (y & 15) << 8;
    }
}
