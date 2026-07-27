package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.cera.NaturalTextures;
import net.minecraft.util.math.BlockPos;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.compile.pipeline.FastBlockRenderer;

@Mixin(value = FastBlockRenderer.class, remap = false)
public class FastBlockRendererMixin {
    @Inject(method = "writeQuad", at = @At("HEAD"))
    private void cera$prepareNaturalTexture(BakedQuadView quad, BlockPos pos, Material material,
                                            ModelQuadOrientation orientation, ChunkBuildBuffers buffers, CallbackInfo ci, @Share("transform") LocalIntRef transform) {
        transform.set(NaturalTextures.getTransform(quad, pos));
    }

    @WrapOperation(method = "writeQuad",
            at = @At(value = "INVOKE",
                    target = "Lorg/embeddedt/embeddium/impl/model/quad/BakedQuadView;getTexU(I)F"))
    private float cera$rotateNaturalTextureU(BakedQuadView quad, int vertex, Operation<Float> original, @Share("transform") LocalIntRef transform) {
        return original.call(quad, NaturalTextures.transformVertex(transform.get(), vertex));
    }

    @WrapOperation(method = "writeQuad",
            at = @At(value = "INVOKE",
                    target = "Lorg/embeddedt/embeddium/impl/model/quad/BakedQuadView;getTexV(I)F"))
    private float cera$rotateNaturalTextureV(BakedQuadView quad, int vertex, Operation<Float> original, @Share("transform") LocalIntRef transform) {
        return original.call(quad, NaturalTextures.transformVertex(transform.get(), vertex));
    }
}
