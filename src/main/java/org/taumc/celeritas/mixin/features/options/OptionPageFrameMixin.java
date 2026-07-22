package org.taumc.celeritas.mixin.features.options;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.embeddedt.embeddium.impl.gui.frame.OptionPageFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = OptionPageFrame.class, remap = false)
abstract class OptionPageFrameMixin {
    @ModifyReturnValue(method = "normalizeModForTooltip", at = @At("RETURN"))
    private static String celeritas$normalizeModId(String modId) {
        return "embeddium".equals(modId) ? "celeritas" : modId;
    }
}
