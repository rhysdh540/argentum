package org.taumc.celeritas.mixin.features.model.instancing;

import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.resource.model.BakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.instancing.EntityInstancingRenderer;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/resource/model/BakedModel;ILnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void celeritas$instanceHeldItem(BakedModel model, int color, ItemStack item, CallbackInfo ci) {
        if (EntityInstancingRenderer.recordItem(model, item, color)) {
            ci.cancel();
        }
    }
}
