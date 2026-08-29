package dev.rdh.argentum.impl.render.terrain.compile.pipeline;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PlanksBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.render.texture.TextureUtil;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.util.ColorMixer;
import org.embeddedt.embeddium.impl.model.light.DiffuseProvider;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BakedQuadGroupAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveChunkBuildContext;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveModelUtil;
import dev.rdh.argentum.impl.render.terrain.compile.light.PrimitiveLightDataCache;
import dev.rdh.argentum.impl.world.biome.BiomeColorCache;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;

import java.util.Arrays;
import java.util.List;

public final class FastBlockRenderer {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final PrimitiveChunkBuildContext context;
    private final LightPipelineProvider lighters;
    private final QuadLightData quadLight = new QuadLightData();
    private final int[] colors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private final ModelQuadOrientation[] orientations = new ModelQuadOrientation[DIRECTIONS.length];
    private final BakedQuadGroupAnalyzer analyzer = new BakedQuadGroupAnalyzer();
    private final BlockPos.Mutable neighborPos = new BlockPos.Mutable();
    private final BlockPos.Mutable colorPos = new BlockPos.Mutable();
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    public FastBlockRenderer(PrimitiveChunkBuildContext context, PrimitiveLightDataCache lightCache) {
        this.context = context;
        this.lighters = new LightPipelineProvider(lightCache, DiffuseProvider.NONE, true);
        this.analyzer.setDefaultRenderingFlags(BakedQuadGroupAnalyzer.USE_REORIENTING);
    }

    public void beginSection() {
        this.lighters.reset();
    }

    public void render(BlockState state, BlockPos pos, ChunkRenderContext world, BlockLayer layer,
                       ChunkBuildBuffers buffers, PrimitiveBuiltRenderSectionData renderData) {
        Arrays.fill(this.orientations, null);
        this.analyzer.setDefaultRenderingFlags(BakedQuadGroupAnalyzer.USE_REORIENTING);

        Block block = state.getBlock();
        BakedModel model = Minecraft.getInstance().getBlockRenderDispatcher().getModel(state, world, pos);
        Material material = buffers.getRenderPassConfiguration().getMaterialForRenderType(layer);
        boolean smooth = Minecraft.isAmbientOcclusionEnabled() && block.getLight() == 0 && model.useAmbientOcclusion();
        LightPipeline lighter = this.lighters.getLighter(smooth ? LightMode.SMOOTH : LightMode.FLAT);
        BiomeColorCache.BiomeColorSource colorType = getBiomeColorType(state, world, pos);
        this.setOffset(block, pos);

        for (Direction direction : DIRECTIONS) {
            List<BakedQuad> quads = model.getQuads(direction);
            if (quads.isEmpty()) continue;

            this.neighborPos.set(pos.getX() + direction.getOffsetX(), pos.getY() + direction.getOffsetY(), pos.getZ() + direction.getOffsetZ());
            if (!block.shouldRenderFace(world, this.neighborPos, direction)) continue;

            int flags = this.analyzer.getFlagsForRendering(PrimitiveModelUtil.fromDirection(direction), BakedQuadView.ofList(quads));
            this.renderQuads(quads, pos, state, world, lighter, direction, flags, colorType, material, buffers, renderData);
        }

        List<BakedQuad> quads = model.getQuads();
        if (!quads.isEmpty()) {
            int flags = this.analyzer.getFlagsForRendering(ModelQuadFacing.UNASSIGNED, BakedQuadView.ofList(quads));
            this.renderQuads(quads, pos, state, world, lighter, null, flags, colorType, material, buffers, renderData);
        }
    }

    private void renderQuads(List<BakedQuad> quads, BlockPos pos, BlockState colorState, ChunkRenderContext world,
                             LightPipeline lighter, Direction cullFace, int flags, BiomeColorCache.BiomeColorSource colorType,
                             Material material,
                             ChunkBuildBuffers buffers, PrimitiveBuiltRenderSectionData renderData) {
        ModelQuadFacing cull = cullFace == null ? ModelQuadFacing.UNASSIGNED : PrimitiveModelUtil.fromDirection(cullFace);
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            BakedQuadView view = BakedQuadView.of(quad);
            TextureAtlasSprite sprite = (TextureAtlasSprite)view.celeritas$getSprite();

            lighter.calculate(view, pos.getX(), pos.getY(), pos.getZ(), this.quadLight, cull, view.getLightFace(), true, false);

            if (quad.hasTint()) {
                if (colorType != null && Argentum.CONFIG.biomeBlendRadius > 0) {
                    this.getVertexColors(world, pos, view, colorType);
                } else {
                    Block colorBlock = colorState.getBlock();
                    int color;
                    if (colorBlock == world.getBlockState(pos).getBlock()) {
                        color = colorBlock.getColor(world, pos, quad.getTintIndex());
                    } else if (colorType != null) {
                        color = world.getBiomeColor(pos, colorType);
                    } else {
                        color = colorBlock.getColor(colorState);
                    }
                    if (GameRenderer.anaglyphEnabled) color = TextureUtil.getAnaglyphColor(color);
                    Arrays.fill(this.colors, 0xFF000000 | ColorARGB.toABGR(color));
                }
            } else {
                Arrays.fill(this.colors, -1);
            }

            ModelQuadOrientation orientation = ModelQuadOrientation.NORMAL;
            if ((flags & BakedQuadGroupAnalyzer.USE_REORIENTING) != 0) {
                int index = cullFace == null ? -1 : cullFace.ordinal();
                orientation = index < 0 ? ModelQuadOrientation.NORMAL : this.orientations[index];
                if (orientation == null) {
                    this.orientations[index] = orientation = ModelQuadOrientation.orientByBrightness(this.quadLight.br, this.quadLight.lm);
                }
            }

            Material selected = (view.getFlags() & ModelQuadFlags.IS_TRUSTED_SPRITE) != 0
                    ? this.context.selectMaterial(material, sprite) : material;
            if (sprite != null && sprite.isAnimated()) renderData.animatedSprites.add(sprite);
            this.writeQuad(view, pos, selected, orientation, buffers);
        }
    }

    private static BiomeColorCache.ColorType getBiomeColorType(BlockState state, ChunkRenderContext world, BlockPos pos) {
        Block block = state.getBlock();
        if (block == Blocks.GRASS || block == Blocks.TALLGRASS || block == Blocks.REEDS) {
            return BiomeColorCache.ColorType.GRASS;
        }
        if (block == Blocks.DOUBLE_PLANT) {
            DoublePlantBlock.Variant variant = Blocks.DOUBLE_PLANT.getVariant(world, pos);
            return variant == DoublePlantBlock.Variant.GRASS || variant == DoublePlantBlock.Variant.FERN
                    ? BiomeColorCache.ColorType.GRASS : null;
        }
        if (block == Blocks.LEAVES) {
            PlanksBlock.Variant variant = state.get(LeavesBlock.VARIANT);
            return variant == PlanksBlock.Variant.SPRUCE || variant == PlanksBlock.Variant.BIRCH
                    ? null : BiomeColorCache.ColorType.FOLIAGE;
        }
        if (block == Blocks.LEAVES2 || block == Blocks.VINE) {
            return BiomeColorCache.ColorType.FOLIAGE;
        }
        return null;
    }

    private void getVertexColors(ChunkRenderContext world, BlockPos pos, BakedQuadView quad, BiomeColorCache.BiomeColorSource colorType) {
        for (int vertex = 0; vertex < 4; vertex++) {
            float x = quad.getX(vertex) - 0.5F;
            float z = quad.getZ(vertex) - 0.5F;
            int minX = MathHelper.floor(x);
            int minY = MathHelper.floor(quad.getY(vertex) - 0.5F);
            int minZ = MathHelper.floor(z);
            int worldX = pos.getX() + minX;
            int worldY = pos.getY() + minY;
            int worldZ = pos.getZ() + minZ;

            int c00 = world.getBiomeColor(this.colorPos.set(worldX, worldY, worldZ), colorType);
            int c01 = world.getBiomeColor(this.colorPos.set(worldX, worldY, worldZ + 1), colorType);
            int c10 = world.getBiomeColor(this.colorPos.set(worldX + 1, worldY, worldZ), colorType);
            int c11 = world.getBiomeColor(this.colorPos.set(worldX + 1, worldY, worldZ + 1), colorType);
            int z0 = c00 == c01 ? c00 : ColorMixer.mix(c00, c01, z - minZ);
            int z1 = c10 == c11 ? c10 : ColorMixer.mix(c10, c11, z - minZ);
            int color = z0 == z1 ? z0 : ColorMixer.mix(z0, z1, x - minX);

            if (GameRenderer.anaglyphEnabled) color = TextureUtil.getAnaglyphColor(color);
            this.colors[vertex] = 0xFF000000 | ColorARGB.toABGR(color);
        }
    }

    private void writeQuad(BakedQuadView quad, BlockPos pos, Material material, ModelQuadOrientation orientation, ChunkBuildBuffers buffers) {
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;
        int normal = quad.getComputedFaceNormal();

        for (int destination = 0; destination < 4; destination++) {
            int source = orientation.getVertexIndex(destination);
            ChunkVertexEncoder.Vertex vertex = this.vertices[destination];
            vertex.x = localX + quad.getX(source) + this.offsetX;
            vertex.y = localY + quad.getY(source) + this.offsetY;
            vertex.z = localZ + quad.getZ(source) + this.offsetZ;
            vertex.color = ChunkColorWriter.EMBEDDIUM.writeColor(ModelQuadUtil.mixARGBColors(this.colors[source], quad.getColor(source)), this.quadLight.br[source]);
            vertex.u = quad.getTexU(source);
            vertex.v = quad.getTexV(source);
            vertex.light = this.quadLight.lm[source];
            vertex.vanillaNormal = quad.getNormalFace().getPackedNormal();
            vertex.trueNormal = normal;
        }

        buffers.get(material).getVertexBuffer(quad.getNormalFace()).push(this.vertices, material);
    }

    private void setOffset(Block block, BlockPos pos) {
        this.offsetX = this.offsetY = this.offsetZ = 0.0F;
        if (block.getOffsetType() == Block.OffsetType.NONE) return;

        long seed = MathHelper.hashCode(pos);
        this.offsetX = (((seed >> 16) & 15L) / 15.0F - 0.5F) * 0.5F;
        this.offsetZ = (((seed >> 24) & 15L) / 15.0F - 0.5F) * 0.5F;
        if (block.getOffsetType() == Block.OffsetType.XYZ) {
            this.offsetY = (((seed >> 20) & 15L) / 15.0F - 1.0F) * 0.2F;
        }
    }
}
