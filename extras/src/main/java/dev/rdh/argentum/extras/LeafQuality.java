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

    public boolean rendersBlock(WorldView world, BlockPos pos, BlockPos.Mutable cursor) {
        return this != ENCLOSED || !isEnclosed(world, pos.getX(), pos.getY(), pos.getZ(), cursor);
    }

    public boolean rendersQuad(WorldView world, BlockState state, BlockPos pos, BakedQuadView quad, BlockPos.Mutable cursor) {
        if (!(state.getBlock() instanceof AbstractLeavesBlock)
                || !ModelQuadFlags.contains(quad.getFlags(), ModelQuadFlags.IS_ALIGNED)) {
            return true;
        }

        ModelQuadFacing face = quad.getNormalFace();
        if (!face.isDirection()) return true;

        int x = pos.getX() + face.getStepX();
        int y = pos.getY() + face.getStepY();
        int z = pos.getZ() + face.getStepZ();
        return switch (this) {
            case HOLLOW -> {
                cursor.set(x, y, z);
                yield !(world.getBlockState(cursor).getBlock() instanceof AbstractLeavesBlock);
            }
            case SOLID -> !isEnclosed(world, x, y, z, cursor);
            default -> true;
        };
    }

    public boolean usesSolidMaterial(WorldView world, BlockPos pos, BlockPos.Mutable cursor) {
        return this == SOLID && isEnclosed(world, pos.getX(), pos.getY(), pos.getZ(), cursor);
    }

    private static boolean isEnclosed(WorldView world, int x, int y, int z, BlockPos.Mutable cursor) {
        cursor.set(x, y, z);
        if (!(world.getBlockState(cursor).getBlock() instanceof AbstractLeavesBlock)) return false;

        for (Direction direction : DIRECTIONS) {
            cursor.set(x + direction.getOffsetX(), y + direction.getOffsetY(), z + direction.getOffsetZ());
            Block neighbor = world.getBlockState(cursor).getBlock();
            if (!(neighbor instanceof AbstractLeavesBlock) && !neighbor.isFaceSolid(world, cursor, direction.getOpposite())) {
                return false;
            }
        }
        return true;
    }

    public static String key(int ordinal) {
        return "value.leaf_quality." + values()[ordinal].name().toLowerCase();
    }
}
