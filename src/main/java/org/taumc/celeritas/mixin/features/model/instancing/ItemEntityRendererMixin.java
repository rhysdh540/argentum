package org.taumc.celeritas.mixin.features.model.instancing;

import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Shadow
    @Final
    private ItemRenderer itemRenderer;

    @Unique
    private boolean celeritas$instancing;

    @Inject(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V", at = @At("HEAD"))
    private void celeritas$beginItemEntity(ItemEntity entity, double x, double y, double z,
            float yaw, float tickDelta, CallbackInfo ci) {
        BakedModel model = this.itemRenderer.getModelShaper().getModel(entity.getItem());
        this.celeritas$instancing = EntityInstancingRenderer.beginItemEntity(entity, model);
    }

    @Inject(
            method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;DDDFF)V")
    )
    private void celeritas$endItemEntity(ItemEntity entity, double x, double y, double z,
            float yaw, float tickDelta, CallbackInfo ci) {
        if (this.celeritas$instancing) {
            EntityInstancingRenderer.endItemEntity();
            this.celeritas$instancing = false;
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V", at = @At("RETURN"))
    private void celeritas$finishItemEntity(ItemEntity entity, double x, double y, double z,
            float yaw, float tickDelta, CallbackInfo ci) {
        if (this.celeritas$instancing) {
            EntityInstancingRenderer.endItemEntity();
            this.celeritas$instancing = false;
        }
    }
}
