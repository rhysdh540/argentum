package dev.rdh.cera.ext;

import dev.rdh.cera.modules.BetterGrass;
import dev.rdh.cera.modules.colors.CustomColormaps;
import dev.rdh.cera.modules.NaturalTextures;
import dev.rdh.cera.modules.ctm.ConnectedTextures;

public interface CeraTextureAtlasExtension {
    default BetterGrass cera$getBetterGrass() {
        throw new UnsupportedOperationException();
    }

    default CustomColormaps cera$getCustomColormaps() {
        throw new UnsupportedOperationException();
    }

    default ConnectedTextures cera$getConnectedTextures() {
        throw new UnsupportedOperationException();
    }

    default NaturalTextures cera$getNaturalTextures() {
        throw new UnsupportedOperationException();
    }
}
