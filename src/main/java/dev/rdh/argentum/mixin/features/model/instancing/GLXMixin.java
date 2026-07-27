package dev.rdh.argentum.mixin.features.model.instancing;

import net.minecraft.client.render.platform.GLX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(GLX.class)
public abstract class GLXMixin {
    @Inject(method = "multiTexCoord2f", at = @At("HEAD"))
    private static void celeritas$captureLightmap(int texture, float u, float v, CallbackInfo ci) {
        if (texture == GLX.GL_TEXTURE1) {
            EntityInstancingRenderer.setLight(u, v);
        }
    }
}
