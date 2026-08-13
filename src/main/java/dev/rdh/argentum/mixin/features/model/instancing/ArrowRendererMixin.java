package dev.rdh.argentum.mixin.features.model.instancing;

import net.minecraft.client.render.entity.ArrowRenderer;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.render.entity.instancing.EntityInstancing;

@Mixin(ArrowRenderer.class)
public abstract class ArrowRendererMixin {
    private static final Identifier CELERITAS$ARROW_TEXTURE = new Identifier("textures/entity/arrow.png");

    @Inject(
            method = "render(Lnet/minecraft/entity/projectile/ArrowEntity;DDDFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void celeritas$instanceArrow(ArrowEntity arrow, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        EntityInstancing instancing = EntityInstancing.current();
        if (instancing != null && instancing.recordArrow(arrow, x, y, z, tickDelta,
                CELERITAS$ARROW_TEXTURE, EntityInstancing.packedLight(arrow, tickDelta))) {
            ci.cancel();
        }
    }
}
