package dev.rdh.cera.mixin.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin {
    @Shadow
    public abstract boolean hasColor(ItemStack stack);

    @ModifyReturnValue(method = "getColor", at = @At("RETURN"))
    private int cera$armorDefaultColor(int original, ItemStack stack) {
        if (this.hasColor(stack)) return original;
        return Minecraft.getInstance().cera$getCustomColors().getArmorDefaultColor(original);
    }
}
