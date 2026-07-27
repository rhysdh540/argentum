package dev.rdh.argentum.extras;

import net.minecraft.block.AbstractLeavesBlock;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

public enum LeafQuality {
    FAST,
    HOLLOW,
    ENCLOSED,
    SOLID,
    FANCY;

    private static final Direction[] DIRECTIONS = Direction.values();

    public static boolean isEnclosed(WorldView world, BlockPos pos) {
        if (!(world.getBlockState(pos).getBlock() instanceof AbstractLeavesBlock)) return false;

        BlockPos.Mutable neighborPos = new BlockPos.Mutable();
        for (Direction direction : DIRECTIONS) {
            neighborPos.set(pos.getX() + direction.getOffsetX(), pos.getY() + direction.getOffsetY(),
                    pos.getZ() + direction.getOffsetZ());
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
