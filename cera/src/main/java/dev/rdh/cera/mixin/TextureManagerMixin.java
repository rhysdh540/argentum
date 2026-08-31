package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.texture.SimpleTexture;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.resource.Identifier;

import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

import dev.rdh.cera.ext.CeraTextureManagerExtension;
import dev.rdh.cera.modules.AnimatedTextures;
import dev.rdh.cera.modules.CustomGuis;
import dev.rdh.cera.modules.EmissiveTextures;
import dev.rdh.cera.modules.random.RandomEntities;

@Mixin(TextureManager.class)
public class TextureManagerMixin implements CeraTextureManagerExtension {
    @Shadow @Final
    private Map<Identifier, Texture> textures;

    @Unique
    private final AnimatedTextures cera$animatedTextures = new AnimatedTextures();

    @Unique
    private final CustomGuis cera$customGuis = new CustomGuis();

    @Unique
    private final RandomEntities cera$randomEntities = new RandomEntities();

    @Unique
    private final EmissiveTextures cera$emissiveTextures = new EmissiveTextures();

    @Override
    public AnimatedTextures cera$getAnimatedTextures() {
        return this.cera$animatedTextures;
    }

    @Override
    public EmissiveTextures cera$getEmissiveTextures() {
        return this.cera$emissiveTextures;
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

    @ModifyVariable(method = "bind", at = @At("HEAD"), argsOnly = true)
    private Identifier cera$emissiveTexture(Identifier texture) {
        return cera$emissiveTextures.resolveBound(texture);
    }

    @ModifyExpressionValue(
        method = "bind",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object cera$redirectAnimatedTexture(Object bound, Identifier texture) {
        Texture tex = cera$animatedTextures.overrideFor(texture);
        return tex != null ? tex : bound;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void cera$tickAnimatedTextures(CallbackInfo ci) {
        cera$animatedTextures.tick();
    }

    @Inject(method = "reload", at = @At("HEAD"))
    private void cera$pruneMissingTextures(CallbackInfo ci) {
        var resources = ResourceManager.client();
        this.textures.entrySet().removeIf(entry ->
                entry.getValue() instanceof SimpleTexture && !resources.hasResource(entry.getKey()));
    }
}
