package dev.rdh.cera.mixin;

import dev.rdh.argentum.impl.extensions.WorldRendererExtension;
import dev.rdh.cera.CeraClientWorldExtension;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private ClientWorld world;

    @Inject(method = "tick", at = @At("TAIL"))
    private void cera$updateDynamicLights(CallbackInfo ci) {
        if (this.world != null) {
            var renderer = ((WorldRendererExtension)this).argentum$getWorldRenderer();
            ((CeraClientWorldExtension)this.world).cera$getDynamicLights().update(this.world, renderer);
        }
    }
}
