package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.rdh.cera.BetterGrass;
import dev.rdh.cera.NaturalTextures;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import dev.rdh.argentum.impl.render.terrain.compile.pipeline.FastBlockRenderer;
import dev.rdh.argentum.impl.world.biome.BiomeColorCache;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;
import net.minecraft.block.Block;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = FastBlockRenderer.class, remap = false)
public class FastBlockRendererMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ldev/rdh/argentum/impl/render/terrain/compile/pipeline/FastBlockRenderer;renderQuads(Ljava/util/List;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;Ldev/rdh/argentum/impl/world/cloned/ChunkRenderContext;Lorg/embeddedt/embeddium/impl/model/light/LightPipeline;Lnet/minecraft/util/math/Direction;ILdev/rdh/argentum/impl/world/biome/BiomeColorCache$ColorType;Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;Ldev/rdh/argentum/impl/render/terrain/compile/PrimitiveBuiltRenderSectionData;)V"))
    private List<BakedQuad> cera$betterGrassQuads(List<BakedQuad> quads, BlockPos pos, Block block,
                                                  ChunkRenderContext world, LightPipeline lighter, Direction cullFace, int flags,
                                                  BiomeColorCache.ColorType colorType,
                                                  Material material, ChunkBuildBuffers buffers, PrimitiveBuiltRenderSectionData renderData) {
        return BetterGrass.getFaceQuads(world, world.getBlockState(pos), pos, cullFace, quads);
    }

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
