package dev.rdh.argentum.impl.ext;

public interface ParticleExtension {
    default boolean argentum$isVisible() {
        throw new UnsupportedOperationException();
    }
}
