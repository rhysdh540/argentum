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

    public void validate() {
        this.debugHudRefreshIntervalMs = Math.max(0, this.debugHudRefreshIntervalMs);
    }
}
