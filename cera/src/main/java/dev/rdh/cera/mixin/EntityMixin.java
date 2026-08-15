package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rdh.cera.ext.CeraClientWorldExtension;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public World world;

    @ModifyReturnValue(method = "getLightLevel", at = @At("RETURN"))
    private int cera$applyDynamicLight(int packedLight) {
        return this.world instanceof CeraClientWorldExtension world
                ? world.cera$getDynamicLights().combine((Entity)(Object)this, packedLight)
                : packedLight;
    }
}
