package dev.rdh.argentum.impl.render.entity.instancing;

import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.debug.RenderMetrics;
import dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.resource.Identifier;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayDeque;

public final class EntityInstancing {
    private final ModelInstancer backend;
    private final ArrayDeque<EntityCapture> captures = new ArrayDeque<>();
    private final DeferredNameTags nameTags = new DeferredNameTags();
    private final Matrix4f arrowMatrix = new Matrix4f();
    private final Vector4f arrowColor = new Vector4f(1.0F);
    private final Vector4f arrowOverlayColor = new Vector4f(0.0F);

    private EntityCapture activeCapture;
    private int entityCount;
    private int playerCount;
    private String debugString = "Entity instancing: waiting";

    public EntityInstancing(ModelInstancer backend) {
        this.backend = backend;
    }

    public static EntityInstancing current() {
        ArgentumWorldRenderer renderer = ArgentumWorldRenderer.instanceNullable();
        return renderer == null ? null : renderer.getEntityInstancing();
    }

    public boolean beginBatch() {
        this.resetCaptures();
        this.nameTags.clear();
        this.entityCount = 0;
        this.playerCount = 0;
        if (!Argentum.CONFIG.entityInstancing) {
            this.debugString = "Entity instancing: disabled by config";
            return false;
        }
        if (!this.backend.beginBatch()) {
            this.debugString = "Entity instancing: unsupported";
            return false;
        }
        return true;
    }

    public boolean isBatchActive() {
        return this.backend.isBatchActive();
    }

    public EntityCapture beginEntity(Model model, Identifier texture, boolean player, boolean preserveFixedFunction,
            int packedLight, float effectTime, float overlayRed, float overlayGreen, float overlayBlue,
            float overlayAlpha) {
        if (!this.backend.isBatchActive() || model == null || texture == null) {
            return null;
        }
        EntityCapture capture = this.acquire();
        capture.beginEntity(model, texture, player, preserveFixedFunction, packedLight, effectTime,
                overlayRed, overlayGreen, overlayBlue, overlayAlpha
        );
        return capture;
    }

    public EntityCapture beginItemEntity(ItemEntity entity, BakedModel model, int packedLight) {
        if (!this.backend.isBatchActive() || entity.getItem() == null
                || !this.backend.supportsItem(model, entity.getItem())) {
            return null;
        }
        EntityCapture capture = this.acquire();
        capture.beginItem(entity, packedLight);
        return capture;
    }

    public boolean recordArrow(ArrowEntity arrow, double x, double y, double z, float tickDelta,
            Identifier texture, int packedLight) {
        if (this.activeCapture != null && this.activeCapture.isModelActive()) {
            return this.activeCapture.recordArrow(arrow, x, y, z, tickDelta, texture);
        }
        if (!this.backend.isBatchActive()) {
            return false;
        }
        this.transformArrow(this.arrowMatrix.identity(), arrow, x, y, z, tickDelta);
        if (!this.backend.submit(this.backend.arrow(), texture, InstanceRenderPass.CULL_BACK, this.arrowMatrix,
                packedLight, this.arrowColor, arrow.ticks + tickDelta, this.arrowOverlayColor)) {
            return false;
        }
        this.entityCount++;
        return true;
    }

    void transformArrow(Matrix4f matrix, ArrowEntity arrow, double x, double y, double z, float tickDelta) {
        matrix.translate((float)x, (float)y, (float)z)
                .rotateY((float)Math.toRadians(arrow.lastYaw + (arrow.yaw - arrow.lastYaw) * tickDelta - 90.0F))
                .rotateZ((float)Math.toRadians(arrow.lastPitch + (arrow.pitch - arrow.lastPitch) * tickDelta));
        float shake = arrow.shake - tickDelta;
        if (shake > 0.0F) {
            matrix.rotateZ((float)Math.toRadians(-(float)Math.sin(shake * 3.0F) * shake));
        }
        matrix.rotateX((float)Math.toRadians(45.0F)).scale(0.05625F).translate(-4.0F, 0.0F, 0.0F);
    }

    public void flush(CommandList commandList) {
        RenderMetrics.Category previous = RenderMetrics.setCategory(RenderMetrics.Category.ENTITY);
        ModelInstancer.BatchStats stats;
        try {
            stats = this.backend.flush(commandList);
        } finally {
            RenderMetrics.setCategory(previous);
        }
        this.debugString = "Entity instancing: %d entities (%d players) | %d parts | %d draws | %d textures".formatted(
                this.entityCount, this.playerCount, stats.instances(), stats.draws(), stats.textures()
        );
    }

    public void renderNameTags() {
        this.nameTags.render();
    }

    public void discardBatch() {
        this.resetCaptures();
        this.nameTags.clear();
        this.backend.discardBatch();
    }

    public String getDebugString() {
        return this.debugString;
    }

    ModelInstancer backend() {
        return this.backend;
    }

    EntityCapture activeCapture() {
        return this.activeCapture;
    }

    DeferredNameTags nameTags() {
        return this.nameTags;
    }

    void finish(EntityCapture capture) {
        if (capture != this.activeCapture) {
            return;
        }
        this.activeCapture = capture.previous();
        if (capture.recorded()) {
            this.entityCount++;
            if (capture.isPlayer()) {
                this.playerCount++;
            }
        }
        capture.markFinished();
    }

    void release(EntityCapture capture) {
        this.finish(capture);
        capture.markReleased();
        this.captures.addFirst(capture);
    }

    private EntityCapture acquire() {
        EntityCapture capture = this.captures.pollFirst();
        if (capture == null) {
            capture = new EntityCapture(this);
        }
        capture.open(this.activeCapture);
        this.activeCapture = capture;
        return capture;
    }

    private void resetCaptures() {
        while (this.activeCapture != null) {
            EntityCapture capture = this.activeCapture;
            this.activeCapture = capture.previous();
            capture.markReleased();
            this.captures.addFirst(capture);
        }
    }

    public static int packedLight(Entity entity, float tickDelta) {
        return entity.isOnFire() ? 0xF000F0 : entity.getLightLevel(tickDelta);
    }
}
