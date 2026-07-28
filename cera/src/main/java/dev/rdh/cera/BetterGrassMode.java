package dev.rdh.cera;

public enum BetterGrassMode {
    OFF,
    FAST,
    FANCY;

    public String key() {
        return "value.better_grass." + this.name().toLowerCase();
    }
}
