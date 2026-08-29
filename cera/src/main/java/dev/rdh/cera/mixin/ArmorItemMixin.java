package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmorItem.class)
public class ArmorItemMixin {
    // Only override the undyed default color; dyed leather keeps its stored NBT color.
    @ModifyReturnValue(method = "getColor", at = @At("RETURN"))
    private int cera$armorDefaultColor(int original, ItemStack stack) {
        if (((ArmorItem) (Object) this).hasColor(stack)) return original;
        return Minecraft.getInstance().cera$getCustomColors().getArmorDefaultColor(original);
    }
}
