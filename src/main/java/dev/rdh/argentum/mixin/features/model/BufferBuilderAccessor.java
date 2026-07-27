package dev.rdh.argentum.mixin.features.model;

import net.minecraft.client.render.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("building")
    boolean celeritas$isBuilding();
}
