package org.taumc.celeritas.mixin.features.model.instancing;

import org.embeddedt.embeddium.impl.gl.tessellation.GlAbstractTessellation;
import org.embeddedt.embeddium.impl.gl.tessellation.TessellationBinding;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.InstancedVertexAttributeBinding;

@Mixin(value = GlAbstractTessellation.class, remap = false)
public abstract class GlAbstractTessellationMixin {
    @Shadow @Final
    protected TessellationBinding[] bindings;

    @Inject(method = "bindAttributes", at = @At("TAIL"), remap = false)
    private void celeritas$bindDivisors(CallbackInfo ci) {
        for (TessellationBinding binding : this.bindings) {
            for (var attribute : binding.attributeBindings()) {
                if (attribute instanceof InstancedVertexAttributeBinding instanced && instanced.divisor() != 0) {
                    ARBInstancedArrays.glVertexAttribDivisorARB(instanced.getIndex(), instanced.divisor());
                }
            }
        }
    }
}
