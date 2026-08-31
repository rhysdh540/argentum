package dev.rdh.cera.mixin.colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.entity.particle.PortalParticle;
import net.minecraft.world.World;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.util.ColorU8;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PortalParticle.class)
public class PortalParticleMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void cera$portalColor(World world, double x, double y, double z, double vx, double vy, double vz, CallbackInfo ci) {
        int color = Minecraft.getInstance().cera$getCustomColors().getParticlePortalColor(-1);
        if (color >= 0) {
            ((Particle) (Object) this).setColor(
                    ColorU8.byteToNormalizedFloat(ColorARGB.unpackRed(color)),
                    ColorU8.byteToNormalizedFloat(ColorARGB.unpackGreen(color)),
                    ColorU8.byteToNormalizedFloat(ColorARGB.unpackBlue(color))
            );
        }
    }
}
