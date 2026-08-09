package dev.rdh.cera.modules.ctm;

import dev.rdh.cera.Cera;
import dev.rdh.cera.modules.ctm.CtmRule.Method;
import dev.rdh.cera.modules.ctm.CtmRule.Tile;
import dev.rdh.cera.modules.ctm.CtmRule.TileAction;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dev.rdh.cera.modules.ctm.CtmRule.sprite;
import static org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags.IS_PARTIAL;

public final class ConnectedTextures {
    private List<CtmRule> pending = List.of();
    private volatile State state = State.EMPTY;

    public void reload(TextureAtlas atlas, Map<String, TextureAtlasSprite> sourcedSprites) {
        CtmLookup.validate();
        CompactCtm.validate();
        PaneCulling.validate();
        CtmRule.validate();
        pending = CtmRuleLoader.load(atlas, sourcedSprites);
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
        state = pending.isEmpty() ? State.EMPTY : new State(Map.copyOf(blocks), Map.copyOf(tiles));
        Cera.LOGGER.info("Loaded {} connected texture rules", pending.size());
        pending = List.of();
    }

    public List<BakedQuad> transform(WorldView world, BlockState blockState, BlockPos pos,
            List<BakedQuad> quads, List<Overlay> overlays, CtmRenderContext context) {
        State state = this.state;
        if (Cera.CONFIG.connectedTextures == Mode.OFF || state == State.EMPTY) return quads;
        context.begin(state, world, pos);
        boolean paneGeometry = blockState.getBlock() instanceof PaneBlock
                && usesPaneGeometry(state, world, blockState, pos, quads);
        if (paneGeometry) quads = PaneCulling.cullInnerFaces(blockState, quads, context);

        List<BakedQuad> transformed = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad original = quads.get(i);
            Result replacement = transform(state, world, blockState, pos, original,
                    overlays, context, paneGeometry);
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

    private Result transform(State state, WorldView world, BlockState blockState,
            BlockPos pos, BakedQuad quad, List<Overlay> overlays, CtmRenderContext context,
            boolean paneGeometry) {
        TextureAtlasSprite sprite = sprite(quad);
        if (sprite == null) return Result.NO_MATCH;
        List<BakedQuad> visible = paneGeometry
                ? PaneCulling.cull(world, blockState, pos, quad, sprite, context) : null;
        if (visible != null) {
            if (visible.isEmpty()) return Result.CULLED;
            if (visible.size() == 1) {
                BakedQuad part = visible.getFirst();
                Result result = transformVisible(state, world, blockState, pos,
                        part, sprite, overlays, context);
                return result.matched ? result : Result.of(part);
            }
            List<BakedQuad> transformed = new ObjectArrayList<>(visible.size());
            for (BakedQuad part : visible) {
                Result result = transformVisible(state, world, blockState, pos,
                        part, sprite, overlays, context);
                if (!result.matched) transformed.add(part);
                else if (result.quads == null) transformed.add(result.quad);
                else transformed.addAll(result.quads);
            }
            return Result.split(transformed);
        }
        return transformVisible(state, world, blockState, pos, quad, sprite, overlays, context);
    }

    private static boolean usesPaneGeometry(State state, WorldView world, BlockState blockState,
            BlockPos pos, List<BakedQuad> quads) {
        List<CtmRule> blockRules = state.blocks.get(blockState.getBlock());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = sprite(quad);
            if (sprite == null) continue;
            if (usesPaneGeometry(state.tiles.get(sprite.getName()), world, blockState, pos,
                    quad, sprite) || usesPaneGeometry(blockRules, world, blockState, pos, quad, sprite)) {
                return true;
            }
        }
        return false;
    }

    private static boolean usesPaneGeometry(List<CtmRule> rules, WorldView world,
            BlockState blockState, BlockPos pos, BakedQuad quad, TextureAtlasSprite sprite) {
        if (rules == null) return false;
        for (CtmRule rule : rules) {
            if ((rule.method() == Method.CTM || rule.method() == Method.CTM_COMPACT
                    || rule.method() == Method.OVERLAY_CTM)
                    && rule.matches(world, blockState, pos, quad.getFace(), sprite)) return true;
        }
        return false;
    }

    private Result transformVisible(State state, WorldView world, BlockState blockState,
            BlockPos pos, BakedQuad quad, TextureAtlasSprite sprite,
            List<Overlay> overlays, CtmRenderContext context) {
        Result result = apply(state.tiles.get(sprite.getName()), world, blockState, pos,
                quad, sprite, overlays, context);
        if (!result.matched) {
            result = apply(state.blocks.get(blockState.getBlock()), world, blockState, pos,
                    quad, sprite, overlays, context);
        }
        if (!result.matched || result.quads != null) return result;

        BakedQuad transformed = result.quad;
        if (transformed == quad) return result;
        for (int pass = 1; pass < 4; pass++) {
            sprite = sprite(transformed);
            if (sprite == null) break;
            Result next = apply(state.tiles.get(sprite.getName()), world, blockState, pos,
                    transformed, sprite, overlays, context);
            if (!next.matched || next.quads != null || next.quad == transformed) break;
            result = next;
            transformed = next.quad;
        }
        return result;
    }

    private Result apply(List<CtmRule> rules, WorldView world, BlockState blockState, BlockPos pos,
            BakedQuad quad, TextureAtlasSprite sprite, List<Overlay> overlays, CtmRenderContext context) {
        if (rules == null) return Result.NO_MATCH;
        for (CtmRule rule : rules) {
            if (!rule.matches(world, blockState, pos, quad.getFace(), sprite)) continue;
            if (rule.method().overlay()) {
                if (overlays == null || (BakedQuadView.of(quad).getFlags() & IS_PARTIAL) != 0) continue;
                for (Tile tile : rule.overlays(world, blockState, pos, quad, sprite, context)) {
                    overlays.add(new Overlay(
                            context.remap(quad, sprite, tile.sprite(), rule.tintIndex()),
                            rule.layer(), rule.tintState()));
                }
                continue;
            }
            if (rule.method() == Method.CTM_COMPACT) {
                List<BakedQuad> compact = rule.compact(world, blockState, pos, quad, sprite, context);
                if (compact != null) return Result.split(compact);
                continue;
            }
            Tile tile = rule.select(world, blockState, pos, quad, sprite, context);
            if (tile == null) continue;
            if (tile.action() == TileAction.SKIP) continue;
            if (tile.action() == TileAction.DEFAULT || tile.sprite() == sprite) return Result.of(quad);
            return Result.of(context.remap(quad, sprite, tile.sprite(), quad.getTintIndex()));
        }
        return Result.NO_MATCH;
    }

    private record State(Map<Block, List<CtmRule>> blocks, Map<String, List<CtmRule>> tiles) {
        private static final State EMPTY = new State(Map.of(), Map.of());
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
