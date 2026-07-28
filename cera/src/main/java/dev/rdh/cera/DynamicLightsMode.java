package dev.rdh.cera;

public enum DynamicLightsMode {
    OFF,
    FAST,
    FANCY;

    public String key() {
        return "value.dynamic_lights." + name().toLowerCase();
    }
}
