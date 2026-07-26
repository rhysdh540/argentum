package org.taumc.celeritas.mixin.features.font;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.render.vertex.VertexBuffer;
import net.minecraft.client.render.vertex.VertexFormat;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.taumc.celeritas.impl.Celeritas;
import org.taumc.celeritas.impl.debug.RenderMetrics;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    @Unique
    private static final int WIDTH_CACHE_SIZE = 2048;

    @Unique
    private static final int GEOMETRY_CACHE_SIZE = 1024;

    @Shadow
    private int[] characterWidths;

    @Shadow
    private byte[] glyphSizes;

    @Shadow
    @Final
    private Identifier fontLocation;

    @Shadow
    @Final
    private TextureManager textureManager;

    @Shadow
    private float x;

    @Shadow
    private float y;

    @Shadow
    private boolean unicode;

    @Shadow
    private Identifier getFontPage(int page) {
        return null;
    }

    @Unique
    private BufferBuilder celeritas$buffer;

    @Unique
    private BufferBuilder celeritas$decorationBuffer;

    @Unique
    private BufferUploader celeritas$uploader;

    @Unique
    private Identifier celeritas$texture;

    @Unique
    private boolean celeritas$batching;

    @Unique
    private boolean celeritas$drawing;

    @Unique
    private boolean celeritas$drawingDecorations;

    @Unique
    private float celeritas$red = 1.0F;

    @Unique
    private float celeritas$green = 1.0F;

    @Unique
    private float celeritas$blue = 1.0F;

    @Unique
    private float celeritas$alpha = 1.0F;

    @Unique
    private Map<String, Integer> celeritas$widthCache;

    @Unique
    private Map<GeometryKey, Geometry> celeritas$geometryCache;

    @Unique
    private final GeometryKey celeritas$lookupKey = new GeometryKey();

    @Unique
    private GeometryKey celeritas$pendingKey;

    @Unique
    private VertexBuffer celeritas$pendingBuffer;

    @Unique
    private float celeritas$originX;

    @Unique
    private float celeritas$originY;

    @Unique
    private RenderMetrics.Category celeritas$previousCategory;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void celeritas$createBatch(GameOptions options, Identifier fontLocation, TextureManager textureManager,
            boolean unicode, CallbackInfo ci) {
        this.celeritas$buffer = new BufferBuilder(64 * 1024 / Integer.BYTES);
        this.celeritas$decorationBuffer = new BufferBuilder(4 * 1024 / Integer.BYTES);
        this.celeritas$uploader = new BufferUploader();
        this.celeritas$widthCache = new HashMap<>(256);
        this.celeritas$geometryCache = new LinkedHashMap<>(256, 0.75F, true);
    }

    @Inject(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void celeritas$getCachedWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (text == null) {
            return;
        }

        Integer width = this.celeritas$widthCache.get(text);
        if (width != null) {
            cir.setReturnValue(width);
        }
    }

    @Inject(method = "getWidth(Ljava/lang/String;)I", at = @At("RETURN"))
    private void celeritas$cacheWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (text == null) {
            return;
        }

        this.celeritas$widthCache.put(text, cir.getReturnValue());
        if (this.celeritas$widthCache.size() > WIDTH_CACHE_SIZE) {
            this.celeritas$widthCache.clear();
        }
    }

    @Inject(method = {"reload", "setUnicode"}, at = @At("RETURN"))
    private void celeritas$clearCaches(CallbackInfo ci) {
        this.celeritas$widthCache.clear();
        this.celeritas$clearGeometryCache();
    }

    @Inject(method = "drawLayer(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void celeritas$beginBatch(String text, boolean shadow, CallbackInfo ci) {
        this.celeritas$previousCategory = RenderMetrics.setCategory(RenderMetrics.Category.TEXT);
        this.celeritas$batching = Celeritas.CONFIG.fontBatching;
        this.celeritas$pendingKey = null;
        this.celeritas$pendingBuffer = null;
        if (!this.celeritas$batching || !this.celeritas$canCache(text)) {
            return;
        }

        GeometryKey key = this.celeritas$lookupKey.set(text, shadow,
                Float.floatToIntBits(this.celeritas$red), Float.floatToIntBits(this.celeritas$green),
                Float.floatToIntBits(this.celeritas$blue), Float.floatToIntBits(this.celeritas$alpha));
        Geometry geometry = this.celeritas$geometryCache.get(key);
        if (geometry != null) {
            this.textureManager.bind(this.fontLocation);
            this.celeritas$draw(geometry.buffer(), this.x, this.y);
            this.x += geometry.advance();
            this.celeritas$batching = false;
            RenderMetrics.setCategory(this.celeritas$previousCategory);
            ci.cancel();
            return;
        }

        this.celeritas$pendingKey = new GeometryKey(key);
        this.celeritas$originX = this.x;
        this.celeritas$originY = this.y;
    }

    @Inject(method = "drawLayer(Ljava/lang/String;Z)V", at = @At("RETURN"))
    private void celeritas$endBatch(String text, boolean shadow, CallbackInfo ci) {
        this.celeritas$flush();
        this.celeritas$flushDecorations();
        if (this.celeritas$pendingBuffer != null) {
            this.celeritas$geometryCache.put(this.celeritas$pendingKey,
                    new Geometry(this.celeritas$pendingBuffer, this.x - this.celeritas$originX));
            if (this.celeritas$geometryCache.size() > GEOMETRY_CACHE_SIZE) {
                var iterator = this.celeritas$geometryCache.entrySet().iterator();
                iterator.next().getValue().buffer().delete();
                iterator.remove();
            }
        }
        this.celeritas$pendingKey = null;
        this.celeritas$pendingBuffer = null;
        this.celeritas$batching = false;
        RenderMetrics.setCategory(this.celeritas$previousCategory);
    }

    @Inject(method = "drawGlyph", at = @At("HEAD"))
    private void celeritas$flushBeforeCustomGlyph(char character, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (character == '\u011e') {
            this.celeritas$flush();
        }
    }

    @Inject(method = "drawBasicGlyph", at = @At("HEAD"), cancellable = true)
    private void celeritas$drawBasicGlyph(int character, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (!this.celeritas$batching) {
            return;
        }

        int textureX = character % 16 * 8;
        int textureY = character / 16 * 8;
        int slant = italic ? 1 : 0;
        int width = this.characterWidths[character];
        float right = width - 0.01F;

        this.celeritas$useTexture(this.fontLocation);
        this.celeritas$quad(
                this.x + slant, this.y, textureX / 128.0F, textureY / 128.0F,
                this.x - slant, this.y + 7.99F, textureX / 128.0F, (textureY + 7.99F) / 128.0F,
                this.x + right - 1.0F + slant, this.y, (textureX + right - 1.0F) / 128.0F, textureY / 128.0F,
                this.x + right - 1.0F - slant, this.y + 7.99F,
                (textureX + right - 1.0F) / 128.0F, (textureY + 7.99F) / 128.0F
        );
        cir.setReturnValue((float)width);
    }

    @Inject(method = "drawUnicodeGlyph", at = @At("HEAD"), cancellable = true)
    private void celeritas$drawUnicodeGlyph(char character, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (!this.celeritas$batching) {
            return;
        }
        if (this.glyphSizes[character] == 0) {
            cir.setReturnValue(0.0F);
            return;
        }

        int left = this.glyphSizes[character] >>> 4;
        int right = (this.glyphSizes[character] & 15) + 1;
        float textureX = character % 16 * 16 + left;
        float textureY = (character & 255) / 16 * 16;
        float width = right - left - 0.02F;
        float slant = italic ? 1.0F : 0.0F;

        this.celeritas$useTexture(this.getFontPage(character / 256));
        this.celeritas$quad(
                this.x + slant, this.y, textureX / 256.0F, textureY / 256.0F,
                this.x - slant, this.y + 7.99F, textureX / 256.0F, (textureY + 15.98F) / 256.0F,
                this.x + width / 2.0F + slant, this.y, (textureX + width) / 256.0F, textureY / 256.0F,
                this.x + width / 2.0F - slant, this.y + 7.99F,
                (textureX + width) / 256.0F, (textureY + 15.98F) / 256.0F
        );
        cir.setReturnValue((right - left) / 2.0F + 1.0F);
    }

    @WrapWithCondition(
            method = {"drawLayer(Ljava/lang/String;FFIZ)I", "drawLayer(Ljava/lang/String;Z)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;color4f(FFFF)V")
    )
    private boolean celeritas$captureColor(float red, float green, float blue, float alpha) {
        this.celeritas$red = red;
        this.celeritas$green = green;
        this.celeritas$blue = blue;
        this.celeritas$alpha = alpha;
        return true;
    }

    @WrapOperation(
            method = "drawLayer(Ljava/lang/String;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/Tesselator;getBuffer()Lnet/minecraft/client/render/vertex/BufferBuilder;"),
            require = 2
    )
    private BufferBuilder celeritas$getDecorationBuffer(Tesselator tesselator, Operation<BufferBuilder> original) {
        return this.celeritas$batching ? this.celeritas$decorationBuffer : original.call(tesselator);
    }

    @WrapWithCondition(
            method = "drawLayer(Ljava/lang/String;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/BufferBuilder;begin(ILnet/minecraft/client/render/vertex/VertexFormat;)V"),
            require = 2
    )
    private boolean celeritas$beginDecorations(BufferBuilder buffer, int mode, VertexFormat format) {
        if (this.celeritas$batching && !this.celeritas$drawingDecorations) {
            buffer.begin(mode, DefaultVertexFormat.POSITION_COLOR);
            this.celeritas$drawingDecorations = true;
        }
        return !this.celeritas$batching;
    }

    @WrapWithCondition(
            method = "drawLayer(Ljava/lang/String;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/BufferBuilder;nextVertex()V"),
            require = 8
    )
    private boolean celeritas$colorDecoration(BufferBuilder buffer) {
        if (this.celeritas$batching) {
            buffer.color(this.celeritas$red, this.celeritas$green, this.celeritas$blue, this.celeritas$alpha);
        }
        return true;
    }

    @WrapWithCondition(
            method = "drawLayer(Ljava/lang/String;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/Tesselator;end()V"),
            require = 2
    )
    private boolean celeritas$deferDecorations(Tesselator tesselator) {
        return !this.celeritas$batching;
    }

    @WrapWithCondition(
            method = "drawLayer(Ljava/lang/String;Z)V",
            at = {
                    @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;disableTexture()V"),
                    @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;enableTexture()V")
            },
            require = 4
    )
    private boolean celeritas$deferTextureState() {
        return !this.celeritas$batching;
    }

    @Unique
    private void celeritas$useTexture(Identifier texture) {
        if (this.celeritas$drawing && texture.equals(this.celeritas$texture)) {
            return;
        }

        this.celeritas$flush();
        this.textureManager.bind(texture);
        this.celeritas$texture = texture;
        this.celeritas$drawing = true;
        this.celeritas$buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    }

    @Unique
    private void celeritas$quad(float x0, float y0, float u0, float v0,
            float x1, float y1, float u1, float v1,
            float x2, float y2, float u2, float v2,
            float x3, float y3, float u3, float v3) {
        this.celeritas$vertex(x0, y0, u0, v0);
        this.celeritas$vertex(x1, y1, u1, v1);
        this.celeritas$vertex(x3, y3, u3, v3);
        this.celeritas$vertex(x2, y2, u2, v2);
    }

    @Unique
    private void celeritas$vertex(float x, float y, float u, float v) {
        if (this.celeritas$pendingKey != null) {
            x -= this.celeritas$originX;
            y -= this.celeritas$originY;
        }
        this.celeritas$buffer.vertex(x, y, 0.0D).texture(u, v)
                .color(this.celeritas$red, this.celeritas$green, this.celeritas$blue, this.celeritas$alpha)
                .nextVertex();
    }

    @Unique
    private void celeritas$flush() {
        if (!this.celeritas$drawing) {
            return;
        }

        this.celeritas$buffer.end();
        if (this.celeritas$pendingKey == null) {
            RenderMetrics.recordFontBatch();
            this.celeritas$uploader.end(this.celeritas$buffer);
        } else {
            this.celeritas$pendingBuffer = new VertexBuffer(DefaultVertexFormat.POSITION_TEX_COLOR);
            this.celeritas$pendingBuffer.upload(this.celeritas$buffer.getBuffer());
            this.celeritas$draw(this.celeritas$pendingBuffer, this.celeritas$originX, this.celeritas$originY);
        }
        this.celeritas$drawing = false;
    }

    @Unique
    private void celeritas$flushDecorations() {
        if (!this.celeritas$drawingDecorations) {
            return;
        }

        this.celeritas$decorationBuffer.end();
        GlStateManager.disableTexture();
        RenderMetrics.recordFontBatch();
        this.celeritas$uploader.end(this.celeritas$decorationBuffer);
        GlStateManager.enableTexture();
        this.celeritas$drawingDecorations = false;
    }

    @Unique
    private boolean celeritas$canCache(String text) {
        if (this.unicode || text.isEmpty()) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\u00a7' && i + 1 < text.length()) {
                char formatting = Character.toLowerCase(text.charAt(++i));
                if (formatting == 'k' || formatting == 'm' || formatting == 'n') {
                    return false;
                }
            } else if (character < 32 || character > 126) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private void celeritas$draw(VertexBuffer buffer, float x, float y) {
        GlStateManager.pushMatrix();
        GlStateManager.translatef(x, y, 0.0F);
        buffer.bind();
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 24, 0L);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 24, 12L);
        GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 24, 20L);
        RenderMetrics.recordFontBatch();
        buffer.draw(GL11.GL_QUADS);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        buffer.unbind();
        GlStateManager.popMatrix();
    }

    @Unique
    private void celeritas$clearGeometryCache() {
        for (Geometry geometry : this.celeritas$geometryCache.values()) {
            geometry.buffer().delete();
        }
        this.celeritas$geometryCache.clear();
    }

    @Unique
    private static final class GeometryKey {
        private String text;
        private boolean shadow;
        private int red;
        private int green;
        private int blue;
        private int alpha;

        private GeometryKey() {
        }

        private GeometryKey(GeometryKey key) {
            this.set(key.text, key.shadow, key.red, key.green, key.blue, key.alpha);
        }

        private GeometryKey set(String text, boolean shadow, int red, int green, int blue, int alpha) {
            this.text = text;
            this.shadow = shadow;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof GeometryKey key)) {
                return false;
            }
            return this.shadow == key.shadow
                    && this.red == key.red
                    && this.green == key.green
                    && this.blue == key.blue
                    && this.alpha == key.alpha
                    && this.text.equals(key.text);
        }

        @Override
        public int hashCode() {
            int hash = this.text.hashCode();
            hash = 31 * hash + Boolean.hashCode(this.shadow);
            hash = 31 * hash + this.red;
            hash = 31 * hash + this.green;
            hash = 31 * hash + this.blue;
            return 31 * hash + this.alpha;
        }
    }

    @Unique
    private record Geometry(VertexBuffer buffer, float advance) {
    }
}
