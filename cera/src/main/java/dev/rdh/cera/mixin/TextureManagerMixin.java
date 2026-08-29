package dev.rdh.cera.mixin;

import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.rdh.cera.ext.CeraTextureManagerExtension;
import dev.rdh.cera.modules.AnimatedTextures;
import dev.rdh.cera.modules.CustomGuis;
import dev.rdh.cera.modules.random.RandomEntities;

@Mixin(TextureManager.class)
public class TextureManagerMixin implements CeraTextureManagerExtension {
    @Unique
    private final AnimatedTextures cera$animatedTextures = new AnimatedTextures();

    @Unique
    private final CustomGuis cera$customGuis = new CustomGuis();

    @Unique
    private final RandomEntities cera$randomEntities = new RandomEntities();

    @Override
    public AnimatedTextures cera$getAnimatedTextures() {
        return this.cera$animatedTextures;
    }

    @Override
    public CustomGuis cera$getCustomGuis() {
        return this.cera$customGuis;
    }

    @Override
    public RandomEntities cera$getRandomEntities() {
        return this.cera$randomEntities;
    }

    @ModifyVariable(method = "bind", at = @At("HEAD"), argsOnly = true)
    private Identifier cera$resolveCustomGui(Identifier texture) {
		return cera$customGuis.resolve(texture);
    }

    @ModifyVariable(method = "bind", at = @At("HEAD"), argsOnly = true)
    private Identifier cera$randomizeEntityTexture(Identifier texture) {
        return cera$randomEntities.apply(texture);
    }

    @Inject(method = "bind", at = @At("HEAD"))
    private void cera$applyAnimatedTexture(Identifier texture, CallbackInfo ci) {
        cera$animatedTextures.apply(texture);
    }
}
