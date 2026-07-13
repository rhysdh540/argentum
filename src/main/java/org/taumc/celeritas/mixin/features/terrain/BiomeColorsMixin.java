package org.taumc.celeritas.mixin.features.terrain;

import net.minecraft.client.world.color.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Inject(method = "getGrassColor", at = @At("HEAD"), cancellable = true)
    private static void celeritas$getGrassColor(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (world instanceof ChunkRenderContext context) {
            cir.setReturnValue(context.getGrassColor(pos));
        }
    }

    @Inject(method = "getFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void celeritas$getFoliageColor(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (world instanceof ChunkRenderContext context) {
            cir.setReturnValue(context.getFoliageColor(pos));
        }
    }

    @Inject(method = "getWaterFogColor", at = @At("HEAD"), cancellable = true)
    private static void celeritas$getWaterColor(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (world instanceof ChunkRenderContext context) {
            cir.setReturnValue(context.getWaterColor(pos));
        }
    }
}
