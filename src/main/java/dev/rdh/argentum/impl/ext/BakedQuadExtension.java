package dev.rdh.argentum.impl.ext;

import net.minecraft.client.render.texture.TextureAtlasSprite;

public interface BakedQuadExtension {
    default void argentum$setSprite(TextureAtlasSprite sprite) {
        throw new UnsupportedOperationException();
    }
}
