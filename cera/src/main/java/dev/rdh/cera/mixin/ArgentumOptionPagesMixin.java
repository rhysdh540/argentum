package dev.rdh.cera.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import dev.rdh.argentum.impl.gui.ArgentumOptionPages;
import dev.rdh.cera.Cera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.OptionImpl;

// hd fonts requires floating-point char widths, which only the batching text engine supports; so we force that on here
@Mixin(value = ArgentumOptionPages.class, remap = false)
public class ArgentumOptionPagesMixin {
    @ModifyExpressionValue(method = "toggle", at = @At(value = "INVOKE", target = "Lorg/taumc/celeritas/api/options/structure/OptionImpl$Builder;setFlags([Lorg/taumc/celeritas/api/options/structure/OptionFlag;)Lorg/taumc/celeritas/api/options/structure/OptionImpl$Builder;"))
    private static OptionImpl.Builder<?, ?> cera$lockFontBatching(OptionImpl.Builder<?, ?> builder, @Local(argsOnly = true) OptionIdentifier<?> id) {
        if ("font_batching".equals(id.getPath())) {
            builder.setEnabledPredicate(() -> !Cera.CONFIG.hdFonts);
        }
        return builder;
    }
}
