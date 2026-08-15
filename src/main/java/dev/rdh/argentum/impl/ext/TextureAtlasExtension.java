package dev.rdh.argentum.impl.ext;

import net.minecraft.client.render.texture.TextureAtlasSprite;

public interface TextureAtlasExtension {
    default TextureAtlasSprite argentum$findFromUV(float u, float v) {
        throw new UnsupportedOperationException();
    }
}
