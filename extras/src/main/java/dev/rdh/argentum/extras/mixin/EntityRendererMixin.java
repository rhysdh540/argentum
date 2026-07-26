package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @ModifyConstant(method = "postRender", constant = @Constant(doubleValue = 256.0D))
    private double argentumExtras$changeShadowDistance(double vanilla) {
        int distance = ArgentumExtras.CONFIG.entityShadowDistance;
        return distance * distance;
    }
}
