package dev.rdh.cera.modules;

import dev.rdh.cera.Cera;
import dev.rdh.cera.props.BlockMatcher;
import dev.rdh.cera.props.Props;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.resource.Identifier;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReloadListener;

import java.io.IOException;
import java.util.Map;

public final class CustomBlockLayers implements ResourceReloadListener {
    private static final Identifier CONFIG = new Identifier("optifine/block.properties");
    private static final Identifier LEGACY_CONFIG = new Identifier("mcpatcher/block.properties");

    private static final Map<String, BlockLayer> NAMES = Map.of(
            "solid", BlockLayer.SOLID,
            "cutout", BlockLayer.CUTOUT,
            "cutout_mipped", BlockLayer.CUTOUT_MIPPED,
            "translucent", BlockLayer.TRANSLUCENT
    );

    private volatile Map<Block, BlockLayer> layers = Map.of();

    public BlockLayer get(Block block) {
        Map<Block, BlockLayer> current = this.layers;
        return current.isEmpty() || block.isOpaque() ? null : current.get(block);
    }

    @Override
    public void resourcesReloaded(ResourceManager resources) {
        if (!Cera.CONFIG.customBlockLayers) {
            this.layers = Map.of();
            return;
        }

        Resource config = resources.getResource(CONFIG).or(() -> resources.getResource(LEGACY_CONFIG)).orElse(null);
        if (config == null) {
            this.layers = Map.of();
            return;
        }

        Map<Block, BlockLayer> loaded = new Reference2ReferenceOpenHashMap<>();
        try {
            Props props = new Props(config);
            for (var entry : NAMES.entrySet()) {
                String value = props.get("layer." + entry.getKey());
                if (value == null) continue;
                for (Block block : BlockMatcher.parseBlocks(value).keySet()) {
                    BlockLayer previous = loaded.put(block, entry.getValue());
                    if (previous != null && previous != entry.getValue()) {
                        Cera.LOGGER.warn("[CustomBlockLayers] {} is already on layer {}", block, previous);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Cera.LOGGER.warn("[CustomBlockLayers] Failed to load {}", CONFIG, e);
            this.layers = Map.of();
            return;
        }

        this.layers = loaded.isEmpty() ? Map.of() : new Object2ObjectOpenHashMap<>(loaded);
        Cera.LOGGER.info("[CustomBlockLayers] Loaded {} block layer overrides", this.layers.size());
    }
}
