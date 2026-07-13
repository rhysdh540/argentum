package org.taumc.celeritas.impl.render.terrain.sprite;

import net.minecraft.client.render.texture.TextureAtlasSprite;
import org.taumc.celeritas.impl.extensions.SpriteExtension;

public final class SpriteUtil {
    private SpriteUtil() {
    }

    public static void markActive(TextureAtlasSprite sprite) {
        ((SpriteExtension)sprite).celeritas$markActive();
    }
}
