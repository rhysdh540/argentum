package dev.rdh.cera.ext;

import dev.rdh.cera.entity.BlockEntityContext;

public interface CeraBlockEntityRenderDispatcherExtension {
    default BlockEntityContext cera$getBlockEntityContext() {
        throw new UnsupportedOperationException();
    }
}
