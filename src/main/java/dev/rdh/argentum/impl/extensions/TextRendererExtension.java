package dev.rdh.argentum.impl.extensions;

public interface TextRendererExtension {
    default void argentum$beginBatch(Runnable beforeImmediateText) {
        throw new UnsupportedOperationException();
    }

    default void argentum$endBatch() {
        throw new UnsupportedOperationException();
    }
}
