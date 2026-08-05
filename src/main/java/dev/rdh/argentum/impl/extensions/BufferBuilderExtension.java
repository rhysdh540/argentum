package dev.rdh.argentum.impl.extensions;

public interface BufferBuilderExtension {
    default void argentum$appendTranslated(int[] vertices, float x, float y) {
        throw new UnsupportedOperationException();
    }
}
