package dev.rdh.cera.mixin;

import dev.rdh.cera.CeraTextureAtlasExtension;
import net.minecraft.client.render.block.BlockModelShaper;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.client.resource.manager.ResourceManager;
import net.minecraft.client.resource.model.ModelManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public class ModelManagerMixin {
    @Shadow @Final
    private TextureAtlas blocksAtlas;
    @Shadow @Final
    private BlockModelShaper modelShaper;

    @Inject(method = "reload", at = @At("RETURN"))
    private void cera$compileGeometry(ResourceManager resources, CallbackInfo ci) {
        ((CeraTextureAtlasExtension)this.blocksAtlas).cera$getConnectedTextures()
                .compileGeometry(this.modelShaper);
    }
}
