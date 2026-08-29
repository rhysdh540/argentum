package dev.rdh.cera.mixin;

import dev.rdh.cera.entity.BlockEntityContext;
import dev.rdh.cera.ext.CeraBlockEntityRenderDispatcherExtension;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin implements CeraBlockEntityRenderDispatcherExtension {
    @Unique
    private final BlockEntityContext cera$blockEntityContext = new BlockEntityContext();

    @Override
    public BlockEntityContext cera$getBlockEntityContext() {
        return this.cera$blockEntityContext;
    }

    @Inject(
            method = "render(Lnet/minecraft/block/entity/BlockEntity;DDDFI)V",
            at = @At("HEAD")
    )
    private void cera$beginBlockEntityRender(BlockEntity blockEntity, double x, double y, double z, float tickDelta, int breakStage, CallbackInfo ci) {
        this.cera$blockEntityContext.begin(blockEntity);
    }

    @Inject(
            method = "render(Lnet/minecraft/block/entity/BlockEntity;DDDFI)V",
            at = @At("RETURN")
    )
    private void cera$endBlockEntityRender(BlockEntity blockEntity, double x, double y, double z, float tickDelta, int breakStage, CallbackInfo ci) {
        this.cera$blockEntityContext.end(blockEntity);
    }
}
