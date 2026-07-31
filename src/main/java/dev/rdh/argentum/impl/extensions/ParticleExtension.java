package dev.rdh.argentum.impl.extensions;

public interface ParticleExtension {
    default boolean argentum$isVisible() {
        throw new UnsupportedOperationException();
    }
}
