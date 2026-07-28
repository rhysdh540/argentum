package dev.rdh.cera.modules.ctm;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;

import static org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags.IS_ALIGNED;

final class PaneCulling {
    private PaneCulling() {
    }

    static void validate() {
        if (covered(7 / 16F, 9 / 16F, 0, 1, false, false, true, false)
                || !covered(7 / 16F, 9 / 16F, 0, 9 / 16F,
                false, false, true, false)) {
            throw new IllegalStateException("Partial pane must not cover a full pane arm");
        }
    }

    static boolean shouldSkip(WorldView world, BlockState state, BlockPos pos,
            BakedQuad quad, TextureAtlasSprite sprite) {
        Block block = state.getBlock();
        if (!(block instanceof PaneBlock)) return false;

        Direction face = quad.getFace();
        BlockPos neighborPos = pos.offset(face);
        BlockState neighbor = world.getBlockState(neighborPos);
        BakedQuadView view = BakedQuadView.of(quad);
        boolean aligned = (view.getFlags() & IS_ALIGNED) != 0;
        if (sprite.getName().startsWith("minecraft:blocks/glass_pane_top")
                && neighbor == state && (face.getAxis() != Direction.Axis.Y || !aligned)) {
            return true;
        }
        if (face.getAxis() != Direction.Axis.Y || !aligned || neighbor.getBlock() != block) {
            return false;
        }
        if (block == Blocks.STAINED_GLASS_PANE
                && neighbor.get(StainedGlassPaneBlock.COLOR) != state.get(StainedGlassPaneBlock.COLOR)) {
            return false;
        }

        neighbor = block.resolveVirtualProperties(neighbor, world, neighborPos);
        return covered(minX(view), maxX(view), minZ(view), maxZ(view),
                neighbor.get(PaneBlock.WEST), neighbor.get(PaneBlock.EAST),
                neighbor.get(PaneBlock.NORTH), neighbor.get(PaneBlock.SOUTH));
    }

    private static boolean covered(float minX, float maxX, float minZ, float maxZ,
            boolean west, boolean east, boolean north, boolean south) {
        if (!west && !east && !north && !south) return true;
        return (minX >= 0.4F || west) && (maxX <= 0.6F || east)
                && (minZ >= 0.4F || north) && (maxZ <= 0.6F || south);
    }

    private static float minX(BakedQuadView quad) {
        return Math.min(Math.min(quad.getX(0), quad.getX(1)), Math.min(quad.getX(2), quad.getX(3)));
    }

    private static float maxX(BakedQuadView quad) {
        return Math.max(Math.max(quad.getX(0), quad.getX(1)), Math.max(quad.getX(2), quad.getX(3)));
    }

    private static float minZ(BakedQuadView quad) {
        return Math.min(Math.min(quad.getZ(0), quad.getZ(1)), Math.min(quad.getZ(2), quad.getZ(3)));
    }

    private static float maxZ(BakedQuadView quad) {
        return Math.max(Math.max(quad.getZ(0), quad.getZ(1)), Math.max(quad.getZ(2), quad.getZ(3)));
    }
}
