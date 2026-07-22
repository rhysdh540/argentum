package org.taumc.celeritas.impl.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class CeleritasConfig {
    private static final Logger LOGGER = LogManager.getLogger("CeleritasConfig");
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    public int chunkBuilderThreads;
    public boolean deferChunkUpdates = true;
    public AsyncOcclusionMode asyncOcclusion = AsyncOcclusionMode.EVERYTHING;
    public boolean fogCulling = true;
    public boolean entityCulling = true;
    public boolean entityInstancing = true;
    public boolean entityOcclusionCulling;
    public int entityOcclusionIntervalMs = 50;
    public boolean particleCulling = true;
    public boolean translucencySorting = true;
    public boolean animateOnlyVisibleTextures = true;
    public Set<String> renderPassDowngradeDenylist = new HashSet<>();
    public boolean safeChunkEdges = true;
    public boolean compactVertexFormat;
    public boolean checkGlErrors;

    public static CeleritasConfig load(Path path) {
        CeleritasConfig config = new CeleritasConfig();

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                CeleritasConfig loaded = GSON.fromJson(reader, CeleritasConfig.class);
                if (loaded == null) {
                    throw new IllegalArgumentException("Configuration is empty");
                }
                config = loaded;
            } catch (Exception e) {
                LOGGER.error("Could not load configuration from {}", path, e);
                return config;
            }
        }

        config.validate();

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save configuration to {}", path, e);
        }

        return config;
    }

    private void validate() {
        this.chunkBuilderThreads = Math.max(0, this.chunkBuilderThreads);
        this.entityOcclusionIntervalMs = Math.max(0, this.entityOcclusionIntervalMs);
        if (this.asyncOcclusion == null) {
            this.asyncOcclusion = AsyncOcclusionMode.EVERYTHING;
        }
        if (this.renderPassDowngradeDenylist == null) {
            this.renderPassDowngradeDenylist = new HashSet<>();
        }
    }
}
