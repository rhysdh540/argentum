package org.taumc.celeritas.impl.render.cloud;

import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.VertexBuffer;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class CloudRenderer {
    private static final int BOTTOM = 0;
    private static final int TOP = 1;
    private static final int X_NEGATIVE = 2;
    private static final int X_POSITIVE = 3;
    private static final int Z_NEGATIVE = 4;
    private static final int Z_POSITIVE = 5;
    private static final float TEXEL = 1.0F / 256.0F;
    private static final float INSET = 1.0F / 1024.0F;

    private final VertexBuffer[] buffers = new VertexBuffer[6];
    private int cloudX = Integer.MIN_VALUE;
    private int cloudZ = Integer.MIN_VALUE;

    public void render(double cameraX, double cameraZ, float cloudY, Vec3d color, int pass) {
        int cellX = (int)Math.floor(cameraX);
        int cellZ = (int)Math.floor(cameraZ);
        if (cellX != this.cloudX || cellZ != this.cloudZ) {
            this.rebuild(cellX, cellZ);
        }

        float red = (float)color.x;
        float green = (float)color.y;
        float blue = (float)color.z;
        if (pass != 2) {
            float anaglyphRed = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
            float anaglyphGreen = (red * 30.0F + green * 70.0F) / 100.0F;
            float anaglyphBlue = (red * 30.0F + blue * 70.0F) / 100.0F;
            red = anaglyphRed;
            green = anaglyphGreen;
            blue = anaglyphBlue;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scalef(12.0F, 1.0F, 12.0F);
        GlStateManager.translatef((float)(cellX - cameraX), cloudY, (float)(cellZ - cameraZ));
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

        for (int depthPass = 0; depthPass < 2; depthPass++) {
            if (depthPass == 0) {
                GlStateManager.colorMask(false, false, false, false);
            } else if (pass == 0) {
                GlStateManager.colorMask(false, true, true, true);
            } else if (pass == 1) {
                GlStateManager.colorMask(true, false, false, true);
            } else {
                GlStateManager.colorMask(true, true, true, true);
            }

            if (cloudY > -5.0F) {
                this.draw(BOTTOM, red * 0.7F, green * 0.7F, blue * 0.7F);
            }
            if (cloudY <= 5.0F) {
                this.draw(TOP, red, green, blue);
            }
            this.draw(X_NEGATIVE, red * 0.9F, green * 0.9F, blue * 0.9F);
            this.draw(X_POSITIVE, red * 0.9F, green * 0.9F, blue * 0.9F);
            this.draw(Z_NEGATIVE, red * 0.8F, green * 0.8F, blue * 0.8F);
            this.draw(Z_POSITIVE, red * 0.8F, green * 0.8F, blue * 0.8F);
        }

        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.popMatrix();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void delete() {
        for (int i = 0; i < this.buffers.length; i++) {
            if (this.buffers[i] != null) {
                this.buffers[i].delete();
                this.buffers[i] = null;
            }
        }
        this.cloudX = Integer.MIN_VALUE;
        this.cloudZ = Integer.MIN_VALUE;
    }

    private void draw(int side, float red, float green, float blue) {
        GlStateManager.color4f(red, green, blue, 0.8F);
        this.buffers[side].bind();
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 20, 0L);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 20, 12L);
        this.buffers[side].draw(GL11.GL_QUADS);
        this.buffers[side].unbind();
    }

    private void rebuild(int cloudX, int cloudZ) {
        this.cloudX = cloudX;
        this.cloudZ = cloudZ;
        BufferBuilder builder = new BufferBuilder(8192);

        for (int side = 0; side < this.buffers.length; side++) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX);
            this.buildSide(builder, side, cloudX * TEXEL, cloudZ * TEXEL);
            builder.end();
            if (this.buffers[side] == null) {
                this.buffers[side] = new VertexBuffer(DefaultVertexFormat.POSITION_TEX);
            }
            this.buffers[side].upload(builder.getBuffer());
        }
    }

    private void buildSide(BufferBuilder builder, int side, float textureX, float textureZ) {
        for (int cellX = -3; cellX <= 4; cellX++) {
            for (int cellZ = -3; cellZ <= 4; cellZ++) {
                float x = cellX * 8.0F;
                float z = cellZ * 8.0F;
                if (side == BOTTOM || side == TOP) {
                    float y = side == BOTTOM ? 0.0F : 4.0F - INSET;
                    this.quad(builder,
                            x, y, z + 8.0F, x * TEXEL + textureX, (z + 8.0F) * TEXEL + textureZ,
                            x + 8.0F, y, z + 8.0F, (x + 8.0F) * TEXEL + textureX, (z + 8.0F) * TEXEL + textureZ,
                            x + 8.0F, y, z, (x + 8.0F) * TEXEL + textureX, z * TEXEL + textureZ,
                            x, y, z, x * TEXEL + textureX, z * TEXEL + textureZ);
                } else {
                    this.buildEdge(builder, side, cellX, cellZ, x, z, textureX, textureZ);
                }
            }
        }
    }

    private void buildEdge(BufferBuilder builder, int side, int cellX, int cellZ, float x, float z,
            float textureX, float textureZ) {
        if ((side == X_NEGATIVE && cellX <= -1) || (side == X_POSITIVE && cellX > 1)
                || (side == Z_NEGATIVE && cellZ <= -1) || (side == Z_POSITIVE && cellZ > 1)) {
            return;
        }

        for (int strip = 0; strip < 8; strip++) {
            if (side == X_NEGATIVE || side == X_POSITIVE) {
                float edgeX = x + strip + (side == X_POSITIVE ? 1.0F - INSET : 0.0F);
                float u = (x + strip + 0.5F) * TEXEL + textureX;
                this.quad(builder,
                        edgeX, 0.0F, z + 8.0F, u, (z + 8.0F) * TEXEL + textureZ,
                        edgeX, 4.0F, z + 8.0F, u, (z + 8.0F) * TEXEL + textureZ,
                        edgeX, 4.0F, z, u, z * TEXEL + textureZ,
                        edgeX, 0.0F, z, u, z * TEXEL + textureZ);
            } else {
                float edgeZ = z + strip + (side == Z_POSITIVE ? 1.0F - INSET : 0.0F);
                float v = (z + strip + 0.5F) * TEXEL + textureZ;
                this.quad(builder,
                        x, 4.0F, edgeZ, x * TEXEL + textureX, v,
                        x + 8.0F, 4.0F, edgeZ, (x + 8.0F) * TEXEL + textureX, v,
                        x + 8.0F, 0.0F, edgeZ, (x + 8.0F) * TEXEL + textureX, v,
                        x, 0.0F, edgeZ, x * TEXEL + textureX, v);
            }
        }
    }

    private void quad(BufferBuilder builder,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3) {
        builder.vertex(x0, y0, z0).texture(u0, v0).nextVertex();
        builder.vertex(x1, y1, z1).texture(u1, v1).nextVertex();
        builder.vertex(x2, y2, z2).texture(u2, v2).nextVertex();
        builder.vertex(x3, y3, z3).texture(u3, v3).nextVertex();
    }
}
