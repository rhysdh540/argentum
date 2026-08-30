package dev.rdh.cera.mixin.colors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LilyPadBlock.class)
public class LilyPadBlockMixin {
    @ModifyReturnValue(method = "getColor(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;I)I", at = @At("RETURN"))
    private int cera$lilypadColor(int original) {
        return Minecraft.getInstance().cera$getCustomColors().getLilypadColor(original);
    }
}
