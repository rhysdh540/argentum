package dev.rdh.cera.modules.ctm;

import dev.rdh.cera.Cera;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.block.BlockModelShaper;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConnectedTextures {
    private List<CtmRule> pending = List.of();
    private volatile FaceTransformation transformation = FaceTransformation.EMPTY;

    public void reload(ResourceManager resources, TextureAtlas atlas, Map<String, TextureAtlasSprite> sourcedSprites) {
        CtmLookup.validate();
        CompactCtm.validate();
        PaneCulling.validate();
        CtmRule.validate();
        pending = CtmRuleLoader.load(resources, atlas, sourcedSprites);
    }

    public void bake() {
        Map<Block, List<CtmRule>> blocks = new Object2ObjectOpenHashMap<>();
        Map<String, List<CtmRule>> tiles = new Object2ObjectOpenHashMap<>();
        for (CtmRule rule : pending) {
            for (Block block : rule.matchBlocks().keySet()) {
                blocks.computeIfAbsent(block, _ -> new ObjectArrayList<>()).add(rule);
            }
            for (String tile : rule.matchTiles()) {
                tiles.computeIfAbsent(tile, _ -> new ObjectArrayList<>()).add(rule);
            }
        }
        blocks.replaceAll((_, rules) -> List.copyOf(rules));
        tiles.replaceAll((_, rules) -> List.copyOf(rules));
        transformation = pending.isEmpty() ? FaceTransformation.EMPTY
                : new FaceTransformation(Map.copyOf(blocks), Map.copyOf(tiles), new PaneCulling(),
                        new QuadGeometry.Registry());
        Cera.LOGGER.info("[CTM] Loaded {} rules", pending.size());
        pending = List.of();
    }

    public void compileGeometry(BlockModelShaper models) {
        FaceTransformation transformation = this.transformation;
        if (transformation != FaceTransformation.EMPTY) {
            this.transformation = transformation.compile(models);
        }
    }

    public List<BakedQuad> transform(WorldView world, BlockState blockState, BlockPos pos,
            List<BakedQuad> quads, List<Overlay> overlays, CtmRenderContext context) {
        FaceTransformation transformation = this.transformation;
        return Cera.CONFIG.connectedTextures == Mode.OFF
                || transformation == FaceTransformation.EMPTY ? quads
                : transformation.transform(world, blockState, pos, quads, overlays, context);
    }

    public record Overlay(BakedQuad quad, BlockLayer layer, BlockState tintState) {
    }

    public enum Mode {
        OFF,
        FAST,
        FANCY;

        public String key() {
            return "value.connected_textures." + name().toLowerCase(Locale.ROOT);
        }
    }
}
