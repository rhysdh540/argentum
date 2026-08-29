package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
public class ItemDurabilityMixin {
    // The colored durability bar is the 3rd fill(); its green arg is the 0-255 damage value OptiFine keys on.
    @WrapOperation(method = "renderGuiItemDecorations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/ItemRenderer;fill(Lnet/minecraft/client/render/vertex/BufferBuilder;IIIIIIII)V",
            ordinal = 2))
    private void cera$durabilityColor(ItemRenderer self, BufferBuilder buffer, int x1, int y1, int x2, int y2,
            int r, int g, int b, int a, Operation<Void> original) {
        int color = Minecraft.getInstance().cera$getCustomColors().getDurabilityColor(g, -1);
        if (color >= 0) {
            original.call(self, buffer, x1, y1, x2, y2, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, a);
        } else {
            original.call(self, buffer, x1, y1, x2, y2, r, g, b, a);
        }
    }
}
