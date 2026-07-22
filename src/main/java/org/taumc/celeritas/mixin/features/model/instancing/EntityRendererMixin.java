package org.taumc.celeritas.mixin.features.model.instancing;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "bindTexture(Lnet/minecraft/resource/Identifier;)V", at = @At("HEAD"))
    private void celeritas$captureEntityTexture(Identifier texture, CallbackInfo ci) {
        EntityInstancingRenderer.setTexture(texture);
    }
}
