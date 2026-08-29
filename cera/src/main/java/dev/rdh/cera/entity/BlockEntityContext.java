package dev.rdh.cera.entity;

import net.minecraft.block.entity.BlockEntity;

/**
 * Tracks which block entity is currently being rendered. Owned by the {@code BlockEntityRenderDispatcher}.
 */
public final class BlockEntityContext {
    private BlockEntity current;

    public BlockEntity current() {
        return this.current;
    }

    public void begin(BlockEntity blockEntity) {
        this.current = blockEntity;
    }

    public void end(BlockEntity blockEntity) {
        if (this.current == blockEntity) this.current = null;
    }
}
