package dev.rdh.argentum.impl.render.terrain.compile;

import net.minecraft.util.math.Direction;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;

public final class PrimitiveModelUtil {
    private PrimitiveModelUtil() {
    }

    public static ModelQuadFacing fromDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> ModelQuadFacing.NEG_Y;
            case UP -> ModelQuadFacing.POS_Y;
            case NORTH -> ModelQuadFacing.NEG_Z;
            case SOUTH -> ModelQuadFacing.POS_Z;
            case WEST -> ModelQuadFacing.NEG_X;
            case EAST -> ModelQuadFacing.POS_X;
        };
    }
}
