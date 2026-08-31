package dev.rdh.cera.mixin.colors;

import net.minecraft.block.Blocks;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.entity.particle.RedstoneParticle;
import net.minecraft.util.math.BlockPos;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedstoneParticle.class)
public class RedstoneParticleMixin extends Particle {
    private RedstoneParticleMixin() {
        super(null, 0, 0, 0);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cera$reddustColor(CallbackInfo ci) {
		BlockPos pos = new BlockPos(this);
        BlockState state = this.world.getBlockState(pos);
        if (state.getBlock() != Blocks.REDSTONE_WIRE) return;
        int level = state.getBlock().getMetadataFromState(state);
        int color = Minecraft.getInstance().getBlocksAtlas().cera$getCustomColormaps().redstoneColor(level);
        if (color >= 0) {
            this.setColor(ColorARGB.unpackRed(color) / 255.0F, ColorARGB.unpackGreen(color) / 255.0F, ColorARGB.unpackBlue(color) / 255.0F);
        }
    }
}
