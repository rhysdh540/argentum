package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.block.BlockLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public class BlockMixin {
    @ModifyReturnValue(method = "getRenderLayer", at = @At("RETURN"))
    private BlockLayer cera$customBlockLayer(BlockLayer original) {
        BlockLayer custom = Minecraft.getInstance().cera$getCustomBlockLayers().get((Block)(Object)this);
        return custom == null ? original : custom;
    }
}
