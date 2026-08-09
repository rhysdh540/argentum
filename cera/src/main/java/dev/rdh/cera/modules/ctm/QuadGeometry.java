package dev.rdh.cera.modules.ctm;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.Direction;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags.IS_ALIGNED;
import static org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags.IS_PARTIAL;

final class QuadGeometry {
    private static final float EPSILON = 1.0E-6F;

    final Direction face;
    final float minX;
    final float minY;
    final float minZ;
    final float maxX;
    final float maxY;
    final float maxZ;
    final UvSlope xUv;
    final UvSlope zUv;
    final PositionTransform transform;
    private final int flags;
    private final float uX;
    private final float uY;
    private final float uZ;

    private QuadGeometry(BakedQuad quad, TextureAtlasSprite sprite) {
        BakedQuadView view = BakedQuadView.of(quad);
        this.face = quad.getFace();
        this.flags = view.getFlags();
        this.minX = min(view.getX(0), view.getX(1), view.getX(2), view.getX(3));
        this.minY = min(view.getY(0), view.getY(1), view.getY(2), view.getY(3));
        this.minZ = min(view.getZ(0), view.getZ(1), view.getZ(2), view.getZ(3));
        this.maxX = max(view.getX(0), view.getX(1), view.getX(2), view.getX(3));
        this.maxY = max(view.getY(0), view.getY(1), view.getY(2), view.getY(3));
        this.maxZ = max(view.getZ(0), view.getZ(1), view.getZ(2), view.getZ(3));

        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        this.xUv = uvSlope(vertices, stride, 0);
        this.zUv = uvSlope(vertices, stride, 2);
        this.transform = positionTransform(vertices, stride, sprite);

        float xSum = 0;
        float ySum = 0;
        float zSum = 0;
        float uSum = 0;
        float xuSum = 0;
        float yuSum = 0;
        float zuSum = 0;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            xSum += x;
            ySum += y;
            zSum += z;
            uSum += u;
            xuSum += x * u;
            yuSum += y * u;
            zuSum += z * u;
        }
        this.uX = 4 * xuSum - xSum * uSum;
        this.uY = 4 * yuSum - ySum * uSum;
        this.uZ = 4 * zuSum - zSum * uSum;
    }

    static QuadGeometry of(BakedQuad quad, TextureAtlasSprite sprite) {
        return new QuadGeometry(quad, sprite);
    }

    boolean aligned() {
        return (this.flags & IS_ALIGNED) != 0;
    }

    boolean partial() {
        return (this.flags & IS_PARTIAL) != 0;
    }

    boolean mirrored(Direction left) {
        float direction = switch (left.getAxis()) {
            case X -> this.uX;
            case Y -> this.uY;
            case Z -> this.uZ;
        };
        return (left.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -direction : direction) > 0;
    }

    UvSlope uv(Direction.Axis axis) {
        return axis == Direction.Axis.X ? this.xUv : axis == Direction.Axis.Z ? this.zUv : null;
    }

    private static UvSlope uvSlope(int[] data, int stride, int coordinateOffset) {
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
                if (edge) {
                    return new UvSlope(
                            (Float.intBitsToFloat(data[secondOffset + 4])
                                    - Float.intBitsToFloat(data[firstOffset + 4])) / delta,
                            (Float.intBitsToFloat(data[secondOffset + 5])
                                    - Float.intBitsToFloat(data[firstOffset + 5])) / delta);
                }
            }
        }
        return null;
    }

    private static PositionTransform positionTransform(int[] data, int stride,
            TextureAtlasSprite sprite) {
        if (sprite == null) return null;
        int first = stride;
        int second = stride * 3;
        float u = local(Float.intBitsToFloat(data[4]), sprite.getUMin(), sprite.getUMax());
        float v = local(Float.intBitsToFloat(data[5]), sprite.getVMin(), sprite.getVMax());
        float firstU = local(Float.intBitsToFloat(data[first + 4]),
                sprite.getUMin(), sprite.getUMax()) - u;
        float firstV = local(Float.intBitsToFloat(data[first + 5]),
                sprite.getVMin(), sprite.getVMax()) - v;
        float secondU = local(Float.intBitsToFloat(data[second + 4]),
                sprite.getUMin(), sprite.getUMax()) - u;
        float secondV = local(Float.intBitsToFloat(data[second + 5]),
                sprite.getVMin(), sprite.getVMax()) - v;
        float determinant = firstU * secondV - secondU * firstV;
        if (Math.abs(determinant) < EPSILON) return null;

        float firstX = Float.intBitsToFloat(data[first]) - Float.intBitsToFloat(data[0]);
        float secondX = Float.intBitsToFloat(data[second]) - Float.intBitsToFloat(data[0]);
        float firstY = Float.intBitsToFloat(data[first + 1]) - Float.intBitsToFloat(data[1]);
        float secondY = Float.intBitsToFloat(data[second + 1]) - Float.intBitsToFloat(data[1]);
        float firstZ = Float.intBitsToFloat(data[first + 2]) - Float.intBitsToFloat(data[2]);
        float secondZ = Float.intBitsToFloat(data[second + 2]) - Float.intBitsToFloat(data[2]);
        return new PositionTransform(
                (firstX * secondV - secondX * firstV) / determinant,
                (secondX * firstU - firstX * secondU) / determinant,
                (firstY * secondV - secondY * firstV) / determinant,
                (secondY * firstU - firstY * secondU) / determinant,
                (firstZ * secondV - secondZ * firstV) / determinant,
                (secondZ * firstU - firstZ * secondU) / determinant
        );
    }

    private static float local(float value, float min, float max) {
        return (value - min) / (max - min) * 16;
    }

    private static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    record UvSlope(float u, float v) {
    }

    record PositionTransform(float xu, float xv, float yu, float yv, float zu, float zv) {
    }

    static final class Registry {
        private final Map<BakedQuad, QuadGeometry> geometries = new Reference2ReferenceOpenHashMap<>();
        private final Map<BakedQuad, QuadGeometry> fallback = new ConcurrentHashMap<>();

        void compile(BakedModel model) {
            compile(model.getQuads());
            for (Direction face : Direction.values()) compile(model.getQuads(face));
        }

        void compile(Iterable<BakedQuad> quads) {
            for (BakedQuad quad : quads) add(quad);
        }

        QuadGeometry add(BakedQuad quad) {
            return this.geometries.computeIfAbsent(quad, Registry::create);
        }

        QuadGeometry get(BakedQuad quad) {
            QuadGeometry geometry = this.geometries.get(quad);
            return geometry != null ? geometry
                    : this.fallback.computeIfAbsent(quad, Registry::create);
        }

        void validateCompiled() {
            if (this.geometries.isEmpty()) throw new IllegalStateException("No quad geometry was compiled");
        }

        private static QuadGeometry create(BakedQuad quad) {
            return new QuadGeometry(quad, (TextureAtlasSprite) BakedQuadView.of(quad).celeritas$getSprite());
        }
    }
}
