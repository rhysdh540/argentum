package org.taumc.celeritas.mixin.features.texture;

import net.minecraft.client.render.texture.Texture;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

import java.util.Map;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {
    @Shadow
    @Final
    private Map<Identifier, Texture> textures;

    @Inject(method = "bind", at = @At("HEAD"))
    private void celeritas$captureEntityTexture(Identifier identifier, CallbackInfo ci) {
        EntityInstancingRenderer.setTexture(identifier);
    }

    @Inject(
            method = "close",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/texture/TextureUtil;deleteTextures(I)V")
    )
    private void celeritas$removeClosedTexture(Identifier identifier, CallbackInfo ci) {
        this.textures.remove(identifier);
    }
}
