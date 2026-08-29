package dev.rdh.cera.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.block.entity.SignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SignRenderer.class)
public class SignRendererMixin {
    @ModifyArg(method = "render(Lnet/minecraft/block/entity/SignBlockEntity;DDDFI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;draw(Ljava/lang/String;III)I"), index = 3)
    private int cera$signTextColor(int color) {
        return Minecraft.getInstance().cera$getCustomColors().getSignTextColor(color);
    }
}
