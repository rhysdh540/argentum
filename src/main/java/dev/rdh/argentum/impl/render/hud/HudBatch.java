package dev.rdh.argentum.impl.render.hud;

import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import org.lwjgl.opengl.GL11;

public final class HudBatch {
    private static final BufferUploader UPLOADER = new BufferUploader();

    private HudBatch() {
    }

    public static Colored colored(int capacityBytes) {
        return new Colored(capacityBytes);
    }

    public static Textured textured(int capacityBytes) {
        return new Textured(capacityBytes);
    }

    public static final class Colored implements Runnable {
        private final BufferBuilder buffer;
        private boolean drawing;

        private Colored(int capacityBytes) {
            this.buffer = new BufferBuilder(capacityBytes / Integer.BYTES);
        }

        public void fill(int left, int top, int right, int bottom, int color) {
            if (!this.drawing) {
                this.buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
                this.drawing = true;
            }

            int x1 = Math.max(left, right);
            int x2 = Math.min(left, right);
            int y1 = Math.max(top, bottom);
            int y2 = Math.min(top, bottom);
            float alpha = (color >>> 24) / 255.0F;
            float red = (color >> 16 & 255) / 255.0F;
            float green = (color >> 8 & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;

            this.buffer.vertex(x1, y2, 0).color(red, green, blue, alpha).nextVertex();
            this.buffer.vertex(x2, y2, 0).color(red, green, blue, alpha).nextVertex();
            this.buffer.vertex(x2, y1, 0).color(red, green, blue, alpha).nextVertex();
            this.buffer.vertex(x1, y1, 0).color(red, green, blue, alpha).nextVertex();
        }

        public void draw() {
            if (!this.drawing) {
                return;
            }

            this.buffer.end();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture();
            GlStateManager.blendFuncSeparate(770, 771, 1, 0);
            UPLOADER.end(this.buffer);
            GlStateManager.enableTexture();
            GlStateManager.disableBlend();
            this.drawing = false;
        }

        @Override
        public void run() {
            this.draw();
        }
    }

    public static final class Textured {
        private final BufferBuilder buffer;
        private boolean drawing;

        private Textured(int capacityBytes) {
            this.buffer = new BufferBuilder(capacityBytes / Integer.BYTES);
        }

        public boolean isEmpty() {
            return !this.drawing;
        }

        public void quad(float x, float y, float u, float v, int sourceWidth, int sourceHeight,
                int width, int height, float textureWidth, float textureHeight, float z) {
            if (!this.drawing) {
                this.buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX);
                this.drawing = true;
            }

            float u0 = u / textureWidth;
            float v0 = v / textureHeight;
            float u1 = (u + sourceWidth) / textureWidth;
            float v1 = (v + sourceHeight) / textureHeight;
            this.buffer.vertex(x, y + height, z).texture(u0, v1).nextVertex();
            this.buffer.vertex(x + width, y + height, z).texture(u1, v1).nextVertex();
            this.buffer.vertex(x + width, y, z).texture(u1, v0).nextVertex();
            this.buffer.vertex(x, y, z).texture(u0, v0).nextVertex();
        }

        public void draw() {
            if (!this.drawing) {
                return;
            }

            this.buffer.end();
            UPLOADER.end(this.buffer);
            this.drawing = false;
        }
    }
}
