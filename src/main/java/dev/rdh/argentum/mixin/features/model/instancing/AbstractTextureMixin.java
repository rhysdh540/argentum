package dev.rdh.argentum.mixin.features.model.instancing;

import dev.rdh.argentum.impl.ext.TextureGenerationExtension;
import net.minecraft.client.render.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTexture.class)
public abstract class AbstractTextureMixin implements TextureGenerationExtension {
    @Shadow
    protected int glId;

    @Unique
    private int argentum$generation;

    @Inject(method = "clearGlId", at = @At("HEAD"))
    private void argentum$incrementGeneration(CallbackInfo ci) {
        if (this.glId != -1) {
            this.argentum$generation++;
        }
    }

    @Override
    public int argentum$getGeneration() {
        return this.argentum$generation;
    }
}
