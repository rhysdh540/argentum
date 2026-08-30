package dev.rdh.cera.mixin.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin {
    @ModifyReturnValue(method = "getDisplayColor", at = @At("RETURN"))
    private int cera$spawnEggColor(int original, ItemStack stack, int layer) {
        return Minecraft.getInstance().cera$getCustomColors().getSpawnEggColor(stack.getMetadata(), layer, original);
    }
}
