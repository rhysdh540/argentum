package dev.rdh.argentum.mixin.features.texture;

import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.metadata.AnimationMetadata;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.rdh.argentum.impl.extensions.SpriteExtension;

import java.awt.image.BufferedImage;
import java.util.List;

@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin implements SpriteExtension {
    @Shadow
    protected List<int[][]> frames;

    private boolean celeritas$active;
    private SpriteTransparencyLevel celeritas$transparency = SpriteTransparencyLevel.TRANSLUCENT;

    @Inject(method = "load", at = @At("RETURN"))
    private void celeritas$analyzeTransparency(BufferedImage[] images, AnimationMetadata animation, CallbackInfo ci) {
        SpriteTransparencyLevel level = SpriteTransparencyLevel.OPAQUE;
        for (int[][] frame : this.frames) {
            if (frame == null || frame.length == 0 || frame[0] == null) continue;
            for (int color : frame[0]) {
                int alpha = color >>> 24;
                if (alpha != 255) {
                    level = level.chooseNextLevel(alpha == 0
                            ? SpriteTransparencyLevel.TRANSPARENT
                            : SpriteTransparencyLevel.TRANSLUCENT);
                    if (level == SpriteTransparencyLevel.TRANSLUCENT) break;
                }
            }
            if (level == SpriteTransparencyLevel.TRANSLUCENT) break;
        }
        this.celeritas$transparency = level;
    }

    @Inject(method = {"getUMin", "getU"}, at = @At("RETURN"))
    private void celeritas$markUsed(CallbackInfoReturnable<Float> cir) {
        this.celeritas$active = true;
    }

    @Override
    public void celeritas$markActive() {
        this.celeritas$active = true;
    }

    @Override
    public boolean celeritas$shouldUpdate() {
        boolean active = this.celeritas$active;
        this.celeritas$active = false;
        return active;
    }

    @Override
    public SpriteTransparencyLevel embeddium$getTransparencyLevel() {
        return this.celeritas$transparency;
    }
}
