package dev.rdh.cera.modules.ctm;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import dev.rdh.cera.modules.BetterGrass;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

import java.util.List;

public final class CtmRenderContext {
    private final Reference2ObjectMap<BakedQuad,
            Reference2ObjectMap<TextureAtlasSprite, Int2ObjectMap<BakedQuad>>> remapped
            = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<CtmRule,
            Reference2ObjectMap<BakedQuad, List<BakedQuad>[]>> compact
            = new Reference2ObjectOpenHashMap<>();
    private final BlockPos.Mutable neighborPos = new BlockPos.Mutable();
    private final long[] neighborPositions = new long[32];
    private final Direction[] neighborFaces = new Direction[32];
    private final TextureAtlasSprite[] neighborSprites = new TextureAtlasSprite[32];
    private final boolean[] neighborPresent = new boolean[32];
    private Object owner;
    private WorldView neighborWorld;
    private int blockX;
    private int blockY;
    private int blockZ;
    private int neighborCount;

    void begin(Object owner, WorldView world, BlockPos pos) {
        if (this.owner != owner) {
            this.owner = owner;
            this.remapped.clear();
            this.compact.clear();
        }
        if (this.neighborWorld != world || this.blockX != pos.getX()
                || this.blockY != pos.getY() || this.blockZ != pos.getZ()) {
            this.neighborWorld = world;
            this.blockX = pos.getX();
            this.blockY = pos.getY();
            this.blockZ = pos.getZ();
            this.neighborCount = 0;
        }
    }

    BakedQuad remap(BakedQuad quad, TextureAtlasSprite from, TextureAtlasSprite to, int tintIndex) {
        Int2ObjectMap<BakedQuad> tinted = this.remapped
                .computeIfAbsent(quad, ignored -> new Reference2ObjectOpenHashMap<>())
                .computeIfAbsent(to, ignored -> new Int2ObjectOpenHashMap<>());
        return tinted.computeIfAbsent(tintIndex, ignored -> CtmRule.remap(quad, from, to, tintIndex));
    }

    @SuppressWarnings("unchecked")
    List<BakedQuad> compact(CtmRule rule, BakedQuad quad, int connections) {
        List<BakedQuad>[] results = this.compact
                .computeIfAbsent(rule, ignored -> new Reference2ObjectOpenHashMap<>())
                .computeIfAbsent(quad, ignored -> new List[256]);
        return results[connections & 255];
    }

    @SuppressWarnings("unchecked")
    void putCompact(CtmRule rule, BakedQuad quad, int connections, List<BakedQuad> result) {
        this.compact
                .computeIfAbsent(rule, ignored -> new Reference2ObjectOpenHashMap<>())
                .computeIfAbsent(quad, ignored -> new List[256])[connections & 255] = result;
    }

    BlockPos offset(BlockPos pos, Direction direction) {
        return this.offset(pos, direction, null);
    }

    BlockPos offset(BlockPos pos, Direction first, Direction second) {
        int x = pos.getX() + first.getOffsetX();
        int y = pos.getY() + first.getOffsetY();
        int z = pos.getZ() + first.getOffsetZ();
        if (second != null) {
            x += second.getOffsetX();
            y += second.getOffsetY();
            z += second.getOffsetZ();
        }
        return this.neighborPos.set(x, y, z);
    }

    TextureAtlasSprite neighborSprite(WorldView world, BlockState state, BlockPos pos, Direction face) {
        long position = pos.toLong();
        for (int i = 0; i < this.neighborCount; i++) {
            if (this.neighborPositions[i] == position && this.neighborFaces[i] == face) {
                return this.neighborPresent[i] ? this.neighborSprites[i] : null;
            }
        }

        TextureAtlasSprite sprite = loadNeighborSprite(world, state, pos, face);
        if (this.neighborCount < this.neighborPositions.length) {
            this.neighborPositions[this.neighborCount] = position;
            this.neighborFaces[this.neighborCount] = face;
            this.neighborSprites[this.neighborCount] = sprite;
            this.neighborPresent[this.neighborCount++] = sprite != null;
        }
        return sprite;
    }

    private static TextureAtlasSprite loadNeighborSprite(WorldView world, BlockState state,
            BlockPos pos, Direction face) {
        state = state.getBlock().resolveVirtualProperties(state, world, pos);
        BakedModel model = Minecraft.getInstance().getBlockRenderDispatcher().getModel(state, world, pos);
        List<BakedQuad> quads = BetterGrass.getFaceQuads(world, state, pos, face, model.getQuads(face));
        if (!quads.isEmpty()) return CtmRule.sprite(quads.getFirst());
        for (BakedQuad quad : model.getQuads()) {
            if (quad.getFace() == face) return CtmRule.sprite(quad);
        }
        return null;
    }
}
