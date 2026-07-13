package org.taumc.celeritas.mixin.core.render;

import com.llamalad7.mixinextras.sugar.Local;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.extensions.RenderGlobalExtension;
import org.taumc.celeritas.impl.render.entity.EntityGatherer;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mixin(value = WorldRenderer.class, priority = 900)
public abstract class WorldRendererMixin implements RenderGlobalExtension {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private int lastViewDistance;

    @Shadow
    public abstract void reload();

    @Shadow
    private ClientWorld world;
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    private int renderedEntityCount;

    private CeleritasWorldRenderer renderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.renderer = new CeleritasWorldRenderer();
    }

    @Override
    public CeleritasWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(@Coerce World world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }


    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int render(BlockLayer layer, double ticks, int anaglyphRenderPass, Entity viewEntity) {
        RenderDevice.enterManagedCode();

        Lighting.turnOff();

        double d3 = viewEntity.prevX + (viewEntity.x - viewEntity.prevX) * ticks;
        // Do not apply eye height here or weird offsets will happen
        double d4 = viewEntity.prevY + (viewEntity.y - viewEntity.prevY) * ticks;
        double d5 = viewEntity.prevZ + (viewEntity.z - viewEntity.prevZ) * ticks;

        this.minecraft.gameRenderer.enableLightMap();

        try {
            this.renderer.drawChunkLayer(layer, d3, d4, d5);
        } finally {
            RenderDevice.exitManagedCode();
        }

        this.minecraft.gameRenderer.disableLightMap();

        return 1;
    }

    @Unique
    private int frame = 0;

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setupRender(Entity camera, double tickDelta, Culler culler, int frame, boolean loadChunks) {
        if (this.minecraft.options.viewDistance != this.lastViewDistance) {
            this.reload();
        }

        updateFrustums(culler, (float)tickDelta);
    }

    public void updateFrustums(Culler camera, float tick) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setupTerrain(((ViewportProvider)camera).sodium$createViewport(),
                    CeleritasWorldRenderer.captureCameraState(tick),
                    this.frame++, this.minecraft.player.noClip, false);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void markDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @author embeddedt
     * @reason Disable vanilla chunk compilation
     */
    @Overwrite
    public void compileChunksUntil(long time) {

    }

    @Inject(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/world/WorldRenderer;globalBlockEntities:Ljava/util/Set;", ordinal = 0, opcode = Opcodes.GETFIELD))
    public void sodium$renderTileEntities(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) float partialTicks) {
        this.renderer.renderBlockEntities(partialTicks);
    }

    private final EntityGatherer celeritas$entityGatherer = new EntityGatherer();

    @Inject(method = "renderEntities", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=entities"))
    private void celeritas$renderEntities(Entity camera, Culler culler, float tickDelta, CallbackInfo ci, @Local(ordinal = 0) double d, @Local(ordinal = 1) double e, @Local(ordinal = 2) double g) {
        celeritas$entityGatherer.clear();
        var entityList = celeritas$entityGatherer.getLoadedEntityList(this.world);
        this.renderer.prepareEntityCulling(entityList, camera, d, e, g);

        BlockPos.Mutable entityBlockPos = new BlockPos.Mutable();

        for (Entity entity : entityList) {
            if ((!this.entityRenderDispatcher.shouldRender(entity, culler, d, e, g) || !this.renderer.isEntityVisible(entity))
                    && entity.rider != this.minecraft.player) {
                if (entity instanceof WitherSkullEntity) {
                    this.minecraft.getEntityRenderDispatcher().renderNameTag(entity, tickDelta);
                }
                continue;
            }

            boolean isSelfSleeping = this.minecraft.getCamera() instanceof LivingEntity le && le.isSleeping();
            if (entity == this.minecraft.getCamera() && this.minecraft.options.perspective == 0 && !isSelfSleeping) {
                continue;
            }

            if (entity.y >= 0.0 && entity.y < 256.0) {
                entityBlockPos.set(MathHelper.floor(entity.x), MathHelper.floor(entity.y), MathHelper.floor(entity.z));
                if (!this.world.isChunkLoaded(entityBlockPos)) {
                    continue;
                }
            }

            this.renderedEntityCount++;
            this.entityRenderDispatcher.render(entity, tickDelta);
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0))
    private Iterator<?> celeritas$skipVanillaEntityChunks(List<?> chunks) {
        return Collections.emptyIterator();
    }

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String getChunkDebugInfo() {
        return this.renderer.getChunksDebugString();
    }

}
