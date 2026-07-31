package dev.rdh.argentum.impl.extensions;

import dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer;

public interface WorldRendererExtension {
    default ArgentumWorldRenderer argentum$getWorldRenderer() {
        throw new UnsupportedOperationException();
    }
}
