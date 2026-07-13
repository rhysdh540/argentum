package org.taumc.celeritas.impl.extensions;

import net.minecraft.client.render.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.util.collections.quadtree.QuadTree;

public interface TextureAtlasExtension {
    QuadTree<TextureAtlasSprite> celeritas$getQuadTree();

    TextureAtlasSprite celeritas$findFromUV(float u, float v);
}
