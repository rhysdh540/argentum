package org.taumc.celeritas.mixin.features.model.instancing;

import net.minecraft.client.render.texture.AbstractTexture;
import net.minecraft.client.render.texture.Texture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(AbstractTexture.class)
public abstract class AbstractTextureMixin {
    @Inject(method = "clearGlId", at = @At("HEAD"))
    private void celeritas$invalidateInstancedSkin(CallbackInfo ci) {
        EntityInstancingRenderer.invalidateTexture((Texture)(Object)this);
    }
}
