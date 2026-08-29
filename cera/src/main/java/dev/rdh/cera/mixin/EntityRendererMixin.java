package dev.rdh.cera.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @ModifyVariable(method = "bindTexture(Lnet/minecraft/resource/Identifier;)V", at = @At("HEAD"), argsOnly = true)
    private Identifier cera$randomizeEntityTexture(Identifier texture) {
        return Minecraft.getInstance().getTextureManager().cera$getRandomEntities().apply(texture);
    }
}
