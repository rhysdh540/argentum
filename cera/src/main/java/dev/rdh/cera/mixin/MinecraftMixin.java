package dev.rdh.cera.mixin;

import dev.rdh.cera.ext.CeraMinecraftExtension;
import dev.rdh.cera.modules.DynamicLights;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Minecraft.class)
public class MinecraftMixin implements CeraMinecraftExtension {
    @Unique
    private final DynamicLights.Rules cera$dynamicLightRules = new DynamicLights.Rules();

    @Override
    public DynamicLights.Rules cera$getDynamicLightRules() {
        return this.cera$dynamicLightRules;
    }
}
