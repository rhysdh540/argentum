package dev.rdh.cera.modules.colors;

import dev.rdh.cera.Cera;
import dev.rdh.cera.mixin.MapColorAccessor;
import dev.rdh.cera.props.Props;
import net.minecraft.block.material.MapColor;
import net.minecraft.resource.Identifier;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReloadListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public final class CustomColors implements ResourceReloadListener {
    private static final String[] FILES = {"optifine/color.properties", "mcpatcher/color.properties"};

    private static final Map<String, MapColor> MAP_COLORS = Map.ofEntries(
            Map.entry("air", MapColor.AIR),
            Map.entry("grass", MapColor.GRASS),
            Map.entry("sand", MapColor.SAND),
            Map.entry("cloth", MapColor.WEB),
            Map.entry("tnt", MapColor.LAVA),
            Map.entry("ice", MapColor.ICE),
            Map.entry("iron", MapColor.IRON),
            Map.entry("foliage", MapColor.FOLIAGE),
            Map.entry("snow", MapColor.WHITE),
            Map.entry("white", MapColor.WHITE),
            Map.entry("clay", MapColor.CLAY),
            Map.entry("dirt", MapColor.DIRT),
            Map.entry("stone", MapColor.STONE),
            Map.entry("water", MapColor.WATER),
            Map.entry("wood", MapColor.WOOD),
            Map.entry("quartz", MapColor.QUARTZ),
            Map.entry("adobe", MapColor.ORANGE),
            Map.entry("orange", MapColor.ORANGE),
            Map.entry("magenta", MapColor.MAGENTA),
            Map.entry("light_blue", MapColor.LIGHT_BLUE),
            Map.entry("lightBlue", MapColor.LIGHT_BLUE),
            Map.entry("yellow", MapColor.YELLOW),
            Map.entry("lime", MapColor.LIME),
            Map.entry("pink", MapColor.PINK),
            Map.entry("gray", MapColor.GRAY),
            Map.entry("silver", MapColor.LIGHT_GRAY),
            Map.entry("cyan", MapColor.CYAN),
            Map.entry("purple", MapColor.PURPLE),
            Map.entry("blue", MapColor.BLUE),
            Map.entry("brown", MapColor.BROWN),
            Map.entry("green", MapColor.GREEN),
            Map.entry("red", MapColor.RED),
            Map.entry("black", MapColor.BLACK),
            Map.entry("gold", MapColor.GOLD),
            Map.entry("diamond", MapColor.DIAMOND),
            Map.entry("lapis", MapColor.LAPIS),
            Map.entry("emerald", MapColor.EMERALD),
            Map.entry("podzol", MapColor.SPRUCE),
            Map.entry("netherrack", MapColor.NETHER)
    );

    private int[] mapColorOriginals;

    @Override
    public void resourcesReloaded(ResourceManager resources) {
        applyMapColors(Cera.CONFIG.customColors ? parseMapColors(resources) : null);
    }

    private static int[] parseMapColors(ResourceManager resources) {
        int[] colors = new int[MapColor.BY_ID.length];
        Arrays.fill(colors, -1);
        boolean any = false;
        for (String file : FILES) {
            Resource resource = resources.getResource(new Identifier(file)).orElse(null);
            if (resource == null) continue;
            try {
                Props props = new Props(resource);
                for (var entry : MAP_COLORS.entrySet()) {
                    int color = props.getColor("map." + entry.getKey()).orElse(-1);
                    if (color >= 0) {
                        colors[entry.getValue().id] = color;
                        any = true;
                    }
                }
            } catch (IOException e) {
                Cera.LOGGER.warn("[CustomColors] Failed to read {}", file, e);
            }
        }
        return any ? colors : null;
    }

    private void applyMapColors(int[] overrides) {
        if (overrides == null && this.mapColorOriginals == null) return;
        if (this.mapColorOriginals == null) {
            this.mapColorOriginals = new int[MapColor.BY_ID.length];
            for (int i = 0; i < MapColor.BY_ID.length; i++) {
                MapColor color = MapColor.BY_ID[i];
                this.mapColorOriginals[i] = color == null ? -1 : color.color;
            }
        }
        for (int i = 0; i < MapColor.BY_ID.length; i++) {
            MapColor color = MapColor.BY_ID[i];
            if (color == null) continue;
            int target = overrides != null && overrides[i] >= 0 ? overrides[i] : this.mapColorOriginals[i];
            ((MapColorAccessor) color).cera$setColor(target);
        }
    }
}
