package dev.rdh.cera;

public class CeraConfig {
    public BetterGrassMode betterGrass = BetterGrassMode.OFF;
    public boolean naturalTextures = false;

    public void validate() {
        if (this.betterGrass == null) this.betterGrass = BetterGrassMode.OFF;
    }
}
