package org.taumc.celeritas.mixin.features.cloud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.GL11;
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
    private static Identifier CLOUDS_LOCATION;

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

        Entity camera = this.minecraft.getCamera();
        float cameraY = (float)(camera.lastY + (camera.y - camera.lastY) * tickDelta);
        double cloudTime = this.ticks + tickDelta;
        double cameraX = (camera.prevX + (camera.x - camera.prevX) * tickDelta + cloudTime * 0.03F) / 12.0D;
        double cameraZ = (camera.prevZ + (camera.z - camera.prevZ) * tickDelta) / 12.0D + 0.33D;
        cameraX -= Math.floor(cameraX / 2048.0D) * 2048.0D;
        cameraZ -= Math.floor(cameraZ / 2048.0D) * 2048.0D;

        GlStateManager.disableCull();
        this.textureManager.bind(CLOUDS_LOCATION);
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        this.celeritas$cloudRenderer.render(cameraX, cameraZ,
                this.world.dimension.getCloudHeight() - cameraY + 0.33F,
                this.world.getCloudColor(tickDelta), pass);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        ci.cancel();
    }

    @Inject(method = "releaseGlLists", at = @At("HEAD"))
    private void celeritas$deleteCloudBuffers(CallbackInfo ci) {
        this.celeritas$cloudRenderer.delete();
    }
}
