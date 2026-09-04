package dev.rdh.argentum.impl.ext;

public interface BlockEntityLightHolder {
    /** {@return the cached light, or -1 if nothing valid is held for this generation} */
    default int argentum$getCachedLight(int generation) {
        return -1;
    }

    default void argentum$cacheLight(int light, int generation) {
    }
}
