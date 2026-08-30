package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rdh.argentum.impl.render.entity.instancing.ModelInstancer;
import net.minecraft.client.Minecraft;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ModelInstancer.class, remap = false)
public class ModelInstancerMixin {
    @ModifyReturnValue(method = "emissiveOverlay", at = @At("RETURN"))
    private Identifier cera$emissiveOverlay(Identifier original, Identifier texture) {
        return Minecraft.getInstance().getTextureManager().cera$getEmissiveTextures().emissiveTexture(texture);
    }
}
