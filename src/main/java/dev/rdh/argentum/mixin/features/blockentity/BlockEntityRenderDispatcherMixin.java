package dev.rdh.argentum.mixin.features.blockentity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rdh.argentum.impl.ext.BlockEntityLightHolder;
import dev.rdh.argentum.impl.ext.WorldExtension;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/block/entity/BlockEntity;FI)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getLightColor(Lnet/minecraft/util/math/BlockPos;I)I")
    )
    private int argentum$cacheLight(World world, BlockPos pos, int minLight, Operation<Integer> original,
            @Local(argsOnly = true) BlockEntity blockEntity) {
        int generation = ((WorldExtension)world).argentum$getBlockEntityLight()
                .generation(pos.getX(), pos.getY(), pos.getZ());
        BlockEntityLightHolder holder = (BlockEntityLightHolder)blockEntity;
        int cached = holder.argentum$getCachedLight(generation);
        if (cached != -1) {
            return cached;
        }
        int light = original.call(world, pos, minLight);
        holder.argentum$cacheLight(light, generation);
        return light;
    }
}
