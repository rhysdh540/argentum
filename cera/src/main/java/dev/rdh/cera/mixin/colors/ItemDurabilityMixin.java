package dev.rdh.cera.mixin.colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemRenderer.class)
public class ItemDurabilityMixin {
    @ModifyArgs(method = "renderGuiItemDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/ItemRenderer;fill(Lnet/minecraft/client/render/vertex/BufferBuilder;IIIIIIII)V", ordinal = 2))
    private void cera$durabilityColor(Args args) {
        int g = args.get(6);
        int color = Minecraft.getInstance().cera$getCustomColors().getDurabilityColor(g, -1);
        if (color >= 0) {
            args.set(5, color >> 16 & 0xFF); // red
            args.set(6, color >> 8 & 0xFF); // green
            args.set(7, color & 0xFF); // blue
        }
    }
}
