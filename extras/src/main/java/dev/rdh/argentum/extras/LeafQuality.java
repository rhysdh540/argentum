package dev.rdh.argentum.extras;

import net.minecraft.block.AbstractLeavesBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;

public enum LeafQuality {
    FAST,
    HOLLOW,
    ENCLOSED,
    SOLID,
    FANCY;

    private static final Direction[] DIRECTIONS = Direction.values();

    public boolean cullsAdjacentLeaves() {
        return this != FAST;
    }

    public boolean rendersBlock(WorldView world, BlockPos pos) {
        return this != ENCLOSED || !isEnclosed(world, pos);
    }

    public boolean rendersQuad(WorldView world, BlockState state, BlockPos pos, BakedQuadView quad) {
        if (!(state.getBlock() instanceof AbstractLeavesBlock)
                || !ModelQuadFlags.contains(quad.getFlags(), ModelQuadFlags.IS_ALIGNED)) {
            return true;
        }

        ModelQuadFacing face = quad.getNormalFace();
        if (!face.isDirection()) return true;

        BlockPos neighborPos = pos.add(face.getStepX(), face.getStepY(), face.getStepZ());
        return switch (this) {
            case HOLLOW -> !(world.getBlockState(neighborPos).getBlock() instanceof AbstractLeavesBlock);
            case SOLID -> !isEnclosed(world, neighborPos);
            default -> true;
        };
    }

    public boolean usesSolidMaterial(WorldView world, BlockPos pos) {
        return this == SOLID && isEnclosed(world, pos);
    }

    private static boolean isEnclosed(WorldView world, BlockPos pos) {
        if (!(world.getBlockState(pos).getBlock() instanceof AbstractLeavesBlock)) return false;

        BlockPos.Mutable neighborPos = new BlockPos.Mutable();
        for (Direction direction : DIRECTIONS) {
            neighborPos.set(pos.getX() + direction.getOffsetX(), pos.getY() + direction.getOffsetY(), pos.getZ() + direction.getOffsetZ());
            Block neighbor = world.getBlockState(neighborPos).getBlock();
            if (!(neighbor instanceof AbstractLeavesBlock)
                    && !neighbor.isFaceSolid(world, neighborPos, direction.getOpposite())) {
                return false;
            }
        }
        return true;
    }

    public static String key(int ordinal) {
        return "value.leaf_quality." + values()[ordinal].name().toLowerCase();
    }
}
