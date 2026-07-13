package org.taumc.celeritas.mixin.core;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.model.Polygon;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.render.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Polygon.class)
public abstract class PolygonMixin {
    @Unique
    private boolean celeritas$drawOnSelf;

    @WrapOperation(
            method = "compile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/BufferBuilder;begin(ILnet/minecraft/client/render/vertex/VertexFormat;)V")
    )
    private void celeritas$beginIfNeeded(BufferBuilder buffer, int mode, VertexFormat format, Operation<Void> original) {
        this.celeritas$drawOnSelf = !((BufferBuilderAccessor) buffer).celeritas$isBuilding();
        if (this.celeritas$drawOnSelf) {
            original.call(buffer, mode, format);
        }
    }

    @WrapOperation(
            method = "compile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/vertex/Tesselator;end()V")
    )
    private void celeritas$endIfNeeded(Tesselator tesselator, Operation<Void> original) {
        if (this.celeritas$drawOnSelf) {
            original.call(tesselator);
        }
    }
}
