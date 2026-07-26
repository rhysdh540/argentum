package dev.rdh.argentum.extras;

public class ArgentumExtrasConfig {
    public boolean explosionParticles = true;
    public boolean spellParticles = true;
    public boolean portalParticles = true;
    public boolean smokeAndFlameParticles = true;
    public boolean redstoneParticles = true;
    public boolean waterParticles = true;
    public boolean miscellaneousParticles = true;
    public boolean steadyDebugHud = true;
    public int debugHudRefreshIntervalMs = 250;
    public int debugHudScale = 0;
    public boolean fpsHud = false;
    public int dynamicFovStrength = 100;
    public int portalDistortionStrength = 100;
    public int viewBobbingStrength = 100;
    public int hurtCameraStrength = 100;
    public int cloudRenderDistance = 0;
    public boolean cloudFog = true;
    public int terrainFogDensity = 100;
    public int fluidFogDensity = 100;
    public int weatherRenderDistance = 0;
    public int weatherDensity = 100;

    public void validate() {
        this.debugHudRefreshIntervalMs = Math.max(0, this.debugHudRefreshIntervalMs);
        this.debugHudScale = Math.clamp(this.debugHudScale, 0, 4);
        this.dynamicFovStrength = Math.clamp(this.dynamicFovStrength, 0, 100);
        this.portalDistortionStrength = Math.clamp(this.portalDistortionStrength, 0, 100);
        this.viewBobbingStrength = Math.clamp(this.viewBobbingStrength, 0, 100);
        this.hurtCameraStrength = Math.clamp(this.hurtCameraStrength, 0, 100);
        this.cloudRenderDistance = Math.clamp(this.cloudRenderDistance, 0, 1536);
        this.terrainFogDensity = Math.clamp(this.terrainFogDensity, 0, 100);
        this.fluidFogDensity = Math.clamp(this.fluidFogDensity, 0, 100);
        this.weatherRenderDistance = Math.clamp(this.weatherRenderDistance, 0, 15);
        this.weatherDensity = Math.clamp(this.weatherDensity, 0, 100);
    }

}
