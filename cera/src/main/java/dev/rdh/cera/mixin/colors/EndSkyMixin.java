package dev.rdh.cera.mixin.colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.world.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WorldRenderer.class)
public class EndSkyMixin {
    @ModifyArgs(method = "renderEndSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/BufferBuilder;color(IIII)Lnet/minecraft/client/render/vertex/BufferBuilder;"))
    private void cera$endSkyColor(Args args) {
        int color = Minecraft.getInstance().cera$getCustomColors().getEndSkyColor(-1);
        if (color >= 0) {
            args.set(0, color >> 16 & 0xFF);
            args.set(1, color >> 8 & 0xFF);
            args.set(2, color & 0xFF);
        }
    }
}
