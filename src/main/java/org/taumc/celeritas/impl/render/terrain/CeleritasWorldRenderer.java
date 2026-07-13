package org.taumc.celeritas.impl.render.terrain;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.taumc.celeritas.impl.Celeritas;
import org.taumc.celeritas.impl.extensions.RenderGlobalExtension;
import org.taumc.celeritas.impl.render.terrain.matrix.PrimitiveChunkMatrixGetter;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

/**
 * Provides an extension to vanilla's world renderer.
 */
public class CeleritasWorldRenderer extends SimpleWorldRenderer<World, PrimitiveRenderSectionManager, Object, BlockEntity, Float> {

    /**
     * @return The CeleritasWorldRenderer based on the current dimension
     */
    public static CeleritasWorldRenderer instance() {
        var instance = instanceNullable();

        if (instance == null) {
            throw new IllegalStateException("No renderer attached to active world");
        }

        return instance;
    }

    /**
     * @return The CeleritasWorldRenderer based on the current dimension, or null if none is attached
     */
    public static CeleritasWorldRenderer instanceNullable() {
        var world = Minecraft.getInstance().worldRenderer;

        if (world instanceof RenderGlobalExtension extension) {
            return extension.sodium$getWorldRenderer();
        }

        return null;
    }

    @Override
    protected void loadWorld(World world) {
        super.loadWorld(world);
    }

    public static CameraState captureCameraState(double ticks) {
        Entity viewEntity = Minecraft.getInstance().getCamera();

        Objects.requireNonNull(viewEntity, "Client must have view entity");

        double x = viewEntity.lastX + (viewEntity.x - viewEntity.lastX) * ticks;
        double y = viewEntity.lastY + (viewEntity.y - viewEntity.lastY) * ticks + (double) viewEntity.getEyeHeight();
        double z = viewEntity.lastZ + (viewEntity.z - viewEntity.lastZ) * ticks;

        float pitch = viewEntity.pitch;
        float yaw = viewEntity.yaw;
        float fogDistance = ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff();

        return new CameraState(x, y, z, pitch, yaw, fogDistance);
    }

    @Override
    public int getEffectiveRenderDistance() {
        return Minecraft.getInstance().options.viewDistance;
    }

    @Override
    public int getMinimumBuildHeight() {
        return 0;
    }

    @Override
    public int getMaximumBuildHeight() {
        return this.world.getHeight();
    }

    @Override
    public String getChunksDebugString() {
        return super.getChunksDebugString() + "S: " + this.renderSectionManager.getSectionsWithSkyLight().size();
    }

    @Override
    protected ChunkRenderMatrices createChunkRenderMatrices() {
        return PrimitiveChunkMatrixGetter.getMatrices();
    }

    @Override
    protected PrimitiveRenderSectionManager createRenderSectionManager(CommandList commandList) {
        ChunkVertexType vertexType = Celeritas.CONFIG.compactVertexFormat ? ChunkMeshFormats.COMPACT : ChunkMeshFormats.VANILLA_LIKE;
        return PrimitiveRenderSectionManager.create(vertexType, this.world, this.renderDistance, commandList);
    }

    public boolean isEntityVisible(Entity entity) {
        if (!Celeritas.CONFIG.entityCulling) {
            return true;
        }

        var box = entity.getShape();
        if (!Double.isFinite(box.minX) || !Double.isFinite(box.minY) || !Double.isFinite(box.minZ)
                || !Double.isFinite(box.maxX) || !Double.isFinite(box.maxY) || !Double.isFinite(box.maxZ)) {
            return true;
        }

        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    @Override
    protected void renderBlockEntityList(List<BlockEntity> list, Float partialTicksBoxed) {
        float partialTicks = partialTicksBoxed;
        for (var blockEntity : list) {
            try {
                BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, partialTicks, -1);
            } catch(RuntimeException e) {
                if(blockEntity.isRemoved()) {
                    System.err.println("Suppressing crash from invalid tile entity");
                } else {
                    throw e;
                }
            }
        }
    }
}
