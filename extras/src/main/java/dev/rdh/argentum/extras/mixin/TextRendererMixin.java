package dev.rdh.argentum.extras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Credits to moehreag for this!
@Mixin(TextRenderer.class)
public class TextRendererMixin {

	@Inject(
		method = "draw(Ljava/lang/String;FFIZ)I",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;reset()V")
	)
	private void argentumExtras$disableTextShadows(String text, float x, float y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir, @Local(argsOnly = true) LocalBooleanRef renderShadow
	) {
    if (ArgentumExtras.CONFIG.disableTextShadows) {
		  renderShadow.set(false);
    }
	}
}
