package dev.rdh.argentum.impl.ext;

import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;

public interface TextureAtlasSpriteExtension extends SpriteTransparencyLevel.Holder {
    default void argentum$markActive() {
        throw new UnsupportedOperationException();
    }

    default boolean argentum$shouldUpdate() {
        throw new UnsupportedOperationException();
    }

    @Override
    default SpriteTransparencyLevel embeddium$getTransparencyLevel() {
        throw new UnsupportedOperationException();
    }
}
