package dev.rdh.argentum.impl.ext;

public interface BufferBuilderExtension {
    default void argentum$appendTranslated(int[] vertices, float x, float y) {
        throw new UnsupportedOperationException();
    }
}
