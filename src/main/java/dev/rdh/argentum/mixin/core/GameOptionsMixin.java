package dev.rdh.argentum.mixin.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.options.GameOptions;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.PrintWriter;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Shadow
    public boolean useVbo;

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at = @At("RETURN"))
    private void argentum$enableVbos(Minecraft minecraft, File directory, CallbackInfo ci) {
        this.useVbo = true;
    }

    @WrapWithCondition(
            method = "load",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;useVbo:Z", opcode = Opcodes.PUTFIELD)
    )
    private boolean argentum$skipReadingVbo(GameOptions instance, boolean value) {
        return false;
    }

    @WrapWithCondition(
            method = "save",
            at = @At(value = "INVOKE", target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V", ordinal = 0),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=useVbo:")
            )
    )
    private boolean argentum$skipWritingVbo(PrintWriter instance, String x) {
        return false;
    }

    @Redirect(method = "set(Lnet/minecraft/client/options/GameOptions$Option;I)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;useVbo:Z", opcode = 181))
    private void argentum$keepVbosEnabled(GameOptions options, boolean value) {
        options.useVbo = true;
    }
}
