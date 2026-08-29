package dev.rdh.cera.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GameGui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GameGui.class)
public class GameGuiMixin {
    @ModifyExpressionValue(method = "renderXpBar", at = @At(value = "CONSTANT", args = "intValue=8453920"))
    private int cera$xpBarTextColor(int color) {
        return Minecraft.getInstance().cera$getCustomColors().getExpBarTextColor(color);
    }

    @ModifyExpressionValue(method = "renderBossBars", at = @At(value = "CONSTANT", args = "intValue=16777215"))
    private int cera$bossTextColor(int color) {
        return Minecraft.getInstance().cera$getCustomColors().getBossTextColor(color);
    }
}
