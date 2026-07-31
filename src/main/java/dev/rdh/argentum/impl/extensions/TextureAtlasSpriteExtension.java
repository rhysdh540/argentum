package dev.rdh.argentum.impl.extensions;

import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;

public interface TextureAtlasSpriteExtension extends SpriteTransparencyLevel.Holder {
    void celeritas$markActive();

    boolean celeritas$shouldUpdate();
}
