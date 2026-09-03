package dev.rdh.argentum.mixin.features.text;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rdh.argentum.impl.ext.SignTextCache;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.entity.SignRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SignRenderer.class)
public class SignRendererMixin {
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
