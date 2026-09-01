package dev.rdh.argentum.mixin.core;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.spongepowered.asm.mixin.Mixin;

import dev.rdh.argentum.impl.ext.DirectionExtension;

import net.minecraft.util.math.Direction;

@Mixin(Direction.class)
public class DirectionMixin implements DirectionExtension {
	@Override
	public ModelQuadFacing celeritas$toFacing() {
		return switch ((Direction) (Object) this) {
			case DOWN -> ModelQuadFacing.NEG_Y;
			case UP -> ModelQuadFacing.POS_Y;
			case NORTH -> ModelQuadFacing.NEG_Z;
			case SOUTH -> ModelQuadFacing.POS_Z;
			case WEST -> ModelQuadFacing.NEG_X;
			case EAST -> ModelQuadFacing.POS_X;
		};
	}
}
