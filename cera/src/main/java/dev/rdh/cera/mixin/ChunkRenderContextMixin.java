package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;
import dev.rdh.cera.DynamicLights;
import net.minecraft.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkRenderContext.class)
public abstract class ChunkRenderContextMixin {
    @Shadow
    public abstract BlockState getBlockState(int x, int y, int z);

    @ModifyReturnValue(method = "getLightColor(IIII)I", at = @At("RETURN"))
    private int cera$applyDynamicLight(int packedLight, int x, int y, int z, int ambientLight) {
		if(this.getBlockState(x, y, z).getBlock().isOpaque()) {
            return packedLight;
        } else {
            return DynamicLights.combine(x, y, z, packedLight);
        }
	}
}
