package org.taumc.celeritas.impl.config;

import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import java.util.HashSet;
import java.util.Set;

public class CeleritasConfig {
    public int chunkBuilderThreads = 0;
    public boolean deferChunkUpdates = true;
    public AsyncOcclusionMode asyncOcclusion = AsyncOcclusionMode.EVERYTHING;
    public boolean fogCulling = true;
    public boolean entityCulling = true;
    public boolean entityInstancing = true;
    public int entityOcclusionIntervalMs = 50;
    public boolean particleCulling = true;
    public boolean translucencySorting = true;
    public boolean animateOnlyVisibleTextures = true;
    public int biomeBlendRadius = 3;
    public Set<String> renderPassDowngradeDenylist = new HashSet<>();
    public boolean safeChunkEdges = true;
    public boolean compactVertexFormat = false;
    public boolean checkGlErrors = false;
    public boolean fontBatching = true;

	public void validate() {
        this.chunkBuilderThreads = Math.max(0, this.chunkBuilderThreads);
        this.entityOcclusionIntervalMs = Math.max(0, this.entityOcclusionIntervalMs);
        this.biomeBlendRadius = Math.clamp(this.biomeBlendRadius, 0, 14);
        if (this.asyncOcclusion == null) {
            this.asyncOcclusion = AsyncOcclusionMode.EVERYTHING;
        }
        if (this.renderPassDowngradeDenylist == null) {
            this.renderPassDowngradeDenylist = new HashSet<>();
        }
    }
}
