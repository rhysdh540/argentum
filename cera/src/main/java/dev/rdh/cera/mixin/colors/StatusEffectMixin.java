package dev.rdh.cera.mixin.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StatusEffect.class)
public class StatusEffectMixin {
    @ModifyReturnValue(method = "getPotionColor", at = @At("RETURN"))
    private int cera$potionColor(int original) {
        return Minecraft.getInstance().cera$getCustomColors().getPotionColor(((StatusEffect) (Object) this).getId(), original);
    }
}
