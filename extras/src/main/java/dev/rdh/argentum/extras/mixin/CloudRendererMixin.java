package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import dev.rdh.argentum.impl.render.cloud.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = CloudRenderer.class, remap = false)
public class CloudRendererMixin {
    @ModifyConstant(method = "render", constant = @Constant(doubleValue = 0.03D), remap = false)
    private double argentumExtras$changeCloudSpeed(double vanilla) {
        return vanilla * ArgentumExtras.CONFIG.cloudSpeed / 100.0D;
    }

    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true, remap = false)
    private float argentumExtras$changeCloudHeight(float cloudY) {
        return cloudY + ArgentumExtras.CONFIG.cloudHeightOffset;
    }

    @ModifyConstant(method = "buildSide", constant = @Constant(intValue = -3), remap = false)
    private int argentumExtras$cloudStart(int vanilla) {
        int distance = ArgentumExtras.CONFIG.cloudRenderDistance;
        return distance == 0 ? vanilla : 1 - distance / 96;
    }

    @ModifyConstant(method = "buildSide", constant = @Constant(intValue = 4), remap = false)
    private int argentumExtras$cloudEnd(int vanilla) {
        int distance = ArgentumExtras.CONFIG.cloudRenderDistance;
        return distance == 0 ? vanilla : distance / 96;
    }
}
