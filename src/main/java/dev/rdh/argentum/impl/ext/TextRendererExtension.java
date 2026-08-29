package dev.rdh.argentum.impl.ext;

public interface TextRendererExtension {
    default void argentum$beginBatch(Runnable beforeImmediateText) {
        throw new UnsupportedOperationException();
    }

    default void argentum$endBatch() {
        throw new UnsupportedOperationException();
    }

    /** Drops cached text geometry so it re-bakes (e.g. after the §-code color palette changes). */
    default void argentum$invalidateTextCache() {
        throw new UnsupportedOperationException();
    }
}
