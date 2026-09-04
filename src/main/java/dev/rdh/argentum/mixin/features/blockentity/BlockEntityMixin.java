package dev.rdh.argentum.mixin.features.blockentity;

import dev.rdh.argentum.impl.ext.BlockEntityLightHolder;

import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements BlockEntityLightHolder {
    @Unique
    private int argentum$light = -1;
    @Unique
    private int argentum$generation = -1;

    @Override
    public int argentum$getCachedLight(int generation) {
        return this.argentum$generation == generation ? this.argentum$light : -1;
    }

    @Override
    public void argentum$cacheLight(int light, int generation) {
        this.argentum$light = light;
        this.argentum$generation = generation;
    }
}
