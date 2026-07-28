package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rdh.cera.modules.DynamicLights;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyReturnValue(method = "getLightLevel", at = @At("RETURN"))
    private int cera$applyDynamicLight(int packedLight) {
        return DynamicLights.combine((Entity)(Object)this, packedLight);
    }
}
