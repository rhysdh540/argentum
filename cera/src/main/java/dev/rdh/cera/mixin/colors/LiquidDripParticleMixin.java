package dev.rdh.cera.mixin.colors;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.particle.LiquidDripParticle;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidDripParticle.class)
public class LiquidDripParticleMixin extends Particle {
    @Shadow
    private Material material;

    private LiquidDripParticleMixin() {
        super(null, 0, 0, 0);
    }

    // tick() re-derives the drip color from its material every frame, so override here rather than in the constructor.
    @Inject(method = "tick", at = @At("TAIL"))
    private void cera$dripColor(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int color;
        if (this.material == Material.WATER) {
			BlockPos pos = new BlockPos(this);
            Biome biome = this.world.getBiome(pos);
            int custom = mc.getBlocksAtlas().cera$getCustomColormaps().waterColor(biome, pos);
            int mult = mc.cera$getCustomColors().getParticleWaterColor(-1);
            if (custom < 0 && mult < 0) return;
            int biomeWater = custom >= 0 ? custom : biome.waterFogColor;
            color = mult >= 0 ? multiply(biomeWater, mult) : biomeWater;
        } else if (this.material == Material.LAVA) {
            var colors = mc.cera$getCustomColors();
            color = colors.getLavaDropColor(this.age, colors.getParticleLavaColor(-1));
            if (color < 0) return;
        } else {
            return;
        }
        this.setColor((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }

    @Unique
    private static int multiply(int a, int b) {
        int r = (a >> 16 & 0xFF) * (b >> 16 & 0xFF) / 255;
        int g = (a >> 8 & 0xFF) * (b >> 8 & 0xFF) / 255;
        int bl = (a & 0xFF) * (b & 0xFF) / 255;
        return r << 16 | g << 8 | bl;
    }
}
