package dev.rdh.argentum.mixin.features.texture;

import com.google.common.collect.Iterators;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.util.collections.quadtree.QuadTree;
import org.embeddedt.embeddium.impl.util.collections.quadtree.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.ext.TextureAtlasExtension;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtension {
    @Shadow @Final
    private Map<String, TextureAtlasSprite> stitchedSprites;

    private QuadTree<TextureAtlasSprite> celeritas$quadTree;
    private int celeritas$width;
    private int celeritas$height;

    @Inject(method = "loadAndStitch", at = @At("RETURN"))
    private void celeritas$buildLookup(CallbackInfo ci) {
        int width = 0;
        int height = 0;
        int minSize = Integer.MAX_VALUE;
        for (TextureAtlasSprite sprite : this.stitchedSprites.values()) {
            width = Math.max(width, sprite.getX() + sprite.getWidth());
            height = Math.max(height, sprite.getY() + sprite.getHeight());
            minSize = Math.min(minSize, Math.max(sprite.getWidth(), sprite.getHeight()));
        }
        this.celeritas$width = nextPowerOfTwo(width);
        this.celeritas$height = nextPowerOfTwo(height);
        Rect2i bounds = new Rect2i(0, 0, this.celeritas$width, this.celeritas$height);
        this.celeritas$quadTree = new QuadTree<>(bounds, minSize, this.stitchedSprites.values(),
                sprite -> new Rect2i(sprite.getX(), sprite.getY(), sprite.getWidth(), sprite.getHeight())
        );
    }

    @Redirect(method = "bindAndTick", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<TextureAtlasSprite> celeritas$visibleAnimations(List<TextureAtlasSprite> sprites) {
        Iterator<TextureAtlasSprite> iterator = sprites.iterator();
        return Argentum.CONFIG.animateOnlyVisibleTextures
                ? Iterators.filter(iterator, TextureAtlasSprite::argentum$shouldUpdate)
                : iterator;
    }

    @Override
    public TextureAtlasSprite argentum$findFromUV(float u, float v) {
        QuadTree<TextureAtlasSprite> quadTree = this.celeritas$quadTree;
        return quadTree == null ? null
                : quadTree.find(Math.round(u * this.celeritas$width), Math.round(v * this.celeritas$height));
    }

    private static int nextPowerOfTwo(int value) {
        return value <= 1 ? 1 : Integer.highestOneBit(value - 1) << 1;
    }
}
