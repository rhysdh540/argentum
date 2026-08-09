package dev.rdh.cera.modules.ctm;

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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;

import java.util.Comparator;
import java.util.List;

import static org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags.IS_ALIGNED;

final class PaneCulling {
    private static final float CENTER_MIN = 7 / 16F;
    private static final float CENTER_MAX = 9 / 16F;
    private static final float EPSILON = 1.0E-6F;

    private PaneCulling() {
    }

    static void validate() {
        int[] vertical = new int[28];
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * 7;
            float x = vertex < 2 ? 0 : 1;
            float y = vertex == 1 || vertex == 2 ? 1 : 0;
            vertical[offset] = Float.floatToRawIntBits(x);
            vertical[offset + 1] = Float.floatToRawIntBits(y);
            vertical[offset + 4] = Float.floatToRawIntBits(x * 16);
            vertical[offset + 5] = Float.floatToRawIntBits(y * 16);
        }
        UvTransform transform = UvTransform.of(vertical, 7, true);
        BakedQuad south = new BakedQuad(vertical, -1, Direction.SOUTH);
        BakedQuad north = new BakedQuad(vertical.clone(), -1, Direction.NORTH);
        List<Range> disjoint = subtract(0, 1, new ObjectArrayList<>(List.of(
                new Range(0.2F, 0.3F), new Range(0.7F, 0.8F))
        ));
        if (visibleParts(0, 1, CENTER_MIN, CENTER_MAX) != 3
                || visibleParts(0, CENTER_MAX, 0, CENTER_MAX) != 0
                || visibleParts(0, CENTER_MIN, CENTER_MIN, CENTER_MAX) != -1
                || !hasMinBoundary(0, true) || !hasMaxBoundary(0, true)
                || hasMinBoundary(3, true) || hasMaxBoundary(3, true)
                || transform == null || transform.u != 16 || transform.v != 0
                || UvTransform.of(new int[28], 7, true) != null
                || !equal(1, 1 + EPSILON / 2)
                || !innerFaces(south, BakedQuadView.of(south), north, BakedQuadView.of(north))
                || !disjoint.equals(List.of(new Range(0, 0.2F), new Range(0.3F, 0.7F),
                        new Range(0.8F, 1)))
                || oppositeConnection(Direction.WEST) != 2
                || oppositeConnection(Direction.EAST) != 1
                || oppositeConnection(Direction.NORTH) != 8
                || oppositeConnection(Direction.SOUTH) != 4) {
            throw new IllegalStateException("Invalid partial pane culling");
        }
    }

    static List<BakedQuad> cullInnerFaces(BlockState state, List<BakedQuad> quads,
            CtmRenderContext context) {
        if (!(state.getBlock() instanceof PaneBlock) || quads.size() < 2) return quads;
        List<BakedQuad> cached = context.paneInnerFaces(quads);
        if (cached != null) return cached;

        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            Direction face = quad.getFace();
            if (face.getAxis() == Direction.Axis.Y) {
                if (result != null) result.add(quad);
                continue;
            }

            BakedQuadView view = BakedQuadView.of(quad);
            boolean alongX = face.getAxis() == Direction.Axis.Z;
            float min = alongX ? minX(view) : minZ(view);
            float max = alongX ? maxX(view) : maxZ(view);
            List<Range> covered = null;
            for (int j = 0; j < quads.size(); j++) {
                if (i == j) continue;
                BakedQuad other = quads.get(j);
                BakedQuadView otherView = BakedQuadView.of(other);
                if (!innerFaces(quad, view, other, otherView)) continue;
                float otherMin = alongX ? minX(otherView) : minZ(otherView);
                float otherMax = alongX ? maxX(otherView) : maxZ(otherView);
                if (covered == null) covered = new ObjectArrayList<>();
                covered.add(new Range(Math.max(min, otherMin), Math.min(max, otherMax)));
            }

            if (covered == null) {
                if (result != null) result.add(quad);
                continue;
            }
            List<Range> visible = subtract(min, max, covered);
            if (result == null) {
                result = new ObjectArrayList<>(quads.size());
                result.addAll(quads.subList(0, i));
            }
            if (visible.isEmpty()) continue;
            int[] vertices = quad.getVertices();
            UvTransform transform = UvTransform.of(vertices, vertices.length / 4, alongX);
            if (transform == null) {
                result.add(quad);
                continue;
            }
            for (Range range : visible) {
                result.add(clip(quad, transform, alongX, range.min, range.max));
            }
        }
        List<BakedQuad> culled = result == null ? quads : List.copyOf(result);
        context.putPaneInnerFaces(quads, culled);
        return culled;
    }

    private static List<Range> subtract(float min, float max, List<Range> covered) {
        covered.sort(Comparator.comparingDouble(Range::min));
        List<Range> visible = new ObjectArrayList<>(covered.size() + 1);
        float cursor = min;
        for (Range range : covered) {
            float start = Math.max(min, range.min);
            float end = Math.min(max, range.max);
            if (end <= cursor + EPSILON) continue;
            if (start > cursor + EPSILON) visible.add(new Range(cursor, start));
            cursor = Math.max(cursor, end);
            if (cursor >= max - EPSILON) break;
        }
        if (cursor < max - EPSILON) visible.add(new Range(cursor, max));
        return List.copyOf(visible);
    }

    private static boolean innerFaces(BakedQuad first, BakedQuadView firstView,
            BakedQuad second, BakedQuadView secondView) {
        Direction face = first.getFace();
        if (face.getOpposite() != second.getFace() || face.getAxis() == Direction.Axis.Y) return false;
        return switch (face.getAxis()) {
            case X -> equal(minX(firstView), maxX(firstView))
                    && equal(minX(firstView), minX(secondView))
                    && equal(minY(firstView), minY(secondView))
                    && equal(maxY(firstView), maxY(secondView))
                    && Math.max(minZ(firstView), minZ(secondView))
                    < Math.min(maxZ(firstView), maxZ(secondView));
            case Z -> equal(minZ(firstView), maxZ(firstView))
                    && equal(minZ(firstView), minZ(secondView))
                    && equal(minY(firstView), minY(secondView))
                    && equal(maxY(firstView), maxY(secondView))
                    && Math.max(minX(firstView), minX(secondView))
                    < Math.min(maxX(firstView), maxX(secondView));
            default -> false;
        };
    }

    private static boolean equal(float first, float second) {
        return Math.abs(first - second) < EPSILON;
    }

    static List<BakedQuad> cull(WorldView world, BlockState state, BlockPos pos,
            BakedQuad quad, TextureAtlasSprite sprite, CtmRenderContext context) {
        Block block = state.getBlock();
        if (!(block instanceof PaneBlock)) return null;

        Direction face = quad.getFace();
        BakedQuadView view = BakedQuadView.of(quad);
        boolean aligned = (view.getFlags() & IS_ALIGNED) != 0;
        if (face.getAxis() != Direction.Axis.Y) {
            if (sprite.getName().startsWith("minecraft:blocks/glass_pane_top")) {
                return world.getBlockState(context.offset(pos, face)) == state
                        ? List.of() : null;
            }
            return splitSide(world, state, pos, quad, context);
        }
        if (!aligned) return null;

        int mask = paneMask(world, state, pos, face, context);
        if (mask < 0) return null;

        float minX = minX(view);
        float maxX = maxX(view);
        float minZ = minZ(view);
        float maxZ = maxZ(view);
        float lengthX = maxX - minX;
        float lengthZ = maxZ - minZ;
        if (equal(lengthX, lengthZ)) return List.of();
        boolean alongX = lengthX > lengthZ;

        float min = alongX ? minX : minZ;
        float max = alongX ? maxX : maxZ;
        int axisMask = axisMask(mask, alongX);
        List<BakedQuad> cached = context.paneParts(quad, axisMask);
        if (cached != null) return cached;
        float coveredMin = coveredMin((axisMask & 1) != 0);
        float coveredMax = coveredMax((axisMask & 2) != 0);
        int parts = visibleParts(min, max, coveredMin, coveredMax);
        if (parts < 0) return null;
        if (parts == 0) {
            context.putPaneParts(quad, axisMask, List.of());
            return List.of();
        }

        int[] vertices = quad.getVertices();
        UvTransform transform = UvTransform.of(vertices, vertices.length / 4, alongX);
        if (transform == null) return null;
        BakedQuad first = (parts & 1) != 0
                ? clip(quad, transform, alongX, min, Math.min(max, coveredMin))
                : clip(quad, transform, alongX, Math.max(min, coveredMax), max);
        List<BakedQuad> result = parts == 3
                ? List.of(first, clip(quad, transform, alongX, Math.max(min, coveredMax), max))
                : List.of(first);
        context.putPaneParts(quad, axisMask, result);
        return result;
    }

    static boolean covers(WorldView world, BlockState state, BlockPos pos, Direction direction,
            BakedQuad quad, CtmRenderContext context) {
        if (direction.getAxis() != Direction.Axis.Y || quad.getFace().getAxis() == Direction.Axis.Y) {
            return true;
        }
        int mask = paneMask(world, state, pos, direction, context);
        if (mask < 0) return true;
        BakedQuadView view = BakedQuadView.of(quad);
        boolean alongX = quad.getFace().getAxis() == Direction.Axis.Z;
        float middle = alongX ? (minX(view) + maxX(view)) / 2 : (minZ(view) + maxZ(view)) / 2;
        int axisMask = axisMask(mask, alongX);
        return middle >= coveredMin((axisMask & 1) != 0)
                && middle <= coveredMax((axisMask & 2) != 0);
    }

    static boolean coversCorner(WorldView world, BlockState state, BlockPos corner,
            Direction first, Direction second, BakedQuad quad) {
        if (quad.getFace().getAxis() == Direction.Axis.Y) return true;
        Direction horizontal = first.getAxis() == Direction.Axis.Y ? second : first;
        if (horizontal.getAxis() == Direction.Axis.Y) return true;
        int mask = loadPaneMask(world, state, corner);
        if (mask < 0) return true;
        return (mask & oppositeConnection(horizontal)) != 0;
    }

    private static int oppositeConnection(Direction direction) {
        return switch (direction) {
            case WEST -> 2;
            case EAST -> 1;
            case NORTH -> 8;
            case SOUTH -> 4;
            default -> 0;
        };
    }

    private static List<BakedQuad> splitSide(WorldView world, BlockState state, BlockPos pos,
            BakedQuad quad, CtmRenderContext context) {
        boolean alongX = quad.getFace().getAxis() == Direction.Axis.Z;
        int up = paneMask(world, state, pos, Direction.UP, context);
        int down = paneMask(world, state, pos, Direction.DOWN, context);
        boolean splitMin = hasMinBoundary(up, alongX) || hasMinBoundary(down, alongX);
        boolean splitMax = hasMaxBoundary(up, alongX) || hasMaxBoundary(down, alongX);
        if (!splitMin && !splitMax) return null;

        BakedQuadView view = BakedQuadView.of(quad);
        float min = alongX ? minX(view) : minZ(view);
        float max = alongX ? maxX(view) : maxZ(view);
        splitMin &= min < CENTER_MIN && CENTER_MIN < max;
        splitMax &= min < CENTER_MAX && CENTER_MAX < max;
        if (!splitMin && !splitMax) return null;
        int variant = (splitMin ? 1 : 0) | (splitMax ? 2 : 0);
        List<BakedQuad> cached = context.paneParts(quad, variant);
        if (cached != null) return cached;

        int[] vertices = quad.getVertices();
        UvTransform transform = UvTransform.of(vertices, vertices.length / 4, alongX);
        if (transform == null) return null;
        List<BakedQuad> result = new ObjectArrayList<>(3);
        float start = min;
        if (splitMin) {
            result.add(clip(quad, transform, alongX, start, CENTER_MIN));
            start = CENTER_MIN;
        }
        if (splitMax) {
            result.add(clip(quad, transform, alongX, start, CENTER_MAX));
            start = CENTER_MAX;
        }
        result.add(clip(quad, transform, alongX, start, max));
        result = List.copyOf(result);
        context.putPaneParts(quad, variant, result);
        return result;
    }

    private static int paneMask(WorldView world, BlockState state, BlockPos pos,
            Direction face, CtmRenderContext context) {
        int mask = context.paneMask(face);
        if (mask == CtmRenderContext.UNKNOWN_PANE_MASK) {
            BlockPos neighborPos = context.offset(pos, face);
            mask = loadPaneMask(world, state, neighborPos);
            context.paneMask(face, mask);
        }
        return mask;
    }

    private static int loadPaneMask(WorldView world, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        BlockState neighbor = world.getBlockState(pos);
        if (neighbor.getBlock() != block || block == Blocks.STAINED_GLASS_PANE
                && neighbor.get(StainedGlassPaneBlock.COLOR) != state.get(StainedGlassPaneBlock.COLOR)) {
            return -1;
        }
        neighbor = block.resolveVirtualProperties(neighbor, world, pos);
        int mask = (neighbor.get(PaneBlock.WEST) ? 1 : 0)
                | (neighbor.get(PaneBlock.EAST) ? 2 : 0)
                | (neighbor.get(PaneBlock.NORTH) ? 4 : 0)
                | (neighbor.get(PaneBlock.SOUTH) ? 8 : 0);
        return mask == 0 ? 15 : mask;
    }

    private static int axisMask(int mask, boolean alongX) {
        return (alongX ? mask : mask >> 2) & 3;
    }

    private static boolean hasMinBoundary(int mask, boolean alongX) {
        return mask >= 0 && (axisMask(mask, alongX) & 1) == 0;
    }

    private static boolean hasMaxBoundary(int mask, boolean alongX) {
        return mask >= 0 && (axisMask(mask, alongX) & 2) == 0;
    }

    private static int visibleParts(float min, float max, float coveredMin, float coveredMax) {
        if (coveredMax <= min + EPSILON || coveredMin >= max - EPSILON) return -1;
        return (min < coveredMin - EPSILON ? 1 : 0)
                | (max > coveredMax + EPSILON ? 2 : 0);
    }

    private static float coveredMin(boolean negative) {
        return negative ? 0 : CENTER_MIN;
    }

    private static float coveredMax(boolean positive) {
        return positive ? 1 : CENTER_MAX;
    }

    private static BakedQuad clip(BakedQuad quad, UvTransform transform, boolean alongX,
            float min, float max) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float coordinate = alongX ? x : z;
            float clipped = Math.clamp(coordinate, min, max);
            float delta = clipped - coordinate;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);
            vertices[offset + (alongX ? 0 : 2)] = Float.floatToRawIntBits(clipped);
            vertices[offset + 4] = Float.floatToRawIntBits(u + transform.u * delta);
            vertices[offset + 5] = Float.floatToRawIntBits(v + transform.v * delta);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getFace());
    }

    private static float minX(BakedQuadView quad) {
        return Math.min(Math.min(quad.getX(0), quad.getX(1)), Math.min(quad.getX(2), quad.getX(3)));
    }

    private static float maxX(BakedQuadView quad) {
        return Math.max(Math.max(quad.getX(0), quad.getX(1)), Math.max(quad.getX(2), quad.getX(3)));
    }

    private static float minY(BakedQuadView quad) {
        return Math.min(Math.min(quad.getY(0), quad.getY(1)), Math.min(quad.getY(2), quad.getY(3)));
    }

    private static float maxY(BakedQuadView quad) {
        return Math.max(Math.max(quad.getY(0), quad.getY(1)), Math.max(quad.getY(2), quad.getY(3)));
    }

    private static float minZ(BakedQuadView quad) {
        return Math.min(Math.min(quad.getZ(0), quad.getZ(1)), Math.min(quad.getZ(2), quad.getZ(3)));
    }

    private static float maxZ(BakedQuadView quad) {
        return Math.max(Math.max(quad.getZ(0), quad.getZ(1)), Math.max(quad.getZ(2), quad.getZ(3)));
    }

    private record Range(float min, float max) {
    }

    private record UvTransform(float u, float v) {
        private static UvTransform of(int[] data, int stride, boolean alongX) {
            int coordinateOffset = alongX ? 0 : 2;
            for (int first = 0; first < 3; first++) {
                int firstOffset = first * stride;
                for (int second = first + 1; second < 4; second++) {
                    int secondOffset = second * stride;
                    float delta = Float.intBitsToFloat(data[secondOffset + coordinateOffset])
                            - Float.intBitsToFloat(data[firstOffset + coordinateOffset]);
                    if (Math.abs(delta) < EPSILON) continue;
                    boolean edge = true;
                    for (int coordinate = 0; coordinate < 3; coordinate++) {
                        if (coordinate == coordinateOffset) continue;
                        if (Math.abs(Float.intBitsToFloat(data[secondOffset + coordinate])
                                - Float.intBitsToFloat(data[firstOffset + coordinate])) >= EPSILON) {
                            edge = false;
                            break;
                        }
                    }
                    if (!edge) continue;
                    return new UvTransform(
                            (Float.intBitsToFloat(data[secondOffset + 4])
                                    - Float.intBitsToFloat(data[firstOffset + 4])) / delta,
                            (Float.intBitsToFloat(data[secondOffset + 5])
                                    - Float.intBitsToFloat(data[firstOffset + 5])) / delta);
                }
            }
            return null;
        }
    }
}
