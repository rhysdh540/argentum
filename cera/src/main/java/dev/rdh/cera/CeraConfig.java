package dev.rdh.cera;

public class CeraConfig {
    public BetterGrass.Mode betterGrass = BetterGrass.Mode.OFF;
    public DynamicLights.Mode dynamicLights = DynamicLights.Mode.OFF;
    public boolean naturalTextures = false;

    public void validate() {
        if (this.betterGrass == null) this.betterGrass = BetterGrass.Mode.OFF;
        if (this.dynamicLights == null) this.dynamicLights = DynamicLights.Mode.OFF;
    }
}
