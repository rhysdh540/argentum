package dev.rdh.cera.mixin.colors;

import net.minecraft.block.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapColor.class)
public interface MapColorAccessor {
    @Mutable
    @Accessor("color")
    void cera$setColor(int color);
}
