package dev.rdh.argentum.mixin.features.text;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rdh.argentum.impl.ext.SignTextCache;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.entity.SignRenderer;
import net.minecraft.text.Text;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SignRenderer.class)
public class SignRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDFI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;depthMask(Z)V",
                    ordinal = 0, shift = At.Shift.AFTER)
    )
    private void argentum$beginSignText(SignBlockEntity sign, double x, double y, double z, float tickDelta,
                                        int breakProgress, CallbackInfo ci, @Local TextRenderer textRenderer,
                                        @Share("textRenderer") LocalRef<TextRenderer> bruh) {
        textRenderer.argentum$beginBatch(() -> {});
        bruh.set(textRenderer); // mixin actually pmo
    }

    @Inject(
            method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDFI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;depthMask(Z)V",
                    ordinal = 1)
    )
    private void argentum$endSignText(SignBlockEntity sign, double x, double y, double z, float tickDelta,
                                      int breakProgress, CallbackInfo ci,
                                      @Share("textRenderer") LocalRef<TextRenderer> bruh) {
        bruh.get().argentum$endBatch();
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDFI)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/TextRenderUtils;wrapText(Lnet/minecraft/text/Text;ILnet/minecraft/client/render/TextRenderer;ZZ)Ljava/util/List;")
    )
    private List<Text> argentum$cacheWrappedLine(Text line, int width, TextRenderer textRenderer,
            boolean stripLeadingSpaces, boolean allowFormatting, Operation<List<Text>> original,
            @Local(argsOnly = true) SignBlockEntity sign) {
        SignTextCache cache = (SignTextCache)sign;
        boolean unicode = textRenderer.getUnicode();
        List<Text> wrapped = cache.argentum$getWrappedLine(line, unicode);
        if (wrapped == null) {
            wrapped = original.call(line, width, textRenderer, stripLeadingSpaces, allowFormatting);
            cache.argentum$putWrappedLine(line, unicode, wrapped);
        }
        return wrapped;
    }
}
