package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.taumc.celeritas.impl.render.cloud.CloudRenderer;

@Mixin(value = CloudRenderer.class, remap = false)
public class CloudRendererMixin {
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
