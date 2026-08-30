package dev.rdh.cera.mixin;

import dev.rdh.argentum.impl.render.entity.instancing.EntityInstancing;
import dev.rdh.cera.modules.EmissiveTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.ArrowRenderer;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowRenderer.class)
public class ArrowRendererMixin {
    @Unique
    private static final Identifier CERA$ARROW = new Identifier("textures/entity/arrow.png");
    @Unique
    private boolean cera$reentrant;

    // Instancing-off path (the instanced path is handled in EntityInstancingMixin): two-pass re-render,
    // swapping to the _e texture at full brightness on the second pass.
    @Inject(method = "render(Lnet/minecraft/entity/projectile/ArrowEntity;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void cera$emissiveArrow(ArrowEntity arrow, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        if (this.cera$reentrant) return;
        EmissiveTextures emissive = Minecraft.getInstance().getTextureManager().cera$getEmissiveTextures();
        if (emissive.emissiveTexture(CERA$ARROW) == null) return;
        // Instancing on: argentum instances the arrow and EntityInstancingMixin adds the overlay; skip here.
        EntityInstancing instancing = EntityInstancing.current();
        if (instancing != null && instancing.isBatchActive()) return;

        ci.cancel();
        ArrowRenderer self = (ArrowRenderer) (Object) this;
        this.cera$reentrant = true;
        emissive.beginRender();
        try {
            self.render(arrow, x, y, z, yaw, tickDelta);
            if (emissive.hasEmissive()) {
                emissive.beginRenderEmissive();
                try {
                    self.render(arrow, x, y, z, yaw, tickDelta);
                } finally {
                    emissive.endRenderEmissive();
                }
            }
        } finally {
            emissive.endRender();
            this.cera$reentrant = false;
        }
    }
}
