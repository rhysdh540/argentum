package dev.rdh.argentum.extras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.rdh.argentum.extras.ArgentumExtras;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
	@Shadow
	private ButtonWidget realmsButton;

	@Shadow
	private boolean triedRealmsInit;

	@Inject(method = "init", at = @At("HEAD"))
	private void disableRealms(CallbackInfo ci) {
		this.triedRealmsInit = ArgentumExtras.CONFIG.disableRealms;
	}

	@Inject(method = "initWidgetsNormal", at = @At("RETURN"))
	private void alsoDisableRealms(CallbackInfo ci) {
		this.realmsButton.visible = !ArgentumExtras.CONFIG.disableRealms;
	}
}