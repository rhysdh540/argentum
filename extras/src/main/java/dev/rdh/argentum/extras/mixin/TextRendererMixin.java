package dev.rdh.argentum.extras.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.rdh.argentum.extras.ArgentumExtras;

import net.minecraft.client.render.TextRenderer;

@Mixin(TextRenderer.class)
public class TextRendererMixin {
	@Definition(id = "shadow", local = @Local(type = boolean.class, argsOnly = true))
	@Expression("shadow")
	@ModifyExpressionValue(method = "draw(Ljava/lang/String;FFIZ)I", at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean argentumExtras$disableTextShadows(boolean original) {
		return original && !ArgentumExtras.CONFIG.disableTextShadows;
	}
}
