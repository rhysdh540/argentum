package org.taumc.celeritas.mixin.features.model.instancing;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.resource.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("getTextureLocation")
    Identifier celeritas$getTextureLocation(Entity entity);
}
