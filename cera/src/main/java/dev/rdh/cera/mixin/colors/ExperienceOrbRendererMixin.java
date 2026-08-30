package dev.rdh.cera.mixin.colors;

import com.llamalad7.mixinextras.sugar.Local;
import dev.rdh.cera.modules.colors.CustomColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.ExperienceOrbRenderer;
import net.minecraft.entity.ExperienceOrbEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ExperienceOrbRenderer.class)
public class ExperienceOrbRendererMixin {
    @ModifyArgs(method = "render(Lnet/minecraft/entity/ExperienceOrbEntity;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/BufferBuilder;color(IIII)Lnet/minecraft/client/render/vertex/BufferBuilder;"))
    private void cera$xpOrbColor(Args args, @Local(argsOnly = true) ExperienceOrbEntity orb, @Local(argsOnly = true, ordinal = 1) float partialTicks) {
        CustomColors colors = Minecraft.getInstance().cera$getCustomColors();
        float timer = (orb.renderTicks + partialTicks) / 2.0F;
        int time = colors.getXpOrbTime();
        if (time > 0) timer *= 628.0F / time;
        int color = colors.getXpOrbColor(timer, -1);
        if (color >= 0) {
            args.set(0, color >> 16 & 0xFF);
            args.set(1, color >> 8 & 0xFF);
            args.set(2, color & 0xFF);
        }
    }
}
