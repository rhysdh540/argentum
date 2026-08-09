package dev.rdh.cera.modules.ctm;

import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.List;

public final class CompactCtm {
    private static final int W = 1;
    private static final int S = 1 << 2;
    private static final int E = 1 << 4;
    private static final int N = 1 << 6;
    private static final byte[][] TILES = createTiles();

    private CompactCtm() {
    }

    public static void validate() {
        require(0, 0, 0, 0, 0);
        require(255, 1, 1, 1, 1);
        require(W | S | E | N, 4, 4, 4, 4);
        require(N, 2, 2, 0, 0);
        require(W, 3, 0, 3, 0);

        TextureAtlasSprite sprite = new TestSprite();
        sprite.init(16, 16, 0, 0, false);
        int[] vertices = new int[28];
        vertex(vertices, 0, 0, 0, 0, sprite.getU(16), sprite.getV(16));
        vertex(vertices, 1, 0, 1, 0, sprite.getU(16), sprite.getV(0));
        vertex(vertices, 2, 0, 1, 9 / 16F, sprite.getU(7), sprite.getV(0));
        vertex(vertices, 3, 0, 0, 9 / 16F, sprite.getU(7), sprite.getV(16));
        BakedQuad quad = new BakedQuad(vertices, -1, Direction.WEST);
        QuadGeometry geometry = QuadGeometry.of(quad, sprite);
        BakedQuad clipped = part(quad, geometry, sprite, sprite, Region.LEFT);
        for (int vertex = 0; vertex < 4; vertex++) {
            float z = Float.intBitsToFloat(clipped.getVertices()[vertex * 7 + 2]);
            if (z < 0 || z > 9 / 16F) {
                throw new IllegalStateException("Compact CTM escaped its source quad");
            }
        }
    }

    private static void vertex(int[] data, int vertex, float x, float y, float z, float u, float v) {
        int offset = vertex * 7;
        data[offset] = Float.floatToRawIntBits(x);
        data[offset + 1] = Float.floatToRawIntBits(y);
        data[offset + 2] = Float.floatToRawIntBits(z);
        data[offset + 4] = Float.floatToRawIntBits(u);
        data[offset + 5] = Float.floatToRawIntBits(v);
    }

    static List<BakedQuad> transform(BakedQuad quad, QuadGeometry geometry, TextureAtlasSprite from,
                                     TextureAtlasSprite[] sprites, int connections) {
        if (geometry.transform == null) return null;
        byte[] tiles = TILES[connections & 255];
        int nw = tiles[0];
        int ne = tiles[1];
        int sw = tiles[2];
        int se = tiles[3];
        if (sprites[nw] == null || sprites[ne] == null || sprites[sw] == null || sprites[se] == null) {
            return null;
        }
        if (nw == ne && nw == sw && nw == se) {
            return List.of(part(quad, geometry, from, sprites[nw], Region.FULL));
        }
        if (nw == ne && sw == se) {
            return List.of(
                    part(quad, geometry, from, sprites[nw], Region.TOP),
                    part(quad, geometry, from, sprites[sw], Region.BOTTOM)
            );
        }
        if (nw == sw && ne == se) {
            return List.of(
                    part(quad, geometry, from, sprites[nw], Region.LEFT),
                    part(quad, geometry, from, sprites[ne], Region.RIGHT)
            );
        }
        if (nw == ne) {
            return List.of(
                    part(quad, geometry, from, sprites[nw], Region.TOP),
                    part(quad, geometry, from, sprites[sw], Region.BOTTOM_LEFT),
                    part(quad, geometry, from, sprites[se], Region.BOTTOM_RIGHT)
            );
        }
        if (sw == se) {
            return List.of(
                    part(quad, geometry, from, sprites[nw], Region.TOP_LEFT),
                    part(quad, geometry, from, sprites[ne], Region.TOP_RIGHT),
                    part(quad, geometry, from, sprites[sw], Region.BOTTOM)
            );
        }
        if (nw == sw) {
            return List.of(
                    part(quad, geometry, from, sprites[nw], Region.LEFT),
                    part(quad, geometry, from, sprites[ne], Region.TOP_RIGHT),
                    part(quad, geometry, from, sprites[se], Region.BOTTOM_RIGHT)
            );
        }
        if (ne == se) {
            return List.of(
                    part(quad, geometry, from, sprites[nw], Region.TOP_LEFT),
                    part(quad, geometry, from, sprites[sw], Region.BOTTOM_LEFT),
                    part(quad, geometry, from, sprites[ne], Region.RIGHT)
            );
        }
        return List.of(
                part(quad, geometry, from, sprites[nw], Region.TOP_LEFT),
                part(quad, geometry, from, sprites[ne], Region.TOP_RIGHT),
                part(quad, geometry, from, sprites[sw], Region.BOTTOM_LEFT),
                part(quad, geometry, from, sprites[se], Region.BOTTOM_RIGHT)
        );
    }

    private static byte[][] createTiles() {
        byte[][] tiles = new byte[256][4];
        for (int mask = 0; mask < tiles.length; mask++) {
            tiles[mask][0] = tile(mask, W, N, 1 << 7);
            tiles[mask][1] = tile(mask, E, N, 1 << 5);
            tiles[mask][2] = tile(mask, W, S, 1 << 1);
            tiles[mask][3] = tile(mask, E, S, 1 << 3);
        }
        return tiles;
    }

    private static byte tile(int mask, int horizontal, int vertical, int corner) {
        boolean h = (mask & horizontal) != 0;
        boolean v = (mask & vertical) != 0;
        if (!h) return (byte)(v ? 2 : 0);
        if (!v) return 3;
        return (byte)((mask & corner) != 0 ? 1 : 4);
    }

    private static void require(int mask, int... expected) {
        if (!Arrays.equals(TILES[mask], new byte[]{
                (byte)expected[0], (byte)expected[1], (byte)expected[2], (byte)expected[3]})) {
            throw new IllegalStateException("Invalid compact CTM mapping for " + mask);
        }
    }

    private static BakedQuad part(BakedQuad quad, QuadGeometry geometry, TextureAtlasSprite from, TextureAtlasSprite to, Region region) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            clip(vertices, offset, from, to, geometry.transform, region);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getFace());
    }

    private static void clip(int[] data, int offset, TextureAtlasSprite from, TextureAtlasSprite to, QuadGeometry.PositionTransform transform, Region region) {
        float u = local(Float.intBitsToFloat(data[offset + 4]), from.getUMin(), from.getUMax());
        float v = local(Float.intBitsToFloat(data[offset + 5]), from.getVMin(), from.getVMax());
        float x = Float.intBitsToFloat(data[offset]);
        float y = Float.intBitsToFloat(data[offset + 1]);
        float z = Float.intBitsToFloat(data[offset + 2]);
        float clippedU = Math.clamp(u, region.x1, region.x2);
        float clippedV = Math.clamp(v, region.y1, region.y2);
        float du = clippedU - u;
        float dv = clippedV - v;
        x += transform.xu() * du + transform.xv() * dv;
        y += transform.yu() * du + transform.yv() * dv;
        z += transform.zu() * du + transform.zv() * dv;

        data[offset] = Float.floatToRawIntBits(x);
        data[offset + 1] = Float.floatToRawIntBits(y);
        data[offset + 2] = Float.floatToRawIntBits(z);
        data[offset + 4] = Float.floatToRawIntBits(atlas(clippedU, to.getUMin(), to.getUMax()));
        data[offset + 5] = Float.floatToRawIntBits(atlas(clippedV, to.getVMin(), to.getVMax()));
    }

    private static float local(float value, float min, float max) {
        return (value - min) / (max - min) * 16;
    }

    private static float atlas(float value, float min, float max) {
        return min + value / 16 * (max - min);
    }

    private enum Region {
        FULL(0, 0, 16, 16),
        TOP(0, 0, 16, 8),
        TOP_RIGHT(8, 0, 16, 8),
        RIGHT(8, 0, 16, 16),
        BOTTOM_RIGHT(8, 8, 16, 16),
        BOTTOM(0, 8, 16, 16),
        BOTTOM_LEFT(0, 8, 8, 16),
        LEFT(0, 0, 8, 16),
        TOP_LEFT(0, 0, 8, 8);

        private final int x1;
        private final int y1;
        private final int x2;
        private final int y2;

        Region(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    private static final class TestSprite extends TextureAtlasSprite {
        private TestSprite() {
            super("cera:test");
        }
    }
}
