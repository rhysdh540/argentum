package dev.rdh.cera.mixin;

import dev.rdh.cera.ext.CeraClientWorldExtension;
import dev.rdh.cera.ext.CeraMinecraftExtension;
import dev.rdh.cera.modules.DynamicLights;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements CeraClientWorldExtension {
    @Unique
    private final DynamicLights cera$dynamicLights = new DynamicLights(
            ((CeraMinecraftExtension)Minecraft.getInstance()).cera$getDynamicLightRules());

    @Override
    public DynamicLights cera$getDynamicLights() {
        return this.cera$dynamicLights;
    }
}
