package dev.rdh.cera.modules;

import dev.rdh.cera.Cera;
import dev.rdh.cera.props.Props;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.resource.Identifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReloadListener;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CustomLoadingScreens implements ResourceReloadListener {
    private static final Pattern BACKGROUND = Pattern.compile("optifine/gui/loading/background(-?\\d+)\\.png");
    private static final Identifier CONFIG = new Identifier("optifine/gui/loading/loading.properties");

    private volatile Int2ObjectMap<LoadingScreen> screens = Int2ObjectMaps.emptyMap();
    private volatile int dimension;

    public void setDimension(int id) {
        this.dimension = id;
    }

    public LoadingScreen active() {
        return Cera.CONFIG.customLoadingScreens ? this.screens.get(this.dimension) : null;
    }

    @Override
    public void resourcesReloaded(ResourceManager resources) {
        Props props = null;
        Resource config = resources.getResource(CONFIG).orElse(null);
        if (config != null) {
            try {
                props = new Props(config);
            } catch (IOException | RuntimeException e) {
                Cera.LOGGER.warn("[CustomLoadingScreens] Failed to load {}", CONFIG, e);
            }
        }

        Int2ObjectMap<LoadingScreen> loaded = new Int2ObjectOpenHashMap<>();
        for (NamespacedIdentifier location : resources.findResources("minecraft", "optifine/gui/loading/",
                id -> BACKGROUND.matcher(id.identifier()).matches()).keySet()) {
            Matcher matcher = BACKGROUND.matcher(location.identifier());
            if (!matcher.matches()) continue;
            int id = Integer.parseInt(matcher.group(1));
            loaded.put(id, LoadingScreen.parse(new Identifier(location.namespace(), location.identifier()), id, props));
        }

        this.screens = loaded.isEmpty() ? Int2ObjectMaps.emptyMap() : Int2ObjectMaps.unmodifiable(loaded);
        Cera.LOGGER.info("[CustomLoadingScreens] Loaded {} screens", loaded.size());
    }

    public enum ScaleMode {
        FIXED, FULL, STRETCH
    }

    public record LoadingScreen(Identifier texture, ScaleMode mode, int scale, boolean center) {
        // see net.optifine.CustomLoadingScreen.drawBackground
        public void draw(int width, int height) {
            double div = 16.0 * this.scale;
            double uMax = width / div;
            double vMax = height / div;
            double du = 0.0;
            double dv = 0.0;
            if (this.center) {
                du = (div - width) / (div * 2.0);
                dv = (div - height) / (div * 2.0);
            }

            if (this.mode == ScaleMode.FULL) {
                div = Math.max(width, height);
                uMax = this.scale * width / div;
                vMax = this.scale * height / div;
                if (this.center) {
                    du = this.scale * (div - width) / (div * 2.0);
                    dv = this.scale * (div - height) / (div * 2.0);
                }
            } else if (this.mode == ScaleMode.STRETCH) {
                uMax = this.scale;
                vMax = this.scale;
                du = 0.0;
                dv = 0.0;
            }

            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            Minecraft.getInstance().getTextureManager().bind(this.texture);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.vertex(0.0, height, 0.0).texture(du, dv + vMax).color(255, 255, 255, 255).nextVertex();
            buffer.vertex(width, height, 0.0).texture(du + uMax, dv + vMax).color(255, 255, 255, 255).nextVertex();
            buffer.vertex(width, 0.0, 0.0).texture(du + uMax, dv).color(255, 255, 255, 255).nextVertex();
            buffer.vertex(0.0, 0.0, 0.0).texture(du, dv).color(255, 255, 255, 255).nextVertex();
            tesselator.end();
        }

        private static LoadingScreen parse(Identifier texture, int dimension, Props props) {
            ScaleMode mode = scaleMode(get(props, "scaleMode", dimension));
            int scale = scale(get(props, "scale", dimension), mode == ScaleMode.FIXED ? 2 : 1);
            boolean center = "true".equalsIgnoreCase(get(props, "center", dimension));
            return new LoadingScreen(texture, mode, scale, center);
        }

        private static String get(Props props, String key, int dimension) {
            if (props == null) return null;
            String value = props.get("dim" + dimension + "." + key);
            return value != null ? value : props.get(key);
        }

        private static ScaleMode scaleMode(String value) {
            if (value == null) return ScaleMode.FIXED;
            return switch (value.trim().toLowerCase()) {
                case "full" -> ScaleMode.FULL;
                case "stretch" -> ScaleMode.STRETCH;
                case "fixed" -> ScaleMode.FIXED;
                default -> {
                    Cera.LOGGER.warn("[CustomLoadingScreens] Invalid scale mode: {}", value);
                    yield ScaleMode.FIXED;
                }
            };
        }

        private static int scale(String value, int fallback) {
            if (value == null) return fallback;
            try {
                int scale = Integer.parseInt(value.trim());
                if (scale > 0) return scale;
            } catch (NumberFormatException _) {
                // fall through to the warning
            }
            Cera.LOGGER.warn("[CustomLoadingScreens] Invalid scale: {}", value);
            return fallback;
        }
    }
}
