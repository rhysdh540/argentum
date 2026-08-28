package dev.rdh.argentum.extras.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.world.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = WorldRenderer.class, priority = 1005)
public class ArgentumWorldRendererMixin {

    @TargetHandler(
            mixin = "dev.rdh.argentum.mixin.core.render.WorldRendererMixin",
            name = "argentum$fixSkyVboPath"
    )
    @WrapMethod(method = "@MixinSquared:Handler")
    private void argentumExtras$drawLowerSky(int list, Operation<Void> original) {
        if (ArgentumExtras.CONFIG.sky && ArgentumExtras.CONFIG.lowerSky) {
            original.call(list);
        }
    }

}
