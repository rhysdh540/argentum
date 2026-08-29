package dev.rdh.cera.props;

import dev.rdh.cera.Cera;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.property.Property;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class BlockMatcher {
    private BlockMatcher() {
    }

    public static Object2IntMap<Block> parseBlocks(String value) {
        if (value == null) return new Object2IntOpenHashMap<>();
        Object2IntMap<Block> blocks = new Object2IntOpenHashMap<>();
        for (String token : value.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            String[] parts = token.split(":");
            int blockIndex = parts.length > 1 && !startsWithDigit(parts[1]) && !parts[1].contains("=") ? 1 : 0;
            String[] blockNames = blockIndex == 1
                    ? new String[]{ parts[0] + ":" + parts[1] }
                    : parts[0].split(",");
            String[] parameters = Arrays.copyOfRange(parts, blockIndex + 1, parts.length);
            for (String blockName : blockNames) {
                Block block = parseBlock(blockName);
                if (block == null) {
                    Cera.LOGGER.warn("Unknown block: {}", blockName);
                } else {
                    int mask = parseBlockStates(block, parameters);
                    blocks.mergeInt(block, mask, (first, second) -> first | second);
                }
            }
        }
        return blocks;
    }

    public static boolean matches(Object2IntMap<Block> blocks, BlockState state) {
        int mask = blocks.getInt(state.getBlock());
        int metadata = state.getBlock().getMetadataFromState(state);
        return (mask & 1 << metadata) != 0;
    }

    public static int parseIntMask(String value) {
        if (value == null) return -1;
        NumberList list = NumberList.parse(value.replace(',', ' ')).orElse(null);
        if (list == null) return 0;
        int mask = 0;
        for (int r = 0; r < list.rangeCount(); r++) {
            long range = list.range(r);
            for (int i = Math.max(0, NumberList.start(range)); i <= Math.min(31, NumberList.end(range)); i++) {
                mask |= 1 << i;
            }
        }
        return mask;
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
}
