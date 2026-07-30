package dev.rdh.argentum.extras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import dev.rdh.argentum.extras.ArgentumExtras;
import dev.rdh.argentum.extras.LeafQuality;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import dev.rdh.argentum.impl.render.terrain.compile.pipeline.FastBlockRenderer;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;
import net.minecraft.block.AbstractLeavesBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.util.math.BlockPos;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FastBlockRenderer.class, remap = false)
public class FastBlockRendererMixin {
    @WrapWithCondition(method = "renderQuads",
            at = @At(value = "INVOKE",
                    target = "Ldev/rdh/argentum/impl/render/terrain/compile/pipeline/FastBlockRenderer;writeQuad(Lorg/embeddedt/embeddium/impl/model/quad/BakedQuadView;Lnet/minecraft/util/math/BlockPos;Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;Lorg/embeddedt/embeddium/impl/model/quad/properties/ModelQuadOrientation;Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;)V"))
    private boolean argentumExtras$shouldRenderLeafQuad(FastBlockRenderer renderer, BakedQuadView quad,
            BlockPos pos, Material material, ModelQuadOrientation orientation, ChunkBuildBuffers buffers,
            @Local(argsOnly = true) BlockState colorState,
            @Local(argsOnly = true) ChunkRenderContext world) {
        if (!(colorState.getBlock() instanceof AbstractLeavesBlock)
                || !ModelQuadFlags.contains(quad.getFlags(), ModelQuadFlags.IS_ALIGNED)) {
            return true;
        }

        ModelQuadFacing face = quad.getNormalFace();
        if (!face.isDirection()) return true;

        BlockPos neighborPos = pos.add(face.getStepX(), face.getStepY(), face.getStepZ());
        return switch (ArgentumExtras.CONFIG.leafQuality) {
            case HOLLOW -> !(world.getBlockState(neighborPos).getBlock() instanceof AbstractLeavesBlock);
            case SOLID -> !LeafQuality.isEnclosed(world, neighborPos);
            default -> true;
        };
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void argentumExtras$cullEnclosedLeaf(BlockState state, BlockPos pos, ChunkRenderContext world,
            BlockLayer layer, ChunkBuildBuffers buffers, PrimitiveBuiltRenderSectionData renderData,
            CallbackInfo ci) {
        if (ArgentumExtras.CONFIG.leafQuality == LeafQuality.ENCLOSED && LeafQuality.isEnclosed(world, pos)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lorg/embeddedt/embeddium/impl/render/chunk/RenderPassConfiguration;getMaterialForRenderType(Ljava/lang/Object;)Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;"))
    private Material argentumExtras$solidLeafMaterial(Material original,
            @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) ChunkRenderContext world,
            @Local(argsOnly = true) ChunkBuildBuffers buffers) {
        if (ArgentumExtras.CONFIG.leafQuality == LeafQuality.SOLID && LeafQuality.isEnclosed(world, pos)) {
            return buffers.getRenderPassConfiguration().getMaterialForRenderType(BlockLayer.SOLID);
        }
        return original;
    }
}
