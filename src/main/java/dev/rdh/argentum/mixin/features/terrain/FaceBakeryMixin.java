package dev.rdh.argentum.mixin.features.terrain;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.rdh.argentum.impl.ext.BakedQuadExtension;

import net.minecraft.client.render.model.block.FaceBakery;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {
    @ModifyReturnValue(method = "bakeQuad", at = @At("RETURN"))
    private BakedQuad argentum$attachSprite(BakedQuad quad, @Local(argsOnly = true) TextureAtlasSprite sprite) {
        ((BakedQuadExtension) quad).argentum$setSprite(sprite);
        return quad;
    }
}
