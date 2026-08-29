package dev.rdh.cera.mixin;

import dev.rdh.cera.ext.CeraMinecraftExtension;
import dev.rdh.cera.modules.colors.CustomColors;
import dev.rdh.cera.modules.DynamicLights;
import dev.rdh.cera.modules.OptifineCosmetics;
import dev.rdh.cera.modules.cit.CustomItems;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin implements CeraMinecraftExtension {
    @Unique
    private final DynamicLights.Rules cera$dynamicLightRules = new DynamicLights.Rules();

    @Unique
    private final CustomItems cera$customItems = new CustomItems();
    @Unique
    private final OptifineCosmetics cera$optifineCosmetics = new OptifineCosmetics();
    @Unique
    private final CustomColors cera$customColors = new CustomColors();

    @Override
    public DynamicLights.Rules cera$getDynamicLightRules() {
        return this.cera$dynamicLightRules;
    }

    @Override
    public CustomColors cera$getCustomColors() {
        return this.cera$customColors;
    }

    @Override
    public CustomItems cera$getCustomItems() {
        return this.cera$customItems;
    }

    @Override
    public OptifineCosmetics cera$getOptifineCosmetics() {
        return this.cera$optifineCosmetics;
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void cera$clearCustomSkyTextures(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft)(Object)this;
        if (minecraft.worldRenderer != null) {
            minecraft.worldRenderer.cera$getCustomSky().texturesReloading(minecraft.getTextureManager());
        }
    }
}
