package dev.rdh.cera.mixin.colors;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.particle.LiquidDripParticle;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.util.ColorMixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
            color = mult >= 0 ? ColorMixer.mul(biomeWater, mult) : biomeWater;
        } else if (this.material == Material.LAVA) {
            var colors = mc.cera$getCustomColors();
            color = colors.getLavaDropColor(this.age, colors.getParticleLavaColor(-1));
            if (color < 0) return;
        } else {
            return;
        }
        this.setColor(ColorARGB.unpackRed(color) / 255.0F, ColorARGB.unpackGreen(color) / 255.0F, ColorARGB.unpackBlue(color) / 255.0F);
    }
}
