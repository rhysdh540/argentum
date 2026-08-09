package dev.rdh.cera.modules.ctm;

import dev.rdh.cera.modules.ctm.ConnectedTextures.Overlay;
import dev.rdh.cera.modules.ctm.CtmRule.Compact;
import dev.rdh.cera.modules.ctm.CtmRule.Decoration;
import dev.rdh.cera.modules.ctm.CtmRule.Replacement;
import dev.rdh.cera.modules.ctm.CtmRule.Tile;
import dev.rdh.cera.modules.ctm.CtmRule.TileAction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.render.block.BlockModelShaper;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.List;
import java.util.Map;

import static dev.rdh.cera.modules.ctm.CtmRule.sprite;

final class FaceTransformation {
    static final FaceTransformation EMPTY = new FaceTransformation(Map.of(), Map.of(),
            new PaneCulling(), new QuadGeometry.Registry());

    private final Map<Block, List<CtmRule>> blocks;
    private final Map<String, List<CtmRule>> tiles;
    private final PaneCulling panes;
    private final QuadGeometry.Registry geometries;

    FaceTransformation(Map<Block, List<CtmRule>> blocks, Map<String, List<CtmRule>> tiles,
            PaneCulling panes, QuadGeometry.Registry geometries) {
        this.blocks = blocks;
        this.tiles = tiles;
        this.panes = panes;
        this.geometries = geometries;
    }

    FaceTransformation compile(BlockModelShaper models) {
        PaneCulling panes = new PaneCulling();
        QuadGeometry.Registry geometries = new QuadGeometry.Registry();
        for (Block block : Block.REGISTRY) {
            for (BlockState state : block.stateDefinition().all()) {
                BakedModel model = models.getModel(state);
                geometries.compile(model);
                if (PaneCulling.supports(block)) panes.compile(model, geometries);
            }
        }
        panes.validateCompiled();
        geometries.validateCompiled();
        return new FaceTransformation(this.blocks, this.tiles, panes, geometries);
    }

    List<BakedQuad> transform(WorldView world, BlockState state, BlockPos pos,
            List<BakedQuad> quads, List<Overlay> overlays, CtmRenderContext context) {
        context.begin(this, world, pos);
        boolean paneGeometry = PaneCulling.supports(state.getBlock())
                && usesPaneGeometry(world, state, pos, quads);
        if (paneGeometry) quads = this.panes.prepare(quads);

        List<BakedQuad> transformed = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad original = quads.get(i);
            Result replacement = transform(world, state, pos, original,
                    this.geometries.get(original), overlays, context, paneGeometry);
            if (transformed == null && (!replacement.matched || replacement.quad == original)) {
                continue;
            }
            if (transformed == null) {
                transformed = new ObjectArrayList<>(quads.size() + (replacement.quads == null
                        ? 0 : replacement.quads.size() - 1));
                transformed.addAll(quads.subList(0, i));
            }
            if (!replacement.matched) transformed.add(original);
            else if (replacement.quads == null) transformed.add(replacement.quad);
            else transformed.addAll(replacement.quads);
        }
        return transformed == null ? quads : transformed;
    }

    private Result transform(WorldView world, BlockState state, BlockPos pos, BakedQuad quad,
            QuadGeometry geometry, List<Overlay> overlays, CtmRenderContext context,
            boolean paneGeometry) {
        TextureAtlasSprite sprite = sprite(quad);
        if (sprite == null) return Result.NO_MATCH;
        List<BakedQuad> visible = paneGeometry
                ? this.panes.cull(world, state, pos, quad, geometry, sprite, context) : null;
        if (visible != null) {
            if (visible.isEmpty()) return Result.CULLED;
            if (visible.size() == 1) {
                BakedQuad part = visible.getFirst();
                Result result = transformVisible(world, state, pos, part,
                        this.geometries.get(part), sprite, overlays, context);
                return result.matched ? result : Result.of(part);
            }
            List<BakedQuad> transformed = new ObjectArrayList<>(visible.size());
            for (BakedQuad part : visible) {
                Result result = transformVisible(world, state, pos, part,
                        this.geometries.get(part), sprite, overlays, context);
                if (!result.matched) transformed.add(part);
                else if (result.quads == null) transformed.add(result.quad);
                else transformed.addAll(result.quads);
            }
            return Result.split(transformed);
        }
        return transformVisible(world, state, pos, quad, geometry, sprite, overlays, context);
    }

    private boolean usesPaneGeometry(WorldView world, BlockState state, BlockPos pos,
            List<BakedQuad> quads) {
        List<CtmRule> blockRules = this.blocks.get(state.getBlock());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = sprite(quad);
            if (sprite == null) continue;
            if (usesPaneGeometry(this.tiles.get(sprite.getName()), world, state, pos, quad, sprite)
                    || usesPaneGeometry(blockRules, world, state, pos, quad, sprite)) return true;
        }
        return false;
    }

    private static boolean usesPaneGeometry(List<CtmRule> rules, WorldView world,
            BlockState state, BlockPos pos, BakedQuad quad, TextureAtlasSprite sprite) {
        if (rules == null) return false;
        for (CtmRule rule : rules) {
            if (rule.action().paneGeometry()
                    && rule.matches(world, state, pos, quad.getFace(), sprite)) return true;
        }
        return false;
    }

    private Result transformVisible(WorldView world, BlockState state, BlockPos pos,
            BakedQuad quad, QuadGeometry geometry, TextureAtlasSprite sprite,
            List<Overlay> overlays, CtmRenderContext context) {
        Result result = apply(this.tiles.get(sprite.getName()), world, state, pos,
                quad, geometry, sprite, overlays, context);
        if (!result.matched) {
            result = apply(this.blocks.get(state.getBlock()), world, state, pos,
                    quad, geometry, sprite, overlays, context);
        }
        if (!result.matched || result.quads != null) return result;

        BakedQuad transformed = result.quad;
        if (transformed == quad) return result;
        for (int pass = 1; pass < 4; pass++) {
            sprite = sprite(transformed);
            if (sprite == null) break;
            Result next = apply(this.tiles.get(sprite.getName()), world, state, pos,
                    transformed, geometry, sprite, overlays, context);
            if (!next.matched || next.quads != null || next.quad == transformed) break;
            result = next;
            transformed = next.quad;
        }
        return result;
    }

    private static Result apply(List<CtmRule> rules, WorldView world, BlockState state,
            BlockPos pos, BakedQuad quad, QuadGeometry geometry, TextureAtlasSprite sprite,
            List<Overlay> overlays, CtmRenderContext context) {
        if (rules == null) return Result.NO_MATCH;
        for (CtmRule rule : rules) {
            if (!rule.matches(world, state, pos, quad.getFace(), sprite)) continue;
            if (rule.action() instanceof Decoration action) {
                if (overlays == null || geometry.partial()) continue;
                for (Tile tile : action.selector().select(
                        rule, world, state, pos, geometry, sprite, context)) {
                    overlays.add(new Overlay(
                            context.remap(quad, sprite, tile.sprite(), action.tintIndex()),
                            action.layer(), action.tintState()));
                }
                continue;
            }
            if (rule.action() instanceof Compact action) {
                List<BakedQuad> compact = rule.compact(
                        world, state, pos, quad, geometry, sprite, context, action);
                if (compact != null) return Result.split(compact);
                continue;
            }
            Replacement action = (Replacement)rule.action();
            Tile tile = action.selector().select(
                    rule, world, state, pos, geometry, sprite, context);
            if (tile == null || tile.action() == TileAction.SKIP) continue;
            if (tile.action() == TileAction.DEFAULT || tile.sprite() == sprite) {
                return Result.of(quad);
            }
            return Result.of(context.remap(quad, sprite, tile.sprite(), quad.getTintIndex()));
        }
        return Result.NO_MATCH;
    }

    private record Result(BakedQuad quad, List<BakedQuad> quads, boolean matched) {
        private static final Result NO_MATCH = new Result(null, null, false);
        private static final Result CULLED = new Result(null, List.of(), true);

        private static Result of(BakedQuad quad) {
            return new Result(quad, null, true);
        }

        private static Result split(List<BakedQuad> quads) {
            return new Result(null, quads, true);
        }
    }
}
