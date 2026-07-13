package org.taumc.celeritas.impl.extensions;

import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;

public interface SpriteExtension extends SpriteTransparencyLevel.Holder {
    void celeritas$markActive();

    boolean celeritas$shouldUpdate();
}
