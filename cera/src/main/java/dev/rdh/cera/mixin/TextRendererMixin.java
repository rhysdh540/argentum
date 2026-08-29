package dev.rdh.cera.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextRenderer.class)
public class TextRendererMixin {
    @Shadow
    private int[] colors;

    @Inject(method = "init", at = @At("TAIL"))
    private void cera$applyTextColors(CallbackInfo ci) {
        Minecraft.getInstance().cera$getCustomColors().applyTextColors(this.colors);
    }
}
