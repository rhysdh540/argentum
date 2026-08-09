package dev.rdh.cera.modules.ctm;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class PaneCulling {
    private static final float CENTER_MIN = 7 / 16F;
    private static final float CENTER_MAX = 9 / 16F;
    private static final float EPSILON = 1.0E-6F;

    private final Map<List<BakedQuad>, List<BakedQuad>> prepared = new Reference2ReferenceOpenHashMap<>();
    private final Map<BakedQuad, Parts> parts = new Reference2ReferenceOpenHashMap<>();

    static boolean supports(Block block) {
        return block == Blocks.GLASS_PANE || block == Blocks.STAINED_GLASS_PANE
                || block == Blocks.IRON_BARS;
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
        BakedQuad south = new BakedQuad(vertical, -1, Direction.SOUTH);
        BakedQuad north = new BakedQuad(vertical.clone(), -1, Direction.NORTH);
        QuadGeometry southGeometry = QuadGeometry.of(south, null);
        QuadGeometry northGeometry = QuadGeometry.of(north, null);
        QuadGeometry.UvSlope transform = southGeometry.xUv;
        Parts compiled = compileQuad(south, southGeometry);
        List<Range> disjoint = subtract(0, 1, new Range[] {
                new Range(0.2F, 0.3F), new Range(0.7F, 0.8F)
        });
        if (visibleParts(0, 1, CENTER_MIN, CENTER_MAX) != 3
                || visibleParts(0, CENTER_MAX, 0, CENTER_MAX) != 0
                || visibleParts(0, CENTER_MIN, CENTER_MIN, CENTER_MAX) != -1
                || !hasMinBoundary(0, true) || !hasMaxBoundary(0, true)
                || hasMinBoundary(3, true) || hasMaxBoundary(3, true)
                || transform == null || transform.u() != 16 || transform.v() != 0
                || QuadGeometry.of(new BakedQuad(new int[28], -1, Direction.SOUTH), null).xUv != null
                || !equal(1, 1 + EPSILON / 2)
                || compiled.side[1].size() != 2
                || compiled.side[2].size() != 2
                || compiled.side[3].size() != 3
                || !innerFaces(southGeometry, northGeometry)
                || !disjoint.equals(List.of(new Range(0, 0.2F), new Range(0.3F, 0.7F),
                        new Range(0.8F, 1)))
                || oppositeConnection(Direction.WEST) != 2
                || oppositeConnection(Direction.EAST) != 1
                || oppositeConnection(Direction.NORTH) != 8
                || oppositeConnection(Direction.SOUTH) != 4) {
            throw new IllegalStateException("Invalid partial pane culling");
        }
    }

    void compile(BakedModel model, QuadGeometry.Registry geometries) {
        compile(model.getQuads(), geometries);
        for (Direction face : Direction.values()) compile(model.getQuads(face), geometries);
    }

    void validateCompiled() {
        if (this.parts.isEmpty()) throw new IllegalStateException("No pane geometry was compiled");
    }

    List<BakedQuad> prepare(List<BakedQuad> quads) {
        return this.prepared.getOrDefault(quads, quads);
    }

    private void compile(List<BakedQuad> quads, QuadGeometry.Registry geometries) {
        if (!this.prepared.containsKey(quads)) {
            this.prepared.put(quads, compileInnerFaces(quads, geometries));
        }
    }

    private List<BakedQuad> compileInnerFaces(List<BakedQuad> quads,
            QuadGeometry.Registry geometries) {
        geometries.compile(quads);
        List<BakedQuad> prepared = cullInnerFaces(quads, geometries);
        for (BakedQuad quad : prepared) {
            this.parts.computeIfAbsent(quad, part -> {
                Parts compiled = compileQuad(part, geometries.add(part));
                compiled.register(geometries);
                return compiled;
            });
        }
        return prepared;
    }

    private static List<BakedQuad> cullInnerFaces(List<BakedQuad> quads,
            QuadGeometry.Registry geometries) {
        if (quads.size() < 2) return quads;

        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            QuadGeometry geometry = geometries.get(quad);
            Direction face = geometry.face;
            if (face.getAxis() == Direction.Axis.Y) {
                if (result != null) result.add(quad);
                continue;
            }

            boolean alongX = face.getAxis() == Direction.Axis.Z;
            float min = alongX ? geometry.minX : geometry.minZ;
            float max = alongX ? geometry.maxX : geometry.maxZ;
            List<Range> covered = null;
            for (int j = 0; j < quads.size(); j++) {
                if (i == j) continue;
                BakedQuad other = quads.get(j);
                QuadGeometry otherGeometry = geometries.get(other);
                if (!innerFaces(geometry, otherGeometry)) continue;
                float otherMin = alongX ? otherGeometry.minX : otherGeometry.minZ;
                float otherMax = alongX ? otherGeometry.maxX : otherGeometry.maxZ;
                if (covered == null) covered = new ObjectArrayList<>();
                covered.add(new Range(Math.max(min, otherMin), Math.min(max, otherMax)));
            }

            if (covered == null) {
                if (result != null) result.add(quad);
                continue;
            }
            List<Range> visible = subtract(min, max, covered.toArray(Range[]::new));
            if (result == null) {
                result = new ObjectArrayList<>(quads.size());
                result.addAll(quads.subList(0, i));
            }
            if (visible.isEmpty()) continue;
            QuadGeometry.UvSlope transform = geometry.uv(
                    alongX ? Direction.Axis.X : Direction.Axis.Z);
            if (transform == null) {
                result.add(quad);
                continue;
            }
            for (Range range : visible) {
                result.add(clip(quad, transform, alongX, range.min, range.max));
            }
        }
        return result == null ? quads : List.copyOf(result);
    }

    private static List<Range> subtract(float min, float max, Range[] covered) {
        Arrays.sort(covered, Comparator.comparingDouble(Range::min));
        List<Range> visible = new ObjectArrayList<>(covered.length + 1);
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

    private static boolean innerFaces(QuadGeometry first, QuadGeometry second) {
        Direction face = first.face;
        if (face.getOpposite() != second.face || face.getAxis() == Direction.Axis.Y) return false;
        return switch (face.getAxis()) {
            case X -> equal(first.minX, first.maxX)
                    && equal(first.minX, second.minX)
                    && equal(first.minY, second.minY)
                    && equal(first.maxY, second.maxY)
                    && Math.max(first.minZ, second.minZ) < Math.min(first.maxZ, second.maxZ);
            case Z -> equal(first.minZ, first.maxZ)
                    && equal(first.minZ, second.minZ)
                    && equal(first.minY, second.minY)
                    && equal(first.maxY, second.maxY)
                    && Math.max(first.minX, second.minX) < Math.min(first.maxX, second.maxX);
            default -> false;
        };
    }

    private static boolean equal(float first, float second) {
        return Math.abs(first - second) < EPSILON;
    }

    List<BakedQuad> cull(WorldView world, BlockState state, BlockPos pos,
            BakedQuad quad, QuadGeometry geometry, TextureAtlasSprite sprite,
            CtmRenderContext context) {
        Block block = state.getBlock();
        if (!supports(block)) return null;

        Direction face = geometry.face;
        if (face.getAxis() != Direction.Axis.Y) {
            if (sprite.getName().startsWith("minecraft:blocks/glass_pane_top")) {
                return world.getBlockState(context.offset(pos, face)) == state
                        ? List.of() : null;
            }
            Parts parts = this.parts.get(quad);
            if (parts == null) return null;
            int up = paneMask(world, state, pos, Direction.UP, context);
            int down = paneMask(world, state, pos, Direction.DOWN, context);
            boolean alongX = face.getAxis() == Direction.Axis.Z;
            int variant = (hasMinBoundary(up, alongX) || hasMinBoundary(down, alongX) ? 1 : 0)
                    | (hasMaxBoundary(up, alongX) || hasMaxBoundary(down, alongX) ? 2 : 0);
            return parts.side == null ? null : parts.side[variant];
        }

        int mask = paneMask(world, state, pos, face, context);
        if (mask < 0) return null;
        Parts parts = this.parts.get(quad);
        return parts == null || parts.cap == null ? null : parts.cap[axisMask(mask, parts.alongX)];
    }

    static boolean covers(WorldView world, BlockState state, BlockPos pos, Direction direction,
            QuadGeometry geometry, CtmRenderContext context) {
        if (direction.getAxis() != Direction.Axis.Y || geometry.face.getAxis() == Direction.Axis.Y) {
            return true;
        }
        int mask = paneMask(world, state, pos, direction, context);
        if (mask < 0) return true;
        boolean alongX = geometry.face.getAxis() == Direction.Axis.Z;
        float middle = alongX ? (geometry.minX + geometry.maxX) / 2
                : (geometry.minZ + geometry.maxZ) / 2;
        int axisMask = axisMask(mask, alongX);
        return middle >= coveredMin((axisMask & 1) != 0)
                && middle <= coveredMax((axisMask & 2) != 0);
    }

    static boolean coversCorner(WorldView world, BlockState state, BlockPos corner,
            Direction first, Direction second, QuadGeometry geometry) {
        if (geometry.face.getAxis() == Direction.Axis.Y) return true;
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

    @SuppressWarnings("unchecked")
    private static Parts compileQuad(BakedQuad quad, QuadGeometry geometry) {
        Direction face = geometry.face;
        if (face.getAxis() == Direction.Axis.Y) {
            if (!geometry.aligned()) return Parts.NONE;
            float lengthX = geometry.maxX - geometry.minX;
            float lengthZ = geometry.maxZ - geometry.minZ;
            List<BakedQuad>[] cap = new List[4];
            if (equal(lengthX, lengthZ)) {
                for (int variant = 0; variant < cap.length; variant++) cap[variant] = List.of();
                return new Parts(null, cap, false);
            }
            boolean alongX = lengthX > lengthZ;
            QuadGeometry.UvSlope transform = geometry.uv(
                    alongX ? Direction.Axis.X : Direction.Axis.Z);
            if (transform == null) return Parts.NONE;
            float min = alongX ? geometry.minX : geometry.minZ;
            float max = alongX ? geometry.maxX : geometry.maxZ;
            for (int variant = 0; variant < cap.length; variant++) {
                cap[variant] = clipCap(quad, transform, alongX, min, max, variant);
            }
            return new Parts(null, cap, alongX);
        }

        boolean alongX = face.getAxis() == Direction.Axis.Z;
        float min = alongX ? geometry.minX : geometry.minZ;
        float max = alongX ? geometry.maxX : geometry.maxZ;
        QuadGeometry.UvSlope transform = geometry.uv(
                alongX ? Direction.Axis.X : Direction.Axis.Z);
        if (transform == null) return Parts.NONE;
        List<BakedQuad>[] side = new List[4];
        for (int variant = 1; variant < side.length; variant++) {
            boolean splitMin = (variant & 1) != 0 && min < CENTER_MIN && CENTER_MIN < max;
            boolean splitMax = (variant & 2) != 0 && min < CENTER_MAX && CENTER_MAX < max;
            if (!splitMin && !splitMax) continue;
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
            side[variant] = List.copyOf(result);
        }
        return new Parts(side, null, false);
    }

    private static List<BakedQuad> clipCap(BakedQuad quad, QuadGeometry.UvSlope transform,
            boolean alongX, float min, float max, int variant) {
        float coveredMin = coveredMin((variant & 1) != 0);
        float coveredMax = coveredMax((variant & 2) != 0);
        int parts = visibleParts(min, max, coveredMin, coveredMax);
        if (parts < 0) return null;
        if (parts == 0) return List.of();
        BakedQuad first = (parts & 1) != 0
                ? clip(quad, transform, alongX, min, Math.min(max, coveredMin))
                : clip(quad, transform, alongX, Math.max(min, coveredMax), max);
        return parts == 3
                ? List.of(first, clip(quad, transform, alongX, Math.max(min, coveredMax), max))
                : List.of(first);
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

    private static BakedQuad clip(BakedQuad quad, QuadGeometry.UvSlope transform, boolean alongX,
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
            vertices[offset + 4] = Float.floatToRawIntBits(u + transform.u() * delta);
            vertices[offset + 5] = Float.floatToRawIntBits(v + transform.v() * delta);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getFace());
    }

    private record Range(float min, float max) {
    }

    private record Parts(List<BakedQuad>[] side, List<BakedQuad>[] cap, boolean alongX) {
        private static final Parts NONE = new Parts(null, null, false);

        private void register(QuadGeometry.Registry geometries) {
            register(this.side, geometries);
            register(this.cap, geometries);
        }

        private static void register(List<BakedQuad>[] variants,
                QuadGeometry.Registry geometries) {
            if (variants == null) return;
            for (List<BakedQuad> quads : variants) {
                if (quads != null) geometries.compile(quads);
            }
        }
    }
}
