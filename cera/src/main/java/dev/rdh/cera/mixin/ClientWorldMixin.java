package dev.rdh.cera.mixin;

import dev.rdh.cera.CeraClientWorldExtension;
import dev.rdh.cera.modules.DynamicLights;
import net.minecraft.client.world.ClientWorld;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements CeraClientWorldExtension {
    @Unique
    private final DynamicLights cera$dynamicLights = new DynamicLights();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cera$loadDynamicLightRules(CallbackInfo ci) {
        this.cera$dynamicLights.reload(ResourceManager.client());
    }

    @Override
    public DynamicLights cera$getDynamicLights() {
        return this.cera$dynamicLights;
    }
}
