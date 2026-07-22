package org.taumc.celeritas.mixin.features.model;

import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.Tesselator;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void celeritas$recordModelPart(float scale, CallbackInfo ci) {
        if (EntityInstancingRenderer.record((ModelPart)(Object)this, scale)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "compile",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/render/vertex/Tesselator;getBuffer()Lnet/minecraft/client/render/vertex/BufferBuilder;")
    )
    private void celeritas$beginModel(float scale, CallbackInfo ci) {
        Tesselator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormat.ENTITY);
    }

    @Inject(
            method = "compile",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEndList()V", remap = false)
    )
    private void celeritas$endModel(float scale, CallbackInfo ci) {
        Tesselator.getInstance().end();
    }
}
