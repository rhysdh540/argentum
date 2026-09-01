package dev.rdh.cera.modules;

import dev.rdh.cera.Cera;
import dev.rdh.cera.props.Props;

import net.minecraft.resource.Identifier;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReloadListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CustomPanorama implements ResourceReloadListener {
    private static final int MAX_FOLDERS = 100;

    private final Random random = new Random();
    private volatile Panorama active;

    public Panorama active() {
        return Cera.CONFIG.customPanorama ? this.active : null;
    }

    @Override
    public void resourcesReloaded(ResourceManager resources) {
        List<Panorama> candidates = new ArrayList<>();
        for (int i = 0; i < MAX_FOLDERS; i++) {
            String folder = "optifine/gui/background" + i;
            if (resources.getResource(new Identifier(folder + "/panorama_0.png")).isEmpty()) continue;
            candidates.add(Panorama.parse(resources, folder));
        }

        if (candidates.isEmpty()) {
            this.active = null;
            return;
        }

        int total = 1;
        for (Panorama candidate : candidates) total += candidate.weight();
        int roll = this.random.nextInt(total) - 1;
        Panorama chosen = null;
        for (Panorama candidate : candidates) {
            if (roll < 0) break;
            roll -= candidate.weight();
            if (roll < 0) {
                chosen = candidate;
                break;
            }
        }

        this.active = chosen;
        Cera.LOGGER.info("[CustomPanorama] {} alternative panoramas, using {}",
                candidates.size(), chosen == null ? "vanilla" : chosen.folder());
    }

    public record Panorama(
            String folder, Identifier[] textures, int weight,
            int blur1, int blur2, int blur3,
            int overlay1Top, int overlay1Bottom, int overlay2Top, int overlay2Bottom
    ) {

        private static Panorama parse(ResourceManager resources, String folder) {
            Identifier[] textures = new Identifier[6];
            for (int i = 0; i < textures.length; i++) {
                textures[i] = new Identifier(folder + "/panorama_" + i + ".png");
            }

            Props props = null;
            Resource config = resources.getResource(new Identifier(folder + "/background.properties")).orElse(null);
            if (config != null) {
                try {
                    props = new Props(config);
                } catch (IOException | RuntimeException e) {
                    Cera.LOGGER.warn("[CustomPanorama] Failed to load properties for {}", folder, e);
                }
            }

            return new Panorama(folder, textures,
                    Math.max(0, integer(props, "weight", 1)),
                    Math.max(1, integer(props, "blur1", 64)),
                    Math.max(1, integer(props, "blur2", 3)),
                    Math.max(0, integer(props, "blur3", 3)),
                    color(props, "overlay1.top", 0x80FFFFFF),
                    color(props, "overlay1.bottom", 0x00FFFFFF),
                    color(props, "overlay2.top", 0),
                    color(props, "overlay2.bottom", 0x80000000)
            );
        }

        private static int integer(Props props, String key, int fallback) {
            return props == null ? fallback : props.getInt(key, fallback).orElse(fallback);
        }

        private static int color(Props props, String key, int fallback) {
            return props == null ? fallback : props.getColor(key, fallback).orElse(fallback);
        }
    }
}
