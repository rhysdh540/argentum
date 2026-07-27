package dev.rdh.argentum.mixin.features.terrain;

import net.minecraft.client.world.color.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.world.biome.BiomeColorCache.ColorType;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Inject(method = "getGrassColor", at = @At("HEAD"), cancellable = true)
    private static void celeritas$getGrassColor(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (world instanceof ChunkRenderContext context) {
            cir.setReturnValue(context.getBiomeColor(pos, ColorType.GRASS));
        } else if (Argentum.CONFIG.biomeBlendRadius != 1) {
            cir.setReturnValue(celeritas$getColor(world, pos, 0));
        }
    }

    @Inject(method = "getFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void celeritas$getFoliageColor(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (world instanceof ChunkRenderContext context) {
            cir.setReturnValue(context.getBiomeColor(pos, ColorType.FOLIAGE));
        } else if (Argentum.CONFIG.biomeBlendRadius != 1) {
            cir.setReturnValue(celeritas$getColor(world, pos, 1));
        }
    }

    @Inject(method = "getWaterFogColor", at = @At("HEAD"), cancellable = true)
    private static void celeritas$getWaterColor(WorldView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (world instanceof ChunkRenderContext context) {
            cir.setReturnValue(context.getBiomeColor(pos, ColorType.WATER));
        } else if (Argentum.CONFIG.biomeBlendRadius != 1) {
            cir.setReturnValue(celeritas$getColor(world, pos, 2));
        }
    }

    @Unique
    private static int celeritas$getColor(WorldView world, BlockPos pos, int type) {
        int radius = Argentum.CONFIG.biomeBlendRadius;
        int red = 0;
        int green = 0;
        int blue = 0;
        BlockPos.Mutable sample = new BlockPos.Mutable();

        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                sample.set(pos.getX() + x, pos.getY(), pos.getZ() + z);
                Biome biome = world.getBiome(sample);
                int color = switch (type) {
                    case 0 -> biome.getGrassColor(sample);
                    case 1 -> biome.getFoliageColor(sample);
                    default -> biome.waterFogColor;
                };
                red += color >> 16 & 255;
                green += color >> 8 & 255;
                blue += color & 255;
            }
        }

        int count = (radius * 2 + 1) * (radius * 2 + 1);
        return red / count << 16 | green / count << 8 | blue / count;
    }
}
