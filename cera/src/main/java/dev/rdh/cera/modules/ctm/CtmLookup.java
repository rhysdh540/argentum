package dev.rdh.cera.modules.ctm;

import net.minecraft.block.AbstractLogBlock;
import net.minecraft.block.QuartzBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.Direction;
import it.unimi.dsi.fastutil.ints.IntArrayList;


final class CtmLookup {
    private static final int W = 1;
    private static final int SW = 1 << 1;
    private static final int S = 1 << 2;
    private static final int SE = 1 << 3;
    private static final int E = 1 << 4;
    private static final int NE = 1 << 5;
    private static final int N = 1 << 6;
    private static final int NW = 1 << 7;
    private static final Direction[][] FACE_DIRECTIONS = {
            {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH},
            {Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH},
            {Direction.EAST, Direction.DOWN, Direction.WEST, Direction.UP},
            {Direction.WEST, Direction.DOWN, Direction.EAST, Direction.UP},
            {Direction.NORTH, Direction.DOWN, Direction.SOUTH, Direction.UP},
            {Direction.SOUTH, Direction.DOWN, Direction.NORTH, Direction.UP}
    };
    private static final Direction[] NORTH_AXIS_DIRECTIONS = {
            Direction.WEST, Direction.UP, Direction.EAST, Direction.DOWN
    };
    private static final Direction[] EAST_AXIS_DIRECTIONS = {
            Direction.NORTH, Direction.UP, Direction.SOUTH, Direction.DOWN
    };
    private static final Direction[][][] HORIZONTAL_DIRECTIONS = {
            {
                    {Direction.WEST, Direction.EAST}, {Direction.WEST, Direction.EAST},
                    {Direction.EAST, Direction.WEST}, {Direction.WEST, Direction.EAST},
                    {Direction.NORTH, Direction.SOUTH}, {Direction.SOUTH, Direction.NORTH}
            },
            {
                    {Direction.EAST, Direction.WEST}, {Direction.WEST, Direction.EAST},
                    {Direction.WEST, Direction.EAST}, {Direction.WEST, Direction.EAST},
                    {Direction.DOWN, Direction.UP}, {Direction.UP, Direction.DOWN}
            },
            {
                    {Direction.SOUTH, Direction.NORTH}, {Direction.NORTH, Direction.SOUTH},
                    {Direction.UP, Direction.DOWN}, {Direction.DOWN, Direction.UP},
                    {Direction.NORTH, Direction.SOUTH}, {Direction.NORTH, Direction.SOUTH}
            }
    };
    private static final Direction[][][] VERTICAL_DIRECTIONS = {
            {
                    {Direction.NORTH, Direction.SOUTH}, {Direction.SOUTH, Direction.NORTH},
                    {Direction.DOWN, Direction.UP}, {Direction.DOWN, Direction.UP},
                    {Direction.DOWN, Direction.UP}, {Direction.DOWN, Direction.UP}
            },
            {
                    {Direction.SOUTH, Direction.NORTH}, {Direction.SOUTH, Direction.NORTH},
                    {Direction.UP, Direction.DOWN}, {Direction.DOWN, Direction.UP},
                    {Direction.SOUTH, Direction.NORTH}, {Direction.SOUTH, Direction.NORTH}
            },
            {
                    {Direction.WEST, Direction.EAST}, {Direction.WEST, Direction.EAST},
                    {Direction.WEST, Direction.EAST}, {Direction.WEST, Direction.EAST},
                    {Direction.DOWN, Direction.UP}, {Direction.UP, Direction.DOWN}
            }
    };
    private static final int[] CANONICAL_MASKS = createCanonicalMasks(
            """
            ... ... ... ... ... ... .x. ... .xx .x. xx. xxx
            .#. .#x x#x x#. .#x x#. .#x x#x x#x x#x x#x x#x
            ... ... ... ... .x. .x. .x. .x. .x. .xx xx. .x.
            ... ... ... ... .x. .x. .x. .x. xx. .x. .x. .xx
            .#. .#x x#x x#. .#x x#. x#x x#. x#x x#x x#x x#x
            .x. .xx xxx xx. ... ... ... .x. .x. xx. xxx .xx
            .x. .xx xxx xx. .x. ... .xx ... xxx xxx .xx xx.
            .#. .#x x#x x#. .#x x#x .#x x#x x#x x#x x#x x#x
            .x. .xx xxx xx. .xx xx. .x. .xx xx. .xx xx. .xx
            .x. .xx xxx xx. .xx xx. xx. .x. xx. .xx .x.
            .#. .#x x#x x#. x#x x#. x#x x#. x#x x#x x#x
            ... ... ... ... ... .x. ... xx. xxx xxx .x.
            """
    );
    private static final byte[] TILE_LOOKUP = createLookup();

    private CtmLookup() {
    }

    static void validate() {
        require(horizontalDirections(Direction.WEST, 1), Direction.DOWN, Direction.UP);
        require(horizontalDirections(Direction.NORTH, 2), Direction.UP, Direction.DOWN);
        require(verticalDirections(Direction.NORTH, 1), Direction.UP, Direction.DOWN);
        require(verticalDirections(Direction.EAST, 2), Direction.UP, Direction.DOWN);
    }

    static int tileIndex(int connections) {
        return TILE_LOOKUP[connections & 255] & 255;
    }

    static Direction[] directions(Direction face, int axis) {
        if (face == Direction.NORTH && axis == 1) return NORTH_AXIS_DIRECTIONS;
        if (face == Direction.EAST && axis == 2) return EAST_AXIS_DIRECTIONS;
        return FACE_DIRECTIONS[face.ordinal()];
    }

    static Direction[] horizontalDirections(Direction face, int axis) {
        return HORIZONTAL_DIRECTIONS[axis][face.ordinal()];
    }

    static Direction[] verticalDirections(Direction face, int axis) {
        return VERTICAL_DIRECTIONS[axis][face.ordinal()];
    }

    static Direction logicalFace(Direction face, int axis) {
        if (axis == 1) {
            return switch (face) {
                case DOWN -> Direction.NORTH;
                case UP -> Direction.SOUTH;
                case NORTH -> Direction.UP;
                case SOUTH -> Direction.DOWN;
                default -> face;
            };
        }
        if (axis == 2) {
            return switch (face) {
                case DOWN -> Direction.WEST;
                case UP -> Direction.EAST;
                case WEST -> Direction.UP;
                case EAST -> Direction.DOWN;
                default -> face;
            };
        }
        return face;
    }

    static int axis(BlockState state) {
        int metadata = state.getBlock().getMetadataFromState(state);
        if (state.getBlock() instanceof AbstractLogBlock) {
            return switch ((metadata & 12) >> 2) {
                case 1 -> 2;
                case 2 -> 1;
                default -> 0;
            };
        }
        if (state.getBlock() instanceof QuartzBlock) {
            return metadata == 3 ? 2 : metadata == 4 ? 1 : 0;
        }
        return 0;
    }

    private static int[] createCanonicalMasks(String layout) {
        String[] lines = layout.strip().split("\\R");
        int[] positions = {NW, N, NE, W, 0, E, SW, S, SE};
        IntArrayList masks = new IntArrayList();
        for (int row = 0; row < lines.length; row += 3) {
            String[][] tiles = {
                    lines[row].split(" +"),
                    lines[row + 1].split(" +"),
                    lines[row + 2].split(" +")
            };
            if (tiles[0].length != tiles[1].length || tiles[0].length != tiles[2].length) {
                throw new IllegalStateException("Invalid CTM tile layout");
            }
            for (int column = 0; column < tiles[0].length; column++) {
                String tile = tiles[0][column] + tiles[1][column] + tiles[2][column];
                if (tile.length() != 9 || tile.charAt(4) != '#') {
                    throw new IllegalStateException("Invalid CTM tile " + tile);
                }
                int mask = 0;
                for (int i = 0; i < positions.length; i++) {
                    if (tile.charAt(i) == 'x') mask |= positions[i];
                }
                masks.add(mask);
            }
        }
        return masks.toIntArray();
    }

    private static byte[] createLookup() {
        byte[] lookup = new byte[256];
        boolean[] seen = new boolean[256];
        for (int i = 0; i < CANONICAL_MASKS.length; i++) {
            int mask = CANONICAL_MASKS[i];
            if (mask != normalize(mask) || seen[mask]) {
                throw new IllegalStateException("Invalid CTM neighborhood " + mask);
            }
            seen[mask] = true;
            lookup[mask] = (byte)i;
        }
        for (int mask = 0; mask < lookup.length; mask++) {
            int normalized = normalize(mask);
            if (!seen[normalized]) throw new IllegalStateException("Missing CTM neighborhood " + normalized);
            lookup[mask] = lookup[normalized];
        }
        if ((lookup[0] & 255) != 0 || (lookup[W] & 255) != 3 || (lookup[E] & 255) != 1
                || (lookup[S] & 255) != 12 || (lookup[N] & 255) != 36
                || (lookup[W | S | E | N] & 255) != 46 || (lookup[255] & 255) != 26) {
            throw new IllegalStateException("Invalid CTM tile order");
        }
        return lookup;
    }

    private static int normalize(int mask) {
        for (int corner = 1; corner < 8; corner += 2) {
            int previous = 1 << (corner - 1);
            int next = 1 << ((corner + 1) & 7);
            if ((mask & previous) == 0 || (mask & next) == 0) {
                mask &= ~(1 << corner);
            }
        }
        return mask;
    }

    private static void require(Direction[] actual, Direction first, Direction second) {
        if (actual[0] != first || actual[1] != second) {
            throw new IllegalStateException("Invalid line-method directions");
        }
    }
}
