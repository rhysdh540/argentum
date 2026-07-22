package org.taumc.celeritas.impl.render.entity.instancing;

import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;

import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

final class BakedItemGeometryCache {
    private static final int MULTIPLE_TINTS = Integer.MIN_VALUE;

    private final Map<BakedModel, Int2ObjectOpenHashMap<EntityGeometry>> itemGeometries = new IdentityHashMap<>();
    private final Map<BakedModel, Int2ObjectOpenHashMap<EntityGeometry>> fixedGeometries = new IdentityHashMap<>();
    private final Map<BakedModel, Int2ObjectOpenHashMap<EntityGeometry>> blockGeometries = new IdentityHashMap<>();

    EntityGeometry getItem(BakedModel model, ItemStack item) {
        if (model.isCustomRenderer()) {
            return null;
        }
        int tint = getTint(model, item);
        if (tint == MULTIPLE_TINTS) {
            return null;
        }
        return this.itemGeometries.computeIfAbsent(model, ignored -> new Int2ObjectOpenHashMap<>())
                .computeIfAbsent(tint, ignored -> new BakedItemGeometry(model, tint, false));
    }

    boolean supportsItem(BakedModel model, ItemStack item) {
        return this.getItem(model, item) != null;
    }

    EntityGeometry getFixed(BakedModel model, int color) {
        if (model.isCustomRenderer()) {
            return null;
        }
        return this.fixedGeometries.computeIfAbsent(model, ignored -> new Int2ObjectOpenHashMap<>())
                .computeIfAbsent(color, ignored -> new BakedItemGeometry(model, color, true));
    }

    EntityGeometry getBlock(BakedModel model, float brightness, float red, float green, float blue) {
        if (model.isCustomRenderer()) {
            return null;
        }
        int color = toByte(brightness) << 24 | toByte(red) << 16 | toByte(green) << 8 | toByte(blue);
        return this.blockGeometries.computeIfAbsent(model, ignored -> new Int2ObjectOpenHashMap<>())
                .computeIfAbsent(color, ignored -> new BakedItemGeometry(
                        model, brightness, red, green, blue, true));
    }

    private static int toByte(float value) {
        return Math.min(255, Math.max(0, (int)(value * 255.0F + 0.5F)));
    }

    private static int getTint(BakedModel model, ItemStack item) {
        int tint = -1;
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : model.getQuads(direction)) {
                tint = getTint(quad, item, tint);
                if (tint == MULTIPLE_TINTS) return tint;
            }
        }
        for (BakedQuad quad : model.getQuads()) {
            tint = getTint(quad, item, tint);
            if (tint == MULTIPLE_TINTS) return tint;
        }
        return tint;
    }

    private static int getTint(BakedQuad quad, ItemStack item, int tint) {
        if (!quad.hasTint()) {
            return tint;
        }
        int color = item.getItem().getDisplayColor(item, quad.getTintIndex()) | 0xFF000000;
        return tint == -1 || tint == color ? color : MULTIPLE_TINTS;
    }

    private static final class BakedItemGeometry implements EntityGeometry {
        private static final int VERTEX_STRIDE = 12 * Float.BYTES;
        private static final int INSTANCE_STRIDE = 28 * Float.BYTES;

        private final int vertexBuffer;
        private final int instanceBuffer;
        private final int vertexCount;

        private BakedItemGeometry(BakedModel model, float brightness, float red, float green, float blue,
                boolean block) {
            this(model, brightness, red, green, blue, block, -1, false);
        }

        private BakedItemGeometry(BakedModel model, int color, boolean fixed) {
            this(model, 1.0F, 1.0F, 1.0F, 1.0F, false, color, fixed);
        }

        private BakedItemGeometry(BakedModel model, float brightness, float red, float green, float blue,
                boolean block, int itemColor, boolean fixed) {
            int quads = model.getQuads().size();
            for (Direction direction : Direction.values()) {
                quads += model.getQuads(direction).size();
            }
            this.vertexCount = quads * 4;
            FloatBuffer vertices = BufferUtils.createFloatBuffer(this.vertexCount * 12);
            for (Direction direction : Direction.values()) {
                putQuads(vertices, model.getQuads(direction), brightness, red, green, blue, block, itemColor, fixed);
            }
            putQuads(vertices, model.getQuads(), brightness, red, green, blue, block, itemColor, fixed);
            vertices.flip();
            this.vertexBuffer = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBuffer);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW);
            this.instanceBuffer = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        }

        @Override
        public void render(Instances instances) {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vertexBuffer);
            attribute(0, 3, VERTEX_STRIDE, 0);
            attribute(1, 2, VERTEX_STRIDE, 3L * Float.BYTES);
            attribute(2, 3, VERTEX_STRIDE, 5L * Float.BYTES);
            attribute(9, 4, VERTEX_STRIDE, 8L * Float.BYTES);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.instanceBuffer);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, instances.upload(), GL15C.GL_STREAM_DRAW);
            attribute(3, 4, INSTANCE_STRIDE, 0);
            attribute(4, 4, INSTANCE_STRIDE, 4L * Float.BYTES);
            attribute(5, 4, INSTANCE_STRIDE, 8L * Float.BYTES);
            attribute(6, 4, INSTANCE_STRIDE, 12L * Float.BYTES);
            attribute(7, 3, INSTANCE_STRIDE, 16L * Float.BYTES);
            attribute(8, 4, INSTANCE_STRIDE, 19L * Float.BYTES);
            attribute(10, 1, INSTANCE_STRIDE, 23L * Float.BYTES);
            attribute(11, 4, INSTANCE_STRIDE, 24L * Float.BYTES);
            for (int i = 3; i < 9; i++) {
                ARBInstancedArrays.glVertexAttribDivisorARB(i, 1);
            }
            ARBInstancedArrays.glVertexAttribDivisorARB(10, 1);
            ARBInstancedArrays.glVertexAttribDivisorARB(11, 1);
            ARBDrawInstanced.glDrawArraysInstancedARB(GL11.GL_QUADS, 0, this.vertexCount, instances.count());
        }

        private static void putQuads(FloatBuffer output, Iterable<BakedQuad> quads, float brightness,
                float red, float green, float blue, boolean block, int itemColor, boolean fixed) {
            for (BakedQuad quad : quads) {
                BakedQuadView view = (BakedQuadView)quad;
                Vec3i normal = quad.getFace().getNormal();
                int color = fixed || quad.hasTint() ? itemColor : -1;
                float quadRed = block ? brightness * (quad.hasTint() ? red : 1.0F) : (color >> 16 & 0xFF) / 255.0F;
                float quadGreen = block ? brightness * (quad.hasTint() ? green : 1.0F) : (color >> 8 & 0xFF) / 255.0F;
                float quadBlue = block ? brightness * (quad.hasTint() ? blue : 1.0F) : (color & 0xFF) / 255.0F;
                float quadAlpha = block ? 1.0F : (color >>> 24) / 255.0F;
                for (int i = 0; i < 4; i++) {
                    output.put(view.getX(i)).put(view.getY(i)).put(view.getZ(i))
                            .put(view.getTexU(i)).put(view.getTexV(i))
                            .put(normal.getX()).put(normal.getY()).put(normal.getZ())
                            .put(quadRed).put(quadGreen).put(quadBlue).put(quadAlpha);
                }
            }
        }

        private static void attribute(int index, int size, int stride, long offset) {
            GL20C.glEnableVertexAttribArray(index);
            GL20C.glVertexAttribPointer(index, size, GL11.GL_FLOAT, false, stride, offset);
        }
    }
}
