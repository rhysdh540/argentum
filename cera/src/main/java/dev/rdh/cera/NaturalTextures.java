package dev.rdh.cera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.ornithemc.osl.resource.loader.api.resource.Resource;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class NaturalTextures {
    private static final Identifier CONFIG = new Identifier("optifine/natural.properties");
    private static final Identifier LEGACY_CONFIG = new Identifier("mcpatcher/natural.properties");
    private static volatile Map<String, Rule> rules = Map.of();

    private NaturalTextures() {
    }

    public static void reload(ResourceManager resources) {
        try {
            Resource config = get(resources, CONFIG);
            boolean defaults = resources.getResourceStack(CONFIG).size() == 1;
            if (defaults) {
                Resource legacyConfig = get(resources, LEGACY_CONFIG);
                if (legacyConfig != null) {
                    config = legacyConfig;
                    defaults = false;
                }
            }

            Properties properties = new Properties();
            try (var stream = config.open()) {
                properties.load(stream);
            }

            Map<String, Rule> loaded = new HashMap<>();
            for (String texture : properties.stringPropertyNames()) {
                Rule rule = Rule.parse(properties.getProperty(texture));
                if (rule == null) {
                    Cera.LOGGER.warn("Invalid natural texture rule: {}={}", texture, properties.getProperty(texture));
                } else if (!defaults || isVanillaTexture(resources, texture)) {
                    TextureAtlasSprite sprite = Minecraft.getInstance().getBlocksAtlas()
                            .getSprite("minecraft:blocks/" + texture);
                    if (sprite != Minecraft.getInstance().getBlocksAtlas().getMissingSprite()) {
                        loaded.put(sprite.getName(), rule);
                    }
                }
            }
            rules = Map.copyOf(loaded);
            Cera.LOGGER.info("Loaded {} natural texture rules", rules.size());
        } catch (IOException e) {
            rules = Map.of();
            Cera.LOGGER.warn("Failed to load natural textures", e);
        }
    }

    public static int getTransform(BakedQuadView quad, BlockPos pos) {
        if (!Cera.CONFIG.naturalTextures) return 0;

        TextureAtlasSprite sprite = (TextureAtlasSprite)quad.celeritas$getSprite();
        Rule rule = sprite == null ? null : rules.get(sprite.getName());
        if (rule == null) return 0;

        int random = random(pos, side(quad.getLightFace()));
        int rotation = switch (rule.rotations) {
            case 4 -> random & 3;
            case 2 -> random & 2;
            default -> 0;
        };
        boolean flip = rule.flip && (random & 4) != 0;
        if (rotation != 0 && !isFullSprite(quad, sprite)) rotation = 0;
        return rotation | (flip ? 4 : 0);
    }

    public static int transformVertex(int transform, int vertex) {
        int rotation = transform & 3;
        boolean flip = (transform & 4) != 0;
        return flip ? (3 - rotation - vertex) & 3 : (vertex + rotation) & 3;
    }

    private static Resource get(ResourceManager resources, Identifier id) {
        return resources.getResource(id).orElse(null);
    }

    private static boolean isVanillaTexture(ResourceManager resources, String texture) {
        Resource resource = get(resources, new Identifier("textures/blocks/" + texture + ".png"));
        if (resource == null) return false;
        try (resource) {
            return "Default".equals(resource.sourceName());
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isFullSprite(BakedQuadView quad, TextureAtlasSprite sprite) {
        float toleranceU = (sprite.getUMax() - sprite.getUMin()) / 256.0F;
        float toleranceV = (sprite.getVMax() - sprite.getVMin()) / 256.0F;
        for (int vertex = 0; vertex < 4; vertex++) {
            float u = quad.getTexU(vertex);
            float v = quad.getTexV(vertex);
            if (!nearEdge(u, sprite.getUMin(), sprite.getUMax(), toleranceU)
                    || !nearEdge(v, sprite.getVMin(), sprite.getVMax(), toleranceV)) {
                return false;
            }
        }
        return true;
    }

    private static boolean nearEdge(float value, float min, float max, float tolerance) {
        return Math.abs(value - min) < tolerance || Math.abs(value - max) < tolerance;
    }

    private static int side(ModelQuadFacing face) {
        return switch (face) {
            case NEG_Y -> 0;
            case POS_Y -> 1;
            case NEG_Z -> 2;
            case POS_Z -> 3;
            case NEG_X -> 4;
            case POS_X -> 5;
            default -> -1;
        };
    }

    // From Config.getRandom(BlockPos, int) and Config.intHash(int) in OptiFine 1.8.9 HD U M6 pre2.
    private static int random(BlockPos pos, int side) {
        int value = hash(side + 37);
        value = hash(value + pos.getX());
        value = hash(value + pos.getZ());
        return hash(value + pos.getY());
    }

    private static int hash(int x) {
        x = x ^ 61 ^ x >> 16;
        x += x << 3;
        x ^= x >> 4;
        x *= 668265261;
        return x ^ x >> 15;
    }

    private record Rule(int rotations, boolean flip) {
        private static Rule parse(String value) {
            return switch (value.trim()) {
                case "2" -> new Rule(2, false);
                case "4" -> new Rule(4, false);
                case "F" -> new Rule(1, true);
                case "2F" -> new Rule(2, true);
                case "4F" -> new Rule(4, true);
                default -> null;
            };
        }
    }
}
