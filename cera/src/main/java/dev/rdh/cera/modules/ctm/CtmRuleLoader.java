package dev.rdh.cera.modules.ctm;

import dev.rdh.cera.Cera;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.property.Property;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.texture.TextureAtlas;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.Direction;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.ornithemc.osl.core.api.util.function.IOSupplier;
import net.ornithemc.osl.resource.loader.api.resource.ResourceType;
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager;
import net.ornithemc.osl.resource.loader.api.resource.pack.ResourcePack;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.rdh.cera.modules.ctm.CtmRule.Connect;
import static dev.rdh.cera.modules.ctm.CtmRule.Method;
import static dev.rdh.cera.modules.ctm.CtmRule.Tile;
import static dev.rdh.cera.modules.ctm.CtmRule.TileAction;

final class CtmRuleLoader {
    private static final String OPTIFINE_PREFIX = "optifine/ctm/";
    private static final String MCPATCHER_PREFIX = "mcpatcher/ctm/";
    private static final Pattern BLOCK_FILE = Pattern.compile("^block(\\d+).*");
    private static final Pattern RANGE = Pattern.compile("(-?\\d+)(?:-(-?\\d+))?");

    private CtmRuleLoader() {
    }

    static List<CtmRule> load(TextureAtlas atlas, Map<String, TextureAtlasSprite> sourcedSprites) {
        List<CtmRule> rules = new ObjectArrayList<>();
        Set<String> loadedPaths = new ObjectOpenHashSet<>();
        List<ResourcePack> packs = ResourceManager.client().getResourcePacks();
        for (int i = packs.size() - 1; i >= 0; i--) {
            loadPack(packs.get(i), atlas, sourcedSprites, rules, loadedPaths);
        }
        return List.copyOf(rules);
    }

    private static void loadPack(ResourcePack pack, TextureAtlas atlas, Map<String, TextureAtlasSprite> sourcedSprites, List<CtmRule> rules, Set<String> loadedPaths) {
        Map<String, IOSupplier<InputStream>> files = new Object2ObjectAVLTreeMap<>();
        List<CtmRule> packRules = new ObjectArrayList<>();
        collectProperties(pack, OPTIFINE_PREFIX, files);
        collectProperties(pack, MCPATCHER_PREFIX, files);
        for (var entry : files.entrySet()) {
            String path = entry.getKey();
            if (!loadedPaths.add(path)) continue;
            try (InputStream stream = entry.getValue().get()) {
                Properties properties = new Properties();
                properties.load(stream);
                CtmRule rule = parse(properties, path, atlas, sourcedSprites);
                if (rule != null) packRules.add(rule);
            } catch (Exception e) {
                Cera.LOGGER.warn("Failed to load connected textures rule {} from {}", path, pack.getName(), e);
            }
        }
        packRules.sort(Comparator.comparingInt(CtmRule::weight).reversed().thenComparing(CtmRule::path));
        rules.addAll(packRules);
    }

    private static void collectProperties(ResourcePack pack, String directory,
            Map<String, IOSupplier<InputStream>> files) {
        pack.findResources(ResourceType.CLIENT_ASSETS, "minecraft", directory,
                (location, resource) -> {
                    String path = location.identifier();
                    if (path.endsWith(".properties")) files.put(path, resource);
                });
    }

    private static CtmRule parse(Properties properties, String path, TextureAtlas atlas, Map<String, TextureAtlasSprite> sourcedSprites) {
        String base = path.substring(0, path.lastIndexOf('/'));
        String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".properties".length());
        Method method = Method.parse(properties.getProperty("method", "ctm"));
        if (method == null) return null;

        int metadata = parseIntMask(properties.getProperty("metadata"));
        Map<Block, Integer> matchBlocks = parseBlocks(properties.getProperty("matchBlocks"));
        Set<String> matchTiles = parseMatchTiles(
                properties.getProperty("matchTiles"), base, atlas, sourcedSprites);

        boolean inferredBlock = false;
        if (!properties.containsKey("matchBlocks")) {
            Matcher matcher = BLOCK_FILE.matcher(name);
            if (matcher.matches()) {
                inferredBlock = true;
                Block block = Block.byId(Integer.parseInt(matcher.group(1)));
                if (block != null) matchBlocks.put(block, -1);
            }
        }
        if (!properties.containsKey("matchBlocks") && !inferredBlock && !properties.containsKey("matchTiles")) {
            TextureAtlasSprite sprite = register(atlas, sourcedSprites, new Identifier("blocks/" + name));
            matchTiles.add(sprite.getName());
        }
        if (matchBlocks.isEmpty() && matchTiles.isEmpty()) {
            Cera.LOGGER.warn("No matchBlocks or matchTiles in {}", path);
            return null;
        }

        String tileValue = properties.getProperty("tiles",
                method == Method.CTM ? "0-11 16-27 32-43 48-58"
                        : method == Method.CTM_COMPACT ? "0-4"
                        : method == Method.OVERLAY ? "0-16"
                        : method == Method.OVERLAY_CTM ? "0-11 16-27 32-43 48-58"
                        : method == Method.HORIZONTAL ? "12-15" : "");
        List<String> tileNames = expand(tileValue);
        int requiredTiles = requiredTiles(method, properties);
        if (requiredTiles < 1 || (method == Method.CTM || method == Method.CTM_COMPACT
                || method == Method.OVERLAY || method == Method.OVERLAY_CTM
                ? tileNames.size() < requiredTiles : tileNames.size() != requiredTiles)) {
            Cera.LOGGER.warn("Invalid tile count for connected texture rule {}: expected {}, found {}",
                    path, requiredTiles, tileNames.size());
            return null;
        }
        Tile[] tiles = new Tile[tileNames.size()];
        for (int i = 0; i < tiles.length; i++) {
            String tile = tileNames.get(i);
            if (tile.equals("<skip>") || tile.equals("<skip>.png")) {
                tiles[i] = new Tile(null, TileAction.SKIP);
            } else if (tile.equals("<default>") || tile.equals("<default>.png")) {
                if (method.overlay()) {
                    Cera.LOGGER.warn("<default> is not valid for overlay rule {}", path);
                    return null;
                }
                tiles[i] = new Tile(null, TileAction.DEFAULT);
            } else {
                tiles[i] = new Tile(
                        register(atlas, sourcedSprites, resolveSprite(tile, base, true)),
                        TileAction.REPLACE);
            }
        }

        Connect connect = switch (properties.getProperty("connect", matchBlocks.isEmpty() ? "tile" : "block").trim()) {
            case "block" -> Connect.BLOCK;
            case "tile" -> Connect.TILE;
            case "material" -> Connect.MATERIAL;
            case "state" -> Connect.STATE;
            default -> null;
        };
        if (connect == null) {
            Cera.LOGGER.warn("Invalid connect in {}", path);
            return null;
        }

        int[] weights = method == Method.RANDOM || method == Method.OVERLAY_RANDOM
                ? parseWeights(properties.getProperty("weights"), tiles.length) : null;
        int randomLoops = parseInt(properties.getProperty("randomLoops"), 0);
        if ((method == Method.RANDOM || method == Method.OVERLAY_RANDOM)
                && (randomLoops < 0 || randomLoops > 9)) {
            Cera.LOGGER.warn("Invalid randomLoops in {}", path);
            return null;
        }

        BlockLayer layer = parseLayer(properties.getProperty("layer", "cutout_mipped"));
        if (method.overlay() && layer == null) {
            Cera.LOGGER.warn("Invalid overlay layer in {}", path);
            return null;
        }

        return new CtmRule(
                path,
                parseInt(properties.getProperty("weight"), 0),
                Map.copyOf(matchBlocks),
                Set.copyOf(matchTiles),
                metadata,
                parseFaces(properties.getProperty("faces")),
                parseHeights(properties),
                parseBiomes(properties.getProperty("biomes")),
                parseName(properties.getProperty("name")),
                Boolean.parseBoolean(properties.getProperty("innerSeams", "false")),
                connect,
                method,
                tiles,
                weights,
                randomLoops,
                parseSymmetry(properties.getProperty("symmetry")),
                Boolean.parseBoolean(properties.getProperty("linked", "false")),
                parseInt(properties.getProperty("width"), -1),
                parseInt(properties.getProperty("height"), -1),
                method == Method.CTM_COMPACT ? parseCtmOverrides(properties, tiles.length) : null,
                maxMetadata(metadata, matchBlocks),
                Map.copyOf(parseBlocks(properties.getProperty("connectBlocks"))),
                Set.copyOf(parseMatchTiles(
                        properties.getProperty("connectTiles"), base, atlas, sourcedSprites)),
                parseInt(properties.getProperty("tintIndex"), -1),
                parseTintState(properties.getProperty("tintBlock")),
                layer
        );
    }

    private static int requiredTiles(Method method, Properties properties) {
        return switch (method) {
            case CTM -> 47;
            case CTM_COMPACT -> 5;
            case OVERLAY -> 17;
            case OVERLAY_CTM -> 47;
            case HORIZONTAL, VERTICAL -> 4;
            case TOP, FIXED, OVERLAY_FIXED -> 1;
            case HORIZONTAL_VERTICAL, VERTICAL_HORIZONTAL -> 7;
            case RANDOM, OVERLAY_RANDOM -> expand(properties.getProperty("tiles", "")).size();
            case REPEAT, OVERLAY_REPEAT -> {
                int width = parseInt(properties.getProperty("width"), -1);
                int height = parseInt(properties.getProperty("height"), -1);
                yield width > 0 && height > 0 ? width * height : -1;
            }
        };
    }

    private static int[] parseCtmOverrides(Properties properties, int tileCount) {
        int[] overrides = new int[47];
        Arrays.fill(overrides, -1);
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("ctm.")) continue;
            int index = parseInt(key.substring("ctm.".length()), -1);
            int tile = parseInt(properties.getProperty(key), -1);
            if (index >= 0 && index < overrides.length && tile >= 0 && tile < tileCount) {
                overrides[index] = tile;
            }
        }
        return overrides;
    }

    private static Object2IntMap<Block> parseBlocks(String value) {
        if (value == null) return new Object2IntOpenHashMap<>();
        Object2IntMap<Block> blocks = new Object2IntOpenHashMap<>();
        for (String token : value.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            String[] parts = token.split(":");
            int blockIndex = parts.length > 1 && !startsWithDigit(parts[1]) && !parts[1].contains("=") ? 1 : 0;
            String[] blockNames = blockIndex == 1
                    ? new String[]{parts[0] + ":" + parts[1]} : parts[0].split(",");
            String[] parameters = Arrays.copyOfRange(parts, blockIndex + 1, parts.length);
            for (String blockName : blockNames) {
                Block block = parseBlock(blockName);
                if (block == null) {
                    Cera.LOGGER.warn("Unknown block in connected texture rule: {}", blockName);
                } else {
                    int mask = parseBlockStates(block, parameters);
                    blocks.mergeInt(block, mask, (first, second) -> first | second);
                }
            }
        }
        return blocks;
    }

    private static int parseBlockStates(Block block, String[] parameters) {
        if (parameters.length == 0) return -1;
        if (startsWithDigit(parameters[0])) return parseIntMask(parameters[0]);

        Map<String, Set<String>> expected = new Object2ObjectOpenHashMap<>();
        for (String parameter : parameters) {
            String[] pair = parameter.split("=", 2);
            if (pair.length != 2) return 0;
            expected.put(pair[0], new ObjectOpenHashSet<>(Arrays.asList(pair[1].split(","))));
        }

        int mask = 0;
        for (int metadata = 0; metadata < 16; metadata++) {
            BlockState state;
            try {
                state = block.getStateFromMetadata(metadata);
            } catch (IllegalArgumentException _) {
                continue;
            }
            if (matchesProperties(state, expected)) mask |= 1 << metadata;
        }
        return mask == 0xFFFF ? -1 : mask;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean matchesProperties(BlockState state, Map<String, Set<String>> expected) {
        for (var entry : expected.entrySet()) {
            Property property = state.properties().stream()
                    .filter(candidate -> candidate.getName().equals(entry.getKey()))
                    .findFirst().orElse(null);
            if (property == null || !entry.getValue().contains(propertyValueName(property, state.get(property)))) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> String propertyValueName(Property<T> property, T value) {
        return property.getName(value);
    }

    private static boolean startsWithDigit(String value) {
        return !value.isEmpty() && Character.isDigit(value.charAt(0));
    }

    private static Block parseBlock(String value) {
        try {
            return Block.byId(Integer.parseInt(value));
        } catch (NumberFormatException _) {
            return Block.byKey(value);
        }
    }

    private static BlockState parseTintState(String value) {
        if (value == null) return Blocks.AIR.defaultState();
        Map<Block, Integer> blocks = parseBlocks(value);
        if (blocks.isEmpty()) return Blocks.AIR.defaultState();
        var entry = blocks.entrySet().iterator().next();
        int metadata = entry.getValue() == -1 ? 0 : Integer.numberOfTrailingZeros(entry.getValue());
        return entry.getKey().getStateFromMetadata(metadata);
    }

    private static BlockLayer parseLayer(String value) {
        return switch (value.trim()) {
            case "cutout_mipped" -> BlockLayer.CUTOUT_MIPPED;
            case "cutout" -> BlockLayer.CUTOUT;
            case "translucent" -> BlockLayer.TRANSLUCENT;
            default -> null;
        };
    }

    private static Set<String> parseMatchTiles(String value, String base, TextureAtlas atlas, Map<String, TextureAtlasSprite> sourcedSprites) {
        if (value == null) return new ObjectOpenHashSet<>();
        Set<String> tiles = new ObjectOpenHashSet<>();
        for (String token : value.trim().split("[ ,]+")) {
            if (!token.isEmpty()) {
                tiles.add(register(atlas, sourcedSprites, resolveSprite(token, base, false)).getName());
            }
        }
        return tiles;
    }

    private static TextureAtlasSprite register(TextureAtlas atlas, Map<String, TextureAtlasSprite> sourcedSprites, Identifier id) {
        TextureAtlasSprite sprite = sourcedSprites.get(id.toString());
        return sprite == null ? atlas.registerSprite(id) : sprite;
    }

    private static List<String> expand(String value) {
        List<String> result = new ObjectArrayList<>();
        for (String token : value.trim().split("[ ,]+")) {
            int separator = token.indexOf('-');
            if (separator > 0) {
                try {
                    int min = Integer.parseInt(token.substring(0, separator));
                    int max = Integer.parseInt(token.substring(separator + 1));
                    if (min <= max) {
                        for (int i = min; i <= max; i++) result.add(Integer.toString(i));
                        continue;
                    }
                } catch (NumberFormatException _) {
                }
            }
            if (!token.isEmpty()) result.add(token);
        }
        return result;
    }

    private static Identifier resolveSprite(String value, String base, boolean relative) {
        String namespace = "minecraft";
        String path = value;
        int separator = value.indexOf(':');
        if (separator >= 0) {
            namespace = value.substring(0, separator);
            path = value.substring(separator + 1);
        }
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);

        String root = base.startsWith(OPTIFINE_PREFIX) ? "optifine/" : "mcpatcher/";
        if (path.startsWith("./")) {
            path = base + "/" + path.substring(2);
        } else if (path.startsWith("~/")) {
            path = root + path.substring(2);
        } else if (path.startsWith("/")) {
            path = root + path.substring(1);
        } else if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        } else if (relative && separator < 0 && !path.startsWith(OPTIFINE_PREFIX)
                && !path.startsWith(MCPATCHER_PREFIX) && !path.startsWith(base + "/")) {
            path = base + "/" + path;
        } else if (!relative && !path.contains("/")) {
            path = "blocks/" + path;
        } else if (separator >= 0 && !path.contains("/")) {
            path = "blocks/" + path;
        }
        return new Identifier(namespace, path);
    }

    private static int parseIntMask(String value) {
        if (value == null) return -1;
        int mask = 0;
        for (String token : value.trim().split("[ ,]+")) {
            Matcher matcher = RANGE.matcher(token);
            if (!matcher.matches()) continue;
            int min = Integer.parseInt(matcher.group(1));
            int max = matcher.group(2) == null ? min : Integer.parseInt(matcher.group(2));
            for (int i = Math.max(0, Math.min(min, max)); i <= Math.min(31, Math.max(min, max)); i++) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private static int[] parseWeights(String value, int size) {
        if (value == null) return null;
        int[] parsed = Arrays.stream(value.trim().split("[ ,]+"))
                .filter(token -> !token.isEmpty())
                .mapToInt(token -> parseInt(token, 0))
                .toArray();
        int[] weights = Arrays.copyOf(parsed, size);
        if (parsed.length < size) {
            int average = parsed.length == 0 ? 0 : Arrays.stream(parsed).sum() / parsed.length;
            Arrays.fill(weights, parsed.length, size, average);
        }
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            weights[i] = sum;
        }
        return weights;
    }

    private static int parseSymmetry(String value) {
        if (value == null || value.equals("none")) return 1;
        return switch (value.trim()) {
            case "opposite" -> 2;
            case "all" -> 6;
            default -> 1;
        };
    }

    private static Predicate<String> parseName(String value) {
        if (value == null) return null;
        boolean negative = value.startsWith("!");
        if (negative) value = value.substring(1);
        int flags = 0;
        String expression;
        if (value.startsWith("ipattern:")) {
            flags = Pattern.CASE_INSENSITIVE;
            expression = glob(value.substring("ipattern:".length()));
        } else if (value.startsWith("pattern:")) {
            expression = glob(value.substring("pattern:".length()));
        } else if (value.startsWith("iregex:")) {
            flags = Pattern.CASE_INSENSITIVE;
            expression = value.substring("iregex:".length());
        } else if (value.startsWith("regex:")) {
            expression = value.substring("regex:".length());
        } else {
            expression = Pattern.quote(value);
        }
        Pattern pattern = Pattern.compile(expression, flags);
        return name -> name != null && pattern.matcher(name).matches() != negative;
    }

    private static String glob(String value) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '*') regex.append(".*");
            else if (character == '?') regex.append('.');
            else regex.append(Pattern.quote(String.valueOf(character)));
        }
        return regex.toString();
    }

    private static int parseFaces(String value) {
        if (value == null) return 63;
        int faces = 0;
        for (String face : value.toLowerCase(Locale.ROOT).split("[ ,]+")) {
            faces |= switch (face) {
                case "bottom", "down" -> 1 << Direction.DOWN.ordinal();
                case "top", "up" -> 1 << Direction.UP.ordinal();
                case "north" -> 1 << Direction.NORTH.ordinal();
                case "south" -> 1 << Direction.SOUTH.ordinal();
                case "west" -> 1 << Direction.WEST.ordinal();
                case "east" -> 1 << Direction.EAST.ordinal();
                case "sides" -> (1 << Direction.NORTH.ordinal()) | (1 << Direction.SOUTH.ordinal())
                        | (1 << Direction.WEST.ordinal()) | (1 << Direction.EAST.ordinal());
                case "all" -> 63;
                default -> 0;
            };
        }
        return faces;
    }

    private static IntPredicate parseHeights(Properties properties) {
        String heights = properties.getProperty("heights");
        if (heights != null) {
            List<int[]> parsed = new ObjectArrayList<>();
            for (String token : heights.trim().split("[ ,]+")) {
                Matcher matcher = RANGE.matcher(token);
                if (matcher.matches()) {
                    int first = Integer.parseInt(matcher.group(1));
                    int second = matcher.group(2) == null ? first : Integer.parseInt(matcher.group(2));
                    parsed.add(new int[]{Math.min(first, second), Math.max(first, second)});
                }
            }
            List<int[]> ranges = List.copyOf(parsed);
            return y -> {
                for (int[] range : ranges) {
                    if (y >= range[0] && y <= range[1]) return true;
                }
                return false;
            };
        }
        int min = parseInt(properties.getProperty("minHeight"), Integer.MIN_VALUE);
        int max = parseInt(properties.getProperty("maxHeight"), Integer.MAX_VALUE);
        return y -> y >= min && y <= max;
    }

    private static Set<String> parseBiomes(String value) {
        if (value == null) return Set.of();
        Set<String> biomes = new ObjectOpenHashSet<>();
        for (String biome : value.split("[ ,]+")) {
            if (!biome.isEmpty()) biomes.add(normalizeBiome(biome));
        }
        return Set.copyOf(biomes);
    }

    static String normalizeBiome(String biome) {
        return biome.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    private static int maxMetadata(int metadata, Map<Block, Integer> blocks) {
        int max = metadata == -1 ? -1 : 31 - Integer.numberOfLeadingZeros(metadata);
        for (int mask : blocks.values()) {
            if (mask != -1) max = Math.max(max, 31 - Integer.numberOfLeadingZeros(mask));
        }
        return max;
    }
}
