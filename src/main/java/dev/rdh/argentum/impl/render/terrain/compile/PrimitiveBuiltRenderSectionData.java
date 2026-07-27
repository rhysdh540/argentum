package dev.rdh.argentum.impl.render.terrain.compile;

import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.texture.TextureAtlasSprite;

import java.util.Objects;

public class PrimitiveBuiltRenderSectionData extends MinecraftBuiltRenderSectionData<TextureAtlasSprite, BlockEntity> {
    public boolean hasSkyLight;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PrimitiveBuiltRenderSectionData that = (PrimitiveBuiltRenderSectionData) o;
        return hasSkyLight == that.hasSkyLight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hasSkyLight);
    }
}
