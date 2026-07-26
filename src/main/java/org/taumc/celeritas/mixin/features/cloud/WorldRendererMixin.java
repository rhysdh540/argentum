package org.taumc.celeritas.mixin.features.cloud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.Celeritas;
import org.taumc.celeritas.impl.render.cloud.CloudRenderer;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private TextureManager textureManager;

    @Shadow
    private ClientWorld world;

    @Shadow
    private int ticks;

    @Unique
    private final CloudRenderer celeritas$cloudRenderer = new CloudRenderer();

    @Inject(method = "renderFancyClouds", at = @At("HEAD"), cancellable = true)
    private void celeritas$renderFancyClouds(float tickDelta, int pass, CallbackInfo ci) {
        if (!Celeritas.CONFIG.fasterClouds) {
            return;
        }

        this.celeritas$cloudRenderer.render(this.minecraft, this.textureManager, this.world, this.ticks, tickDelta, pass);
        ci.cancel();
    }

    @Inject(method = "releaseGlLists", at = @At("HEAD"))
    private void celeritas$deleteCloudBuffers(CallbackInfo ci) {
        this.celeritas$cloudRenderer.delete();
    }
}
