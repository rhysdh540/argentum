package dev.rdh.argentum.mixin.features.model.instancing;

import net.minecraft.client.render.model.Box;
import net.minecraft.client.render.model.Polygon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Box.class)
public interface BoxAccessor {
    @Accessor("faces")
    Polygon[] celeritas$getFaces();
}
