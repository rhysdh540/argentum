package dev.rdh.argentum.extras.mixin;

import dev.rdh.argentum.extras.ArgentumExtras;
import dev.rdh.argentum.extras.LeafQuality;
import net.minecraft.block.AbstractLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractLeavesBlock.class)
public class AbstractLeavesBlockMixin {
    @ModifyVariable(method = "setCulling", at = @At("HEAD"), argsOnly = true)
    private boolean argentumExtras$leafQuality(boolean culling) {
        return ArgentumExtras.CONFIG.leafQuality != LeafQuality.FAST;
    }
}
