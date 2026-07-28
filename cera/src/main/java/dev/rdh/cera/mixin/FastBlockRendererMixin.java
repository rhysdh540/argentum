package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.rdh.cera.modules.BetterGrass;
import dev.rdh.cera.modules.ctm.ConnectedTextures;
import dev.rdh.cera.modules.ctm.CtmRenderContext;
import dev.rdh.cera.modules.NaturalTextures;
import dev.rdh.cera.CeraTextureAtlasExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import dev.rdh.argentum.impl.render.terrain.compile.pipeline.FastBlockRenderer;
import dev.rdh.argentum.impl.world.biome.BiomeColorCache;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PlanksBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = FastBlockRenderer.class, remap = false)
public class FastBlockRendererMixin {
    @Unique
    private final BetterGrass cera$betterGrass =
            ((CeraTextureAtlasExtension)Minecraft.getInstance().getBlocksAtlas()).cera$getBetterGrass();
    @Unique
    private final ConnectedTextures cera$connectedTextures =
            ((CeraTextureAtlasExtension)Minecraft.getInstance().getBlocksAtlas()).cera$getConnectedTextures();
    @Unique
    private final NaturalTextures cera$naturalTextures =
            ((CeraTextureAtlasExtension)Minecraft.getInstance().getBlocksAtlas()).cera$getNaturalTextures();
    @Unique
    private final List<ConnectedTextures.Overlay> cera$overlays = new ObjectArrayList<>();
    @Unique
    private final CtmRenderContext cera$ctmContext = new CtmRenderContext(this.cera$betterGrass);

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Ldev/rdh/argentum/impl/render/terrain/compile/pipeline/FastBlockRenderer;renderQuads(Ljava/util/List;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/BlockState;Ldev/rdh/argentum/impl/world/cloned/ChunkRenderContext;Lorg/embeddedt/embeddium/impl/model/light/LightPipeline;Lnet/minecraft/util/math/Direction;ILdev/rdh/argentum/impl/world/biome/BiomeColorCache$ColorType;Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;Ldev/rdh/argentum/impl/render/terrain/compile/PrimitiveBuiltRenderSectionData;)V"))
    private void cera$renderQuads(FastBlockRenderer renderer, List<BakedQuad> quads, BlockPos pos,
            BlockState colorState, ChunkRenderContext world, LightPipeline lighter, Direction cullFace,
            int flags, BiomeColorCache.ColorType colorType, Material material,
            ChunkBuildBuffers buffers, PrimitiveBuiltRenderSectionData renderData, Operation<Void> original) {
        var state = world.getBlockState(pos);
        this.cera$overlays.clear();
        List<BakedQuad> transformed = this.cera$connectedTextures.transform(world, state, pos,
                this.cera$betterGrass.getFaceQuads(world, state, pos, cullFace, quads),
                this.cera$overlays, this.cera$ctmContext);
        original.call(renderer, transformed, pos, colorState, world, lighter, cullFace, flags,
                colorType, material, buffers, renderData);
        for (ConnectedTextures.Overlay overlay : this.cera$overlays) {
            Material overlayMaterial = buffers.getRenderPassConfiguration()
                    .getMaterialForRenderType(overlay.layer());
            original.call(renderer, List.of(overlay.quad()), pos, overlay.tintState(), world, lighter,
                    cullFace, flags, cera$getBiomeColorType(overlay.tintState()),
                    overlayMaterial, buffers, renderData);
        }
    }

    @Unique
    private static BiomeColorCache.ColorType cera$getBiomeColorType(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.GRASS || block == Blocks.TALLGRASS || block == Blocks.REEDS) {
            return BiomeColorCache.ColorType.GRASS;
        }
        if (block == Blocks.DOUBLE_PLANT) {
            DoublePlantBlock.Variant variant = state.get(DoublePlantBlock.VARIANT);
            return variant == DoublePlantBlock.Variant.GRASS || variant == DoublePlantBlock.Variant.FERN
                    ? BiomeColorCache.ColorType.GRASS : null;
        }
        if (block == Blocks.LEAVES) {
            PlanksBlock.Variant variant = state.get(LeavesBlock.VARIANT);
            return variant == PlanksBlock.Variant.SPRUCE || variant == PlanksBlock.Variant.BIRCH
                    ? null : BiomeColorCache.ColorType.FOLIAGE;
        }
        return block == Blocks.LEAVES2 || block == Blocks.VINE
                ? BiomeColorCache.ColorType.FOLIAGE : null;
    }

    @Inject(method = "writeQuad", at = @At("HEAD"))
    private void cera$prepareNaturalTexture(BakedQuadView quad, BlockPos pos, Material material,
                                            ModelQuadOrientation orientation, ChunkBuildBuffers buffers, CallbackInfo ci, @Share("transform") LocalIntRef transform) {
        transform.set(this.cera$naturalTextures.getTransform(quad, pos));
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
