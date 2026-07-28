package dev.rdh.cera;

import dev.rdh.cera.modules.BetterGrass;
import dev.rdh.cera.modules.NaturalTextures;
import dev.rdh.cera.modules.ctm.ConnectedTextures;

public interface CeraTextureAtlasExtension {
    BetterGrass cera$getBetterGrass();

    ConnectedTextures cera$getConnectedTextures();

    NaturalTextures cera$getNaturalTextures();
}
