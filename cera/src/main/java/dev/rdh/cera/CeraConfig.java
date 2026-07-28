package dev.rdh.cera;

import dev.rdh.cera.modules.BetterGrass;
import dev.rdh.cera.modules.ctm.ConnectedTextures;
import dev.rdh.cera.modules.DynamicLights;

public class CeraConfig {
    public BetterGrass.Mode betterGrass = BetterGrass.Mode.OFF;
    public ConnectedTextures.Mode connectedTextures = ConnectedTextures.Mode.OFF;
    public DynamicLights.Mode dynamicLights = DynamicLights.Mode.OFF;
    public boolean naturalTextures = false;

    public void validate() {
        if (this.betterGrass == null) this.betterGrass = BetterGrass.Mode.OFF;
        if (this.connectedTextures == null) this.connectedTextures = ConnectedTextures.Mode.OFF;
        if (this.dynamicLights == null) this.dynamicLights = DynamicLights.Mode.OFF;
    }
}
