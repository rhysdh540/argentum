package dev.rdh.cera.modules.colors;

import dev.rdh.cera.Cera;
import dev.rdh.cera.mixin.colors.MapColorAccessor;
import dev.rdh.cera.mixin.TextRendererInvoker;
import dev.rdh.cera.props.Props;
import dev.rdh.argentum.impl.ext.TextRendererExtension;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.entity.Entities;
import net.minecraft.entity.living.effect.StatusEffect;
import net.minecraft.item.DyeColor;
import net.minecraft.resource.Identifier;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReloadListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

    private static final Map<String, Integer> DYE_ORDINALS = dyeOrdinals();

    private int[] mapColorOriginals;
    private volatile Int2IntMap eggShellColors = empty();
    private volatile Int2IntMap eggSpotsColors = empty();
    private volatile Int2IntMap potionColors = empty();
    private volatile float[][] sheepColors = new float[16][];
    private volatile float[][] collarColors = new float[16][];
    private volatile int[] textColors = unsetTextColors();
    private volatile int particleWaterColor = -1;
    private volatile int particleLavaColor = -1;
    private volatile int particlePortalColor = -1;
    private volatile int lilypadColor = -1;
    private volatile int signTextColor = -1;
    private volatile int expBarTextColor = -1;
    private volatile int bossTextColor = -1;
    private volatile int armorDefaultColor = -1;
    private volatile CloudMode cloudMode = CloudMode.DEFAULT;
    private volatile int[] lavaDropPalette;
    private volatile int[] xpOrbPalette;
    private volatile int[] durabilityPalette;
    private volatile int xpOrbTime = 628;

    @Override
    public void resourcesReloaded(ResourceManager resources) {
        int[] mapOverrides = new int[MapColor.BY_ID.length];
        Arrays.fill(mapOverrides, -1);
        boolean anyMap = false;
        Int2IntMap eggShell = empty(), eggSpots = empty(), potions = empty();
        float[][] sheep = new float[16][], collar = new float[16][];
        int[] text = unsetTextColors();
        int waterColor = -1, lavaColor = -1, portalColor = -1, lilypad = -1, signText = -1;
        int expBar = -1, bossText = -1, armorDefault = -1;
        CloudMode clouds = CloudMode.DEFAULT;
        int xpTime = 628;

        for (String file : FILES) {
            Resource resource = resources.getResource(new Identifier(file)).orElse(null);
            if (resource == null) continue;
            try {
                Props props = new Props(resource);
                anyMap |= parseMapColors(props, mapOverrides);
                parseSpawnEggs(props, "egg.shell.", eggShell);
                parseSpawnEggs(props, "egg.spots.", eggSpots);
                parsePotions(props, potions);
                parseDyeColors(props, "sheep.", sheep);
                parseDyeColors(props, "collar.", collar);
                parseTextColors(props, text);
                waterColor = props.getColor("particle.water").orElse(props.getColor("drop.water").orElse(waterColor));
                lavaColor = props.getColor("particle.lava").orElse(lavaColor);
                portalColor = props.getColor("particle.portal").orElse(portalColor);
                lilypad = props.getColor("lilypad").orElse(lilypad);
                signText = props.getColor("text.sign").orElse(signText);
                expBar = props.getColor("text.xpbar").orElse(expBar);
                bossText = props.getColor("text.boss").orElse(bossText);
                armorDefault = props.getColor("armor.default").orElse(armorDefault);
                clouds = parseCloudMode(props.get("clouds"), clouds);
                xpTime = props.getInt("xporb.time", xpTime).orElse(xpTime);
            } catch (IOException e) {
                Cera.LOGGER.warn("[CustomColors] Failed to read {}", file, e);
            }
        }

        this.eggShellColors = eggShell;
        this.eggSpotsColors = eggSpots;
        this.potionColors = potions;
        this.sheepColors = sheep;
        this.collarColors = collar;
        this.textColors = text;
        this.particleWaterColor = waterColor;
        this.particleLavaColor = lavaColor;
        this.particlePortalColor = portalColor;
        this.lilypadColor = lilypad;
        this.signTextColor = signText;
        this.expBarTextColor = expBar;
        this.bossTextColor = bossText;
        this.armorDefaultColor = armorDefault;
        this.cloudMode = clouds;
        this.lavaDropPalette = readPalette(resources, "lavadrop.png");
        this.xpOrbPalette = readPalette(resources, "xporb.png");
        this.durabilityPalette = readPalette(resources, "durability.png");
        this.xpOrbTime = xpTime;
        applyMapColors(enabled() && anyMap ? mapOverrides : null);
        reapplyTextColors();
    }

    public void reapplyTextColors() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        reapplyTextColors(mc.textRenderer);
        reapplyTextColors(mc.enchantingPhraseRenderer);
    }

    private void reapplyTextColors(TextRenderer renderer) {
        if (renderer == null) return;
        ((TextRendererInvoker) renderer).cera$init();
        ((TextRendererExtension) renderer).argentum$invalidateTextCache();
    }

    public void applyTextColors(int[] target) {
        if (!enabled()) return;
        int[] palette = this.textColors;
        for (int i = 0; i < palette.length && i < target.length; i++) {
            if (palette[i] >= 0) target[i] = palette[i];
        }
    }

    public int getParticlePortalColor(int fallback) {
        return enabled() && this.particlePortalColor >= 0 ? this.particlePortalColor : fallback;
    }

    public int getParticleWaterColor(int fallback) {
        return enabled() && this.particleWaterColor >= 0 ? this.particleWaterColor : fallback;
    }

    public int getParticleLavaColor(int fallback) {
        return enabled() && this.particleLavaColor >= 0 ? this.particleLavaColor : fallback;
    }

    public int getLilypadColor(int fallback) {
        return enabled() && this.lilypadColor >= 0 ? this.lilypadColor : fallback;
    }

    public int getSignTextColor(int fallback) {
        return enabled() && this.signTextColor >= 0 ? this.signTextColor : fallback;
    }

    public int getExpBarTextColor(int fallback) {
        return enabled() && this.expBarTextColor >= 0 ? this.expBarTextColor : fallback;
    }

    public int getBossTextColor(int fallback) {
        return enabled() && this.bossTextColor >= 0 ? this.bossTextColor : fallback;
    }

    public int getArmorDefaultColor(int fallback) {
        return enabled() && this.armorDefaultColor >= 0 ? this.armorDefaultColor : fallback;
    }

    public CloudMode getCloudMode() {
        return enabled() ? this.cloudMode : CloudMode.DEFAULT;
    }

    public enum CloudMode { DEFAULT, OFF, FAST, FANCY }

    private static CloudMode parseCloudMode(String value, CloudMode fallback) {
        if (value == null) return fallback;
        return switch (value.trim()) {
            case "off", "none", "false" -> CloudMode.OFF;
            case "fast" -> CloudMode.FAST;
            case "fancy" -> CloudMode.FANCY;
            default -> fallback;
        };
    }

    public int getLavaDropColor(int age, int fallback) {
        return sample(this.lavaDropPalette, age, fallback);
    }

    public int getXpOrbColor(float timer, int fallback) {
        int[] palette = this.xpOrbPalette;
        if (!enabled() || palette == null || palette.length == 0) return fallback;
        int index = Math.round((float) ((Math.sin(timer) + 1.0) * (palette.length - 1) / 2.0));
        return sample(palette, index, fallback);
    }

    public int getXpOrbTime() {
        return this.xpOrbTime;
    }

    public int getDurabilityColor(int dur255, int fallback) {
        int[] palette = this.durabilityPalette;
        if (palette == null || palette.length == 0) return fallback;
        return sample(palette, dur255 * palette.length / 255, fallback);
    }

    private static int sample(int[] palette, int index, int fallback) {
        if (!enabled() || palette == null || palette.length == 0) return fallback;
        return palette[Math.clamp(index, 0, palette.length - 1)] & 0xFFFFFF;
    }

    private static int[] readPalette(ResourceManager resources, String name) {
        for (String directory : new String[]{"optifine/colormap/", "mcpatcher/colormap/"}) {
            Resource resource = resources.getResource(new Identifier(directory + name)).orElse(null);
            if (resource == null) continue;
            try (resource; InputStream in = resource.open()) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
                }
            } catch (IOException e) {
                Cera.LOGGER.warn("[CustomColors] Failed to read {}", name, e);
            }
        }
        return null;
    }

    public float[] getSheepColor(DyeColor dye, float[] fallback) {
        if (!enabled()) return fallback;
        float[] color = this.sheepColors[dye.ordinal()];
        return color != null ? color : fallback;
    }

    public float[] getCollarColor(DyeColor dye, float[] fallback) {
        if (!enabled()) return fallback;
        float[] color = this.collarColors[dye.ordinal()];
        return color != null ? color : fallback;
    }

    public int getSpawnEggColor(int entityId, int layer, int fallback) {
        if (!enabled()) return fallback;
        int color = (layer == 0 ? this.eggShellColors : this.eggSpotsColors).get(entityId);
        return color >= 0 ? color : fallback;
    }

    public int getPotionColor(int effectId, int fallback) {
        if (!enabled()) return fallback;
        int color = this.potionColors.get(effectId);
        return color >= 0 ? color : fallback;
    }

    private static void parseSpawnEggs(Props props, String prefix, Int2IntMap out) {
        for (String key : props.properties().stringPropertyNames()) {
            if (!key.startsWith(prefix)) continue;
            int id;
            try {
                id = Entities.getId(key.substring(prefix.length()));
            } catch (RuntimeException e) {
                id = -1;
            }
            int color = props.getColor(key).orElse(-1);
            if (id > 0 && color >= 0) out.put(id, color);
            else Cera.LOGGER.warn("[CustomColors] Invalid spawn egg entry: {}", key);
        }
    }

    private static void parsePotions(Props props, Int2IntMap out) {
        for (String key : props.properties().stringPropertyNames()) {
            if (!key.startsWith("potion.")) continue;
            String name = key.substring("potion.".length());
            int id = LEGACY_POTIONS.getOrDefault(name, -1);
            if (id < 0) {
                StatusEffect effect = StatusEffect.get(name);
                if (effect != null) id = effect.getId();
            }
            int color = props.getColor(key).orElse(-1);
            if (id > 0 && color >= 0) out.put(id, color);
            else Cera.LOGGER.warn("[CustomColors] Invalid potion entry: {}", key);
        }
    }

    // OptiFine/MCPatcher legacy effect names -> 1.8.9 status effect ids (potion.water has no effect and is skipped).
    private static final Map<String, Integer> LEGACY_POTIONS = Map.ofEntries(
            Map.entry("moveSpeed", 1),
            Map.entry("moveSlowdown", 2),
            Map.entry("digSpeed", 3),
            Map.entry("digSlowDown", 4),
            Map.entry("damageBoost", 5),
            Map.entry("heal", 6),
            Map.entry("harm", 7),
            Map.entry("jump", 8),
            Map.entry("confusion", 9),
            Map.entry("regeneration", 10),
            Map.entry("resistance", 11),
            Map.entry("fireResistance", 12),
            Map.entry("waterBreathing", 13),
            Map.entry("invisibility", 14),
            Map.entry("blindness", 15),
            Map.entry("nightVision", 16),
            Map.entry("hunger", 17),
            Map.entry("weakness", 18),
            Map.entry("poison", 19),
            Map.entry("wither", 20),
            Map.entry("healthBoost", 21),
            Map.entry("absorption", 22),
            Map.entry("saturation", 23)
    );

    private static void parseDyeColors(Props props, String prefix, float[][] out) {
        for (String key : props.properties().stringPropertyNames()) {
            if (!key.startsWith(prefix)) continue;
            String name = key.substring(prefix.length());
            if (name.equals("lightBlue")) name = "light_blue";
            Integer ordinal = DYE_ORDINALS.get(name);
            int color = props.getColor(key).orElse(-1);
            if (ordinal != null && color >= 0) {
                out[ordinal] = new float[]{(color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F};
            } else {
                Cera.LOGGER.warn("[CustomColors] Invalid dye entry: {}", key);
            }
        }
    }

    private static void parseTextColors(Props props, int[] out) {
        for (String key : props.properties().stringPropertyNames()) {
            if (!key.startsWith("text.code.")) continue;
            int code;
            try {
                code = Integer.parseInt(key.substring("text.code.".length()).trim());
            } catch (NumberFormatException e) {
                code = -1;
            }
            int color = props.getColor(key).orElse(-1);
            if (code >= 0 && code < out.length && color >= 0) out[code] = color;
            else Cera.LOGGER.warn("[CustomColors] Invalid text color entry: {}", key);
        }
    }

    private static int[] unsetTextColors() {
        int[] colors = new int[32];
        Arrays.fill(colors, -1);
        return colors;
    }

    private static Map<String, Integer> dyeOrdinals() {
        var map = new java.util.HashMap<String, Integer>();
        for (DyeColor dye : DyeColor.values()) map.put(dye.getName(), dye.ordinal());
        return Map.copyOf(map);
    }

    private static Int2IntMap empty() {
        Int2IntMap map = new Int2IntOpenHashMap();
        map.defaultReturnValue(-1);
        return map;
    }

    private static boolean parseMapColors(Props props, int[] out) {
        boolean any = false;
        for (var entry : MAP_COLORS.entrySet()) {
            int color = props.getColor("map." + entry.getKey()).orElse(-1);
            if (color >= 0) {
                out[entry.getValue().id] = color;
                any = true;
            }
        }
        return any;
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

    private static boolean enabled() {
        return Cera.CONFIG.customColors;
    }
}
