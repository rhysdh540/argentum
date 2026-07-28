package dev.rdh.cera.modules.ctm;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;

import java.util.List;

import static org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags.IS_ALIGNED;

final class PaneCulling {
    private static final float CENTER_MIN = 7 / 16F;
    private static final float CENTER_MAX = 9 / 16F;

    private PaneCulling() {
    }

    static void validate() {
        List<Range> exposed = exposed(0, 1, false, false);
        if (!exposed.equals(List.of(new Range(0, CENTER_MIN), new Range(CENTER_MAX, 1)))) {
            throw new IllegalStateException("Partial pane overlap must leave both ends exposed");
        }
        if (!exposed(0, CENTER_MAX, true, false).isEmpty()) {
            throw new IllegalStateException("Covered pane arm must not remain visible");
        }
    }

    static List<BakedQuad> cull(WorldView world, BlockState state, BlockPos pos,
            BakedQuad quad, TextureAtlasSprite sprite) {
        Block block = state.getBlock();
        if (!(block instanceof PaneBlock)) return null;

        Direction face = quad.getFace();
        BlockPos neighborPos = pos.offset(face);
        BlockState neighbor = world.getBlockState(neighborPos);
        BakedQuadView view = BakedQuadView.of(quad);
        boolean aligned = (view.getFlags() & IS_ALIGNED) != 0;
        if (sprite.getName().startsWith("minecraft:blocks/glass_pane_top")
                && neighbor == state && (face.getAxis() != Direction.Axis.Y || !aligned)) {
            return List.of();
        }
        if (face.getAxis() != Direction.Axis.Y || !aligned || neighbor.getBlock() != block) {
            return null;
        }
        if (block == Blocks.STAINED_GLASS_PANE
                && neighbor.get(StainedGlassPaneBlock.COLOR) != state.get(StainedGlassPaneBlock.COLOR)) {
            return null;
        }

        neighbor = block.resolveVirtualProperties(neighbor, world, neighborPos);
        boolean west = neighbor.get(PaneBlock.WEST);
        boolean east = neighbor.get(PaneBlock.EAST);
        boolean north = neighbor.get(PaneBlock.NORTH);
        boolean south = neighbor.get(PaneBlock.SOUTH);
        if (!west && !east && !north && !south) west = east = north = south = true;

        float minX = minX(view);
        float maxX = maxX(view);
        float minZ = minZ(view);
        float maxZ = maxZ(view);
        List<Range> ranges;
        boolean alongX = maxX - minX > maxZ - minZ;
        if (alongX) {
            ranges = exposed(minX, maxX, west, east);
        } else if (maxZ - minZ > maxX - minX) {
            ranges = exposed(minZ, maxZ, north, south);
        } else {
            return List.of();
        }

        List<BakedQuad> result = new ObjectArrayList<>(ranges.size());
        for (Range range : ranges) {
            result.add(alongX
                    ? clip(quad, range.min, range.max, minZ, maxZ)
                    : clip(quad, minX, maxX, range.min, range.max));
        }
        return result;
    }

    private static List<Range> exposed(float min, float max, boolean negative, boolean positive) {
        float coveredMin = negative ? 0 : CENTER_MIN;
        float coveredMax = positive ? 1 : CENTER_MAX;
        if (coveredMax <= min || coveredMin >= max) return List.of(new Range(min, max));

        List<Range> result = new ObjectArrayList<>(2);
        if (min < coveredMin) result.add(new Range(min, Math.min(max, coveredMin)));
        if (max > coveredMax) result.add(new Range(Math.max(min, coveredMax), max));
        return result;
    }

    private static BakedQuad clip(BakedQuad quad,
            float minX, float maxX, float minZ, float maxZ) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        UvTransform transform = UvTransform.of(vertices, stride);
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float clippedX = Math.clamp(x, minX, maxX);
            float clippedZ = Math.clamp(z, minZ, maxZ);
            float dx = clippedX - x;
            float dz = clippedZ - z;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);
            vertices[offset] = Float.floatToRawIntBits(clippedX);
            vertices[offset + 2] = Float.floatToRawIntBits(clippedZ);
            vertices[offset + 4] = Float.floatToRawIntBits(u + transform.ux * dx + transform.uz * dz);
            vertices[offset + 5] = Float.floatToRawIntBits(v + transform.vx * dx + transform.vz * dz);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getFace());
    }

    private static float minX(BakedQuadView quad) {
        return Math.min(Math.min(quad.getX(0), quad.getX(1)), Math.min(quad.getX(2), quad.getX(3)));
    }

    private static float maxX(BakedQuadView quad) {
        return Math.max(Math.max(quad.getX(0), quad.getX(1)), Math.max(quad.getX(2), quad.getX(3)));
    }

    private static float minZ(BakedQuadView quad) {
        return Math.min(Math.min(quad.getZ(0), quad.getZ(1)), Math.min(quad.getZ(2), quad.getZ(3)));
    }

    private static float maxZ(BakedQuadView quad) {
        return Math.max(Math.max(quad.getZ(0), quad.getZ(1)), Math.max(quad.getZ(2), quad.getZ(3)));
    }

    private record Range(float min, float max) {
    }

    private record UvTransform(float ux, float uz, float vx, float vz) {
        private static UvTransform of(int[] data, int stride) {
            int first = stride;
            int second = stride * 3;
            float firstX = Float.intBitsToFloat(data[first]) - Float.intBitsToFloat(data[0]);
            float firstZ = Float.intBitsToFloat(data[first + 2]) - Float.intBitsToFloat(data[2]);
            float secondX = Float.intBitsToFloat(data[second]) - Float.intBitsToFloat(data[0]);
            float secondZ = Float.intBitsToFloat(data[second + 2]) - Float.intBitsToFloat(data[2]);
            float determinant = firstX * secondZ - secondX * firstZ;
            if (Math.abs(determinant) < 1.0E-6F) return new UvTransform(0, 0, 0, 0);

            float firstU = Float.intBitsToFloat(data[first + 4]) - Float.intBitsToFloat(data[4]);
            float firstV = Float.intBitsToFloat(data[first + 5]) - Float.intBitsToFloat(data[5]);
            float secondU = Float.intBitsToFloat(data[second + 4]) - Float.intBitsToFloat(data[4]);
            float secondV = Float.intBitsToFloat(data[second + 5]) - Float.intBitsToFloat(data[5]);
            return new UvTransform(
                    (firstU * secondZ - secondU * firstZ) / determinant,
                    (secondU * firstX - firstU * secondX) / determinant,
                    (firstV * secondZ - secondV * firstZ) / determinant,
                    (secondV * firstX - firstV * secondX) / determinant);
        }
    }
}
