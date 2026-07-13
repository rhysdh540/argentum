package org.taumc.celeritas.mixin.core;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.taumc.celeritas.impl.render.terrain.matrix.PrimitiveChunkMatrixGetter;

import net.minecraft.client.render.FrustumCuller;
import net.minecraft.client.render.FrustumData;

@Mixin(FrustumCuller.class)
public class FrustumCullerMixin implements ViewportProvider {
    @Shadow
    private FrustumData frustum;

    @Shadow
    private double offsetX, offsetY, offsetZ;

    @Override
    public Viewport sodium$createViewport() {
        PrimitiveChunkMatrixGetter.update(this.frustum.projectionMatrix, this.frustum.modelMatrix);

        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.set(frustum.modelMatrix);
        modelMatrix.invert();
        Vector3f offset = new Vector3f();
        modelMatrix.transformPosition(offset);
        Frustum cullTester = (minX, minY, minZ, maxX, maxY, maxZ) -> this.frustum.contains(minX, minY, minZ, maxX, maxY, maxZ);
        return new Viewport(cullTester,
                new org.joml.Vector3d(this.offsetX + offset.x, this.offsetY + offset.y, this.offsetZ + offset.z));
    }
}
