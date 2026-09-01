package dev.rdh.argentum.impl.ext;

public interface WorldChunkExtension {
    default boolean argentum$hasEntities() {
        throw new UnsupportedOperationException();
    }
}
