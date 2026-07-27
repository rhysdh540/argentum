package dev.rdh.argentum.mixin.features.options;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.gui.VideoOptionsScreen;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    private void celeritas$openVideoOptions(ButtonWidget button, CallbackInfo ci) {
        if (button.active && button.id == 101) {
            this.minecraft.options.save();
            this.minecraft.openScreen(new VideoOptionsScreen(this));
            ci.cancel();
        }
    }
}
