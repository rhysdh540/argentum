package org.taumc.celeritas.mixin.core;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    @Shadow
    private int[] characterWidths;

    @Shadow
    private byte[] glyphSizes;

    @Shadow
    private Identifier fontLocation;

    @Shadow
    private TextureManager textureManager;

    @Shadow
    private float x;

    @Shadow
    private float y;

    @Shadow
    private Identifier getFontPage(int page) {
        return null;
    }

    @Unique
    private BufferBuilder celeritas$buffer;

    @Unique
    private BufferUploader celeritas$uploader;

    @Unique
    private Identifier celeritas$texture;

    @Unique
    private boolean celeritas$batching;

    @Unique
    private boolean celeritas$drawing;

    @Unique
    private float celeritas$red = 1.0F;

    @Unique
    private float celeritas$green = 1.0F;

    @Unique
    private float celeritas$blue = 1.0F;

    @Unique
    private float celeritas$alpha = 1.0F;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void celeritas$createBatch(GameOptions options, Identifier fontLocation, TextureManager textureManager,
            boolean unicode, CallbackInfo ci) {
        this.celeritas$buffer = new BufferBuilder(64 * 1024 / Integer.BYTES);
        this.celeritas$uploader = new BufferUploader();
    }

    @Inject(method = "drawLayer(Ljava/lang/String;Z)V", at = @At("HEAD"))
    private void celeritas$beginBatch(String text, boolean shadow, CallbackInfo ci) {
        this.celeritas$batching = true;
    }

    @Inject(method = "drawLayer(Ljava/lang/String;Z)V", at = @At("RETURN"))
    private void celeritas$endBatch(String text, boolean shadow, CallbackInfo ci) {
        this.celeritas$flush();
        this.celeritas$batching = false;
    }

    @Inject(method = "drawGlyph", at = @At("HEAD"))
    private void celeritas$flushBeforeCustomGlyph(char character, boolean italic,
            CallbackInfoReturnable<Float> cir) {
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

    @WrapOperation(
            method = {"drawLayer(Ljava/lang/String;FFIZ)I", "drawLayer(Ljava/lang/String;Z)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;color4f(FFFF)V")
    )
    private void celeritas$captureColor(float red, float green, float blue, float alpha, Operation<Void> original) {
        this.celeritas$red = red;
        this.celeritas$green = green;
        this.celeritas$blue = blue;
        this.celeritas$alpha = alpha;
        original.call(red, green, blue, alpha);
    }

    @WrapOperation(
            method = "drawLayer(Ljava/lang/String;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;disableTexture()V")
    )
    private void celeritas$flushBeforeDecoration(Operation<Void> original) {
        this.celeritas$flush();
        original.call();
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
        this.celeritas$uploader.end(this.celeritas$buffer);
        this.celeritas$drawing = false;
    }
}
