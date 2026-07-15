package org.taumc.celeritas.impl.render.entity;

import net.minecraft.client.render.model.Box;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.render.model.Polygon;
import net.minecraft.client.render.model.Vertex;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.Tesselator;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.mixin.features.model.BoxAccessor;
import org.taumc.celeritas.mixin.features.model.BufferBuilderAccessor;
import org.taumc.celeritas.mixin.features.model.PolygonAccessor;

public final class CpuModelBatch {
    private static final boolean DISABLED = Boolean.getBoolean("celeritas.disableCpuModelBatching");
    private static final int STACK_SIZE = 64;
    private static final Matrix4f[] matrices = new Matrix4f[STACK_SIZE];
    private static final Matrix3f normalMatrix = new Matrix3f();
    private static final Vector3f position = new Vector3f();
    private static final Vector3f normal = new Vector3f();

    private static BufferBuilder buffer;
    private static boolean active;
    private static boolean drawing;
    private static int depth;

    static {
        for (int i = 0; i < matrices.length; i++) {
            matrices[i] = new Matrix4f();
        }
    }

    private CpuModelBatch() {
    }

    public static boolean begin() {
        if (DISABLED || active) {
            return false;
        }

        buffer = Tesselator.getInstance().getBuffer();
        if (((BufferBuilderAccessor)buffer).celeritas$isBuilding()) {
            return false;
        }

        matrices[0].identity();
        depth = 0;
        drawing = false;
        active = true;
        return true;
    }

    public static void end() {
        try {
            flush();
        } finally {
            active = false;
            drawing = false;
        }
    }

    public static void flush() {
        if (!active || !drawing) {
            return;
        }

        drawing = false;
        Tesselator.getInstance().end();
    }

    public static boolean render(ModelPart part, float scale, boolean forceTransform) {
        if (!active) {
            return false;
        }

        if (forceTransform) {
            renderForced(part, scale);
        } else {
            render(part, scale);
        }
        return true;
    }

    public static void pushMatrix() {
        if (active) {
            matrices[++depth].set(matrices[depth - 1]);
        }
    }

    public static void popMatrix() {
        if (active && depth > 0) {
            depth--;
        }
    }

    public static void translate(float x, float y, float z) {
        if (active) {
            matrices[depth].translate(x, y, z);
        }
    }

    public static void rotate(float angle, float x, float y, float z) {
        if (!active) {
            return;
        }

        float length = (float)Math.sqrt(x * x + y * y + z * z);
        if (length != 0.0F) {
            matrices[depth].rotate((float)Math.toRadians(angle), x / length, y / length, z / length);
        }
    }

    public static void scale(float x, float y, float z) {
        if (active) {
            matrices[depth].scale(x, y, z);
        }
    }

    private static void render(ModelPart part, float scale) {
        if (part.invisible || !part.visible) {
            return;
        }

        pushMatrix();
        translate(part.translateX, part.translateY, part.translateZ);
        translate(part.x * scale, part.y * scale, part.z * scale);
        matrices[depth].rotateZ(part.rotationZ).rotateY(part.rotationY).rotateX(part.rotationX);
        emit(part, scale);

        if (part.children != null) {
            for (int i = 0; i < part.children.size(); i++) {
                render(part.children.get(i), scale);
            }
        }

        popMatrix();
    }

    private static void renderForced(ModelPart part, float scale) {
        if (part.invisible || !part.visible) {
            return;
        }

        pushMatrix();
        translate(part.x * scale, part.y * scale, part.z * scale);
        matrices[depth].rotateY(part.rotationY).rotateX(part.rotationX).rotateZ(part.rotationZ);
        emit(part, scale);
        popMatrix();
    }

    private static void emit(ModelPart part, float scale) {
        if (part.boxes.isEmpty()) {
            return;
        }

        if (!drawing) {
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.ENTITY);
            drawing = true;
        }

        Matrix4f matrix = matrices[depth];
        matrix.normal(normalMatrix);
        for (int i = 0; i < part.boxes.size(); i++) {
            Polygon[] faces = ((BoxAccessor)part.boxes.get(i)).celeritas$getFaces();
            for (int j = 0; j < faces.length; j++) {
                emit(matrix, faces[j], scale);
            }
        }
    }

    private static void emit(Matrix4f matrix, Polygon polygon, float scale) {
        Vertex[] vertices = polygon.vertices;
        float ax = (float)(vertices[0].pos.x - vertices[1].pos.x);
        float ay = (float)(vertices[0].pos.y - vertices[1].pos.y);
        float az = (float)(vertices[0].pos.z - vertices[1].pos.z);
        float bx = (float)(vertices[2].pos.x - vertices[1].pos.x);
        float by = (float)(vertices[2].pos.y - vertices[1].pos.y);
        float bz = (float)(vertices[2].pos.z - vertices[1].pos.z);
        normal.set(by * az - bz * ay, bz * ax - bx * az, bx * ay - by * ax).normalize();
        if (((PolygonAccessor)polygon).celeritas$isNormalFlipped()) {
            normal.negate();
        }
        normalMatrix.transform(normal).normalize();

        for (int i = 0; i < 4; i++) {
            Vertex vertex = vertices[i];
            matrix.transformPosition((float)vertex.pos.x * scale, (float)vertex.pos.y * scale,
                    (float)vertex.pos.z * scale, position);
            buffer.vertex(position.x, position.y, position.z)
                    .texture(vertex.u, vertex.v)
                    .normal(normal.x, normal.y, normal.z)
                    .nextVertex();
        }
    }
}
