package dev.rdh.argentum.mixin.core.render;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.rdh.argentum.impl.debug.DebugStrings;

import net.minecraft.client.gui.overlay.DebugOverlay;
import net.minecraft.text.Formatting;

import java.util.List;

@Mixin(DebugOverlay.class)
public class DebugOverlayMixin {
    @Inject(method = "getSystemInfo", at = @At("RETURN"))
    private void appendArgentumSystemInfo(CallbackInfoReturnable<List<String>> cir) {
        var strings = cir.getReturnValue();
        strings.add("");
        strings.addAll(DebugStrings.getStringsToRender().stream()
                .map(pair -> pair.right().toString() + pair.left()).toList()
        );
    }
}
