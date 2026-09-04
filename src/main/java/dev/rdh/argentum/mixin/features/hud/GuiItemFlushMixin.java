package dev.rdh.argentum.mixin.features.hud;

import dev.rdh.argentum.impl.render.hud.item.GuiItemIcons;

import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.platform.Lighting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GlStateManager.class, Lighting.class})
public class GuiItemFlushMixin {
    @Inject(method = {"disableDepthTest", "turnOff"}, at = @At("HEAD"))
    private static void argentum$flushBeforeOverlay(CallbackInfo ci) {
        GuiItemIcons.flush();
    }
}
