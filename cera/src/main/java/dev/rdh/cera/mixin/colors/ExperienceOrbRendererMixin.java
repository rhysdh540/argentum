package dev.rdh.cera.mixin.colors;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rdh.cera.modules.colors.CustomColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.ExperienceOrbRenderer;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.entity.ExperienceOrbEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExperienceOrbRenderer.class)
public class ExperienceOrbRendererMixin {
    @WrapOperation(method = "render(Lnet/minecraft/entity/ExperienceOrbEntity;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/BufferBuilder;color(IIII)Lnet/minecraft/client/render/vertex/BufferBuilder;"))
    private BufferBuilder cera$xpOrbColor(BufferBuilder buffer, int r, int g, int b, int a, Operation<BufferBuilder> original,
            @Local(argsOnly = true) ExperienceOrbEntity orb, @Local(argsOnly = true, ordinal = 1) float partialTicks) {
        CustomColors colors = Minecraft.getInstance().cera$getCustomColors();
        float timer = (orb.renderTicks + partialTicks) / 2.0F;
        int time = colors.getXpOrbTime();
        if (time > 0) timer *= 628.0F / time;
        int color = colors.getXpOrbColor(timer, -1);
        return color >= 0
                ? original.call(buffer, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, a)
                : original.call(buffer, r, g, b, a);
    }
}
