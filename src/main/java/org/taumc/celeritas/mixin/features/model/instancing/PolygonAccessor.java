package org.taumc.celeritas.mixin.features.model.instancing;

import net.minecraft.client.render.model.Polygon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Polygon.class)
public interface PolygonAccessor {
    @Accessor("flipNormal")
    boolean celeritas$isNormalFlipped();
}
